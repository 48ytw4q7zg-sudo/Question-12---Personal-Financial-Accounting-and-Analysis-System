package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.common.BusinessException;
import com.example.finance.common.ErrorCode;
import com.example.finance.entity.User;
import com.example.finance.entity.dto.LoginResponse;
import com.example.finance.entity.dto.UserLoginRequest;
import com.example.finance.mapper.UserMapper;
import com.example.finance.service.UserService;
import com.example.finance.util.JwtUtils;
import com.example.finance.util.LoginRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户服务实现（PRD P0-1 登录/JWT + P1-7 修改密码）
 *
 * 关键业务规则：
 * - 密码 BCrypt(12) 加密存储（OWASP A02 安全加固）
 * - 登录不区分「用户不存在」和「密码错误」（防枚举攻击）
 * - 改密时验旧密码 + 新旧密码不可相同 + 新密码 BCrypt 加密更新
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  /** → UserMapper：用户数据访问 */
  private final UserMapper userMapper;

  /**
   * BCrypt 密码编码器（工作因子 12，比默认 10 更安全）
   * <p>与已存哈希 $2a$10$ 兼容，新注册用户自动升级为 $2a$12$ 强度。</p>
   */
  private final BCryptPasswordEncoder bCryptEncoder = new BCryptPasswordEncoder(12);

  /**
   * 用户注册
   *
   * 流程：检查用户名唯一性 → BCrypt 加密密码 → 插入 user 表 → 生成 JWT token → 返回
   *
   * @param request 注册请求（username + password，已由 @Valid 校验）
   * @return LoginResponse（token + userId + username）
   * @throws BusinessException(1001) 用户名已存在
   */
  @Override
  @Transactional
  public LoginResponse register(UserLoginRequest request) {
    // 检查用户名是否已存在
    User existing = userMapper.selectOne(
        new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
    );
    if (existing != null) {
      throw new BusinessException(ErrorCode.USERNAME_EXISTS.getCode(), ErrorCode.USERNAME_EXISTS.getMsg());
    }

    // 创建用户
    User user = new User();
    user.setUsername(request.getUsername());
    user.setPassword(bCryptEncoder.encode(request.getPassword()));
    user.setRole(0);
    user.setCreateTime(LocalDateTime.now());
    user.setUpdateTime(LocalDateTime.now());
    userMapper.insert(user);

    String token = JwtUtils.generateToken(user.getId(), user.getRole());
    return new LoginResponse(token, user.getId(), user.getUsername(), user.getRole());
  }

  /**
   * 用户登录
   *
   * 流程：查用户 → BCrypt 验证密码 → 生成 JWT token → 返回
   * 安全设计：不区分「用户不存在」和「密码错误」以防用户名枚举攻击
   *
   * @param request 登录请求（username + password，已由 @Valid 校验）
   * @return LoginResponse（token + userId + username）
   * @throws BusinessException(1002) 用户名或密码错误（不区分具体原因）
   */
  @Override
  public LoginResponse login(UserLoginRequest request) {
    // 登录限流：同一用户名每秒最多 2 次尝试，超出则拒绝（防暴力破解）
    if (!LoginRateLimiter.tryAcquire(request.getUsername())) {
      throw new BusinessException(ErrorCode.LOGIN_RATE_LIMIT.getCode(), ErrorCode.LOGIN_RATE_LIMIT.getMsg());
    }

    User user = userMapper.selectOne(
        new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
    );
    if (user == null || !bCryptEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new BusinessException(ErrorCode.PASSWORD_ERROR.getCode(), ErrorCode.PASSWORD_ERROR.getMsg());
    }

    String token = JwtUtils.generateToken(user.getId(), user.getRole());
    // 登录成功后清理限流器，释放内存
    LoginRateLimiter.cleanup(request.getUsername());
    return new LoginResponse(token, user.getId(), user.getUsername(), user.getRole());
  }

  /**
   * 修改密码
   *
   * 流程：查用户 → BCrypt 验证旧密码 → 校验新旧密码不可相同 → BCrypt 加密新密码 → 更新数据库
   *
   * @param userId      当前用户 ID（JWT 解码，非请求参数，防越权）
   * @param oldPassword 旧密码（用于身份验证）
   * @param newPassword 新密码（6-20 字符，不能与旧密码相同）
   * @throws BusinessException(1003) 用户不存在（码值对齐 ErrorCode.USER_NOT_FOUND）
   * @throws BusinessException(1005) 旧密码错误
   * @throws BusinessException(1006) 新密码不能与旧密码相同
   */
  @Override
  @Transactional
  public void changePassword(Long userId, String oldPassword, String newPassword) {
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(), ErrorCode.USER_NOT_FOUND.getMsg());
    }

    // 校验旧密码
    if (!bCryptEncoder.matches(oldPassword, user.getPassword())) {
      throw new BusinessException(ErrorCode.OLD_PASSWORD_ERROR.getCode(), ErrorCode.OLD_PASSWORD_ERROR.getMsg());
    }

    // 校验新密码不能与旧密码相同
    if (bCryptEncoder.matches(newPassword, user.getPassword())) {
      throw new BusinessException(ErrorCode.SAME_PASSWORD.getCode(), ErrorCode.SAME_PASSWORD.getMsg());
    }

    // 更新密码
    user.setPassword(bCryptEncoder.encode(newPassword));
    user.setUpdateTime(LocalDateTime.now());
    userMapper.updateById(user);
  }
}
