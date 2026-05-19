package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.common.BusinessException;
import com.example.finance.entity.User;
import com.example.finance.entity.dto.LoginResponse;
import com.example.finance.entity.dto.UserLoginRequest;
import com.example.finance.mapper.UserMapper;
import com.example.finance.service.UserService;
import com.example.finance.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

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
  // OWASP A02 加密失败: BCrypt 工作因子 12 (默认 10) · 与已存哈希 $10$ 兼容,新注册升级强度
  /** BCrypt 密码编码器（工作因子 12，比默认 10 更安全） */
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
  public LoginResponse register(UserLoginRequest request) {
    // 检查用户名是否已存在
    User existing = userMapper.selectOne(
        new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
    );
    if (existing != null) {
      throw new BusinessException(1001, "用户名已存在");
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
    return new LoginResponse(token, user.getId(), user.getUsername());
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
    User user = userMapper.selectOne(
        new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
    );
    if (user == null || !bCryptEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new BusinessException(1002, "用户名或密码错误");
    }

    String token = JwtUtils.generateToken(user.getId(), user.getRole());
    return new LoginResponse(token, user.getId(), user.getUsername());
  }

  /**
   * 修改密码
   *
   * 流程：查用户 → BCrypt 验证旧密码 → 校验新旧密码不可相同 → BCrypt 加密新密码 → 更新数据库
   *
   * @param userId      当前用户 ID（JWT 解码，非请求参数，防越权）
   * @param oldPassword 旧密码（用于身份验证）
   * @param newPassword 新密码（6-20 字符，不能与旧密码相同）
   * @throws BusinessException(1003) 用户不存在
   * @throws BusinessException(1002) 旧密码错误
   * @throws BusinessException(1003) 新密码不能与旧密码相同
   */
  @Override
  public void changePassword(Long userId, String oldPassword, String newPassword) {
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BusinessException(1003, "用户不存在");
    }

    // 校验旧密码
    if (!bCryptEncoder.matches(oldPassword, user.getPassword())) {
      throw new BusinessException(1002, "旧密码错误");
    }

    // 校验新密码不能与旧密码相同
    if (bCryptEncoder.matches(newPassword, user.getPassword())) {
      throw new BusinessException(1003, "新密码不能与旧密码相同");
    }

    // 更新密码
    user.setPassword(bCryptEncoder.encode(newPassword));
    user.setUpdateTime(LocalDateTime.now());
    userMapper.updateById(user);
  }
}
