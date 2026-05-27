// ╔══════════════════════════════════════════════════════════════════════╗
// ║  📋 答辩参考文件（非主讲）— 用户登录/注册业务实现                           ║
// ║                                                                      ║
// ║  【文件整体实现什么】                                                    ║
// ║  UserServiceImpl.java — 用户服务实现类，包含 register()/login()/changePassword() ║
// ║  login() 覆盖了限流/ORM/BCrypt/JWT 等知识点，可作为答辩备选参考                ║
// ║                                                                      ║
// ║  ⚠ 答辩主讲文件已变更为 TransactionServiceImpl.transfer()（转账业务）       ║
// ║     → 路径：system/backend/.../service/impl/TransactionServiceImpl.java    ║
// ╚══════════════════════════════════════════════════════════════════════╝
package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.common.BusinessException;
import com.example.finance.common.ErrorCode;
import com.example.finance.common.enums.UserRole;
import com.example.finance.entity.User;
import com.example.finance.entity.dto.LoginResponse;
import com.example.finance.entity.dto.UserLoginRequest;
import com.example.finance.mapper.UserMapper;
import com.example.finance.service.UserService;
import com.example.finance.util.JwtUtils;
import com.example.finance.util.LoginRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
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

  /** BCrypt 工作因子（OWASP 推荐 ≥10，本项目使用 12 更安全 · SEC-1 修复：提升为 static 避免每次创建实例浪费内存） */
  private static final int BCRYPT_STRENGTH = 12;

  /**
   * BCrypt 密码编码器（工作因子 12，比默认 10 更安全 · static final 共享单例，BCryptPasswordEncoder 线程安全）
   * <p>与已存哈希 $2a$10$ 兼容，新注册用户自动升级为 $2a$12$ 强度。</p>
   * <p>调用方：register() 方法用于加密新密码 · login() 和 changePassword() 方法用于验证密码 · 均在此 UserServiceImpl.java 中</p>
   */
  private static final BCryptPasswordEncoder BCRYPT_ENCODER = new BCryptPasswordEncoder(BCRYPT_STRENGTH);

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
    user.setPassword(BCRYPT_ENCODER.encode(request.getPassword()));  // 使用 BCryptPasswordEncoder（来自 spring-security-crypto 6.3.4）加密密码
    user.setRole(UserRole.NORMAL.getValue());
    user.setCreateTime(LocalDateTime.now());
    user.setUpdateTime(LocalDateTime.now());
    try {
      userMapper.insert(user);
    } catch (DuplicateKeyException e) {
      // 并发注册同一用户名：唯一索引拦截，抛出清晰的业务异常而非数据库异常
      throw new BusinessException(ErrorCode.USERNAME_EXISTS.getCode(), ErrorCode.USERNAME_EXISTS.getMsg());
    }

    String token = JwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());
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
  @Transactional(readOnly = true)
  // ★【答辩第 131 行】@Transactional(readOnly = true) — Spring 声明式只读事务
  //  做什么：告诉 Spring 和数据库"这个方法只做 SELECT 查询，没有增删改"
  //  为什么：MySQL 读写分离架构可以根据 readOnly 标志把读操作路由到从库，减轻主库压力
  //         只读事务比读写事务省开销——不需要维护 undo log 用于回滚
  //         如果方法里有 INSERT/UPDATE 还要标 readOnly=true → 数据库会报错，防呆设计
  public LoginResponse login(UserLoginRequest request) {
  // ★【答辩第 132 行】方法签名
  //  参数 UserLoginRequest：前端表单 {username, password} 的 JSON → Controller 已经用 @Valid 校验过
  //  返回 LoginResponse：{token, userId, username, role}，Controller 包装成 Result.success() 发给前端

    // ★★【答辩第 134-136 行·知识点 1：限流】★★
    //  做什么：LoginRateLimiter.tryAcquire() 检查同一用户名是否超过登录频率限制（每秒最多 2 次）
    //  为什么放第一步（查数据库之前）？防止暴力破解攻击消耗数据库连接池——
    //    攻击者用脚本每秒几百次尝试不同密码，如果先查库再限流，数据库连接已被耗尽
    //    限流在最前面 → 攻击请求连数据库都碰不到就被拒绝
    //  为什么用用户名做限流 Key 而不是 IP？防止同一 IP 下多用户互相影响（家庭/公司 NAT 环境）
    //  LoginRateLimiter 来自 util/LoginRateLimiter.java，内部用 Guava RateLimiter + TTL 过期自动清理
    //  如果触发限流 → 抛 BusinessException(1004)，前端弹窗"登录尝试过于频繁，请稍后再试"
    if (!LoginRateLimiter.tryAcquire(request.getUsername())) {
      throw new BusinessException(ErrorCode.LOGIN_RATE_LIMIT.getCode(), ErrorCode.LOGIN_RATE_LIMIT.getMsg());
      // ErrorCode.LOGIN_RATE_LIMIT 定义在 common/ErrorCode.java，code=1004，msg="登录尝试过于频繁，请稍后再试"
    }

    // ★★【答辩第 138-140 行·知识点 2+3：ORM + 防 SQL 注入】★★
    //  做什么：用 MyBatis-Plus 的 LambdaQueryWrapper 构建 SQL 条件 WHERE username = ?，查 user 表
    //  生成的 SQL：SELECT * FROM user WHERE username = ?（? 是 PreparedStatement 占位符）
    //  为什么用 LambdaQueryWrapper 而不是写 SQL 字符串？
    //    第一、类型安全 — User::getUsername 是 Java 方法引用，编译时检查
    //      如果字段名写错（如 getUsernmae），编译器直接报错 → 写 SQL 字符串要到运行时才发现
    //    第二、防 SQL 注入 — MyBatis-Plus 自动用 PreparedStatement 参数化
    //      用户输入被当成纯数据而非 SQL 代码执行 → 传 ' OR '1'='1 也无法注入
    //    第三、重构友好 — 改字段名时 IDE 一键重构，所有 Lambda 引用自动更新
    //      写 SQL 字符串只能全局搜索手工改，容易漏
    //  userMapper 是 MyBatis-Plus 的 BaseMapper<User>，selectOne 查一条记录（多条匹配抛异常）
    User user = userMapper.selectOne(
        new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
    );

    // ★★【答辩第 141-143 行·知识点 4+5：BCrypt + 防枚举攻击】★★
    //  做什么：判断用户是否存在 + 验证密码是否正确，两个条件合并到一个 if 里
    //  为什么 user==null 和密码错误统一抛同一个异常？
    //    这叫"防用户名枚举攻击"（Username Enumeration Attack）——
    //    如果区分返回"用户不存在"和"密码错误"，攻击者可以写脚本枚举：
    //      返回"密码错误"→ 这个用户名已注册（可以针对性攻击）
    //      返回"用户不存在"→ 这个用户名没注册（跳过）
    //    统一返回"用户名或密码错误"→ 攻击者无法判断，防用户枚举
    //  为什么 BCRYPT_ENCODER.matches() 而不是 MD5？
    //    第一、BCrypt 不可逆哈希 → 即使数据库泄露，无法从哈希值还原明文密码
    //    第二、BCrypt 自动加盐 → 相同密码每次哈希结果不同，防彩虹表碰撞攻击
    //       （MD5 相同密码产生相同哈希 → 攻击者提前算好"123456→e10adc..."查表秒破）
    //    第三、工作因子 12 = 4096 次迭代 → 暴力破解成本极高，MD5 只需 1 次哈希
    //    第四、BCrypt 是 OWASP 推荐的密码存储方案（A02: Cryptographic Failures）
    //  BCRYPT_ENCODER 是 BCryptPasswordEncoder 实例（上面第 161 行），static final 共享单例线程安全
    if (user == null || !BCRYPT_ENCODER.matches(request.getPassword(), user.getPassword())) {
      throw new BusinessException(ErrorCode.PASSWORD_ERROR.getCode(), ErrorCode.PASSWORD_ERROR.getMsg());
      // ErrorCode.PASSWORD_ERROR 定义在 common/ErrorCode.java，code=1002，msg="用户名或密码错误"
    }

    // ★★【答辩第 145 行·知识点 6：JWT 无状态认证】★★
    //  做什么：登录成功，调用 JwtUtils.generateToken() 生成 JWT token
    //  这个方法把 userId、username、role 三个信息编码到 JWT 的 payload 里，用 HMAC-SHA256 签名
    //  生成的 token 是三段 Base64 字符串，用 . 分隔：header.payload.signature
    //  为什么 JWT 里存 username 和 role？这叫"自包含 token"（Self-Contained Token）
    //    LoginInterceptor 解析 token 就能拿到用户身份 → 不需要每次请求都查数据库
    //    → 这就是"无状态设计"——服务器不需要 session 来记住用户是谁
    //  有效期 7 天（配置在 config/JwtConfig.java），过期后需要重新登录
    //  JwtUtils.generateToken() 在 util/JwtUtils.java 里定义
    String token = JwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

    // ★【答辩第 147 行】清理限流器
    //  做什么：登录成功后移除这个用户名的限流器
    //  为什么：限流器只在登录失败时需要保留（阻止攻击者继续尝试），成功后释放内存防止内存泄漏
    //  LoginRateLimiter 内部用 ConcurrentHashMap + TTL 自动过期，cleanup 主动清理释放 Map 条目
    LoginRateLimiter.cleanup(request.getUsername());

    // ★【答辩第 148 行】返回结果
    //  做什么：返回 LoginResponse DTO，包含 token / userId / username / role 四个字段
    //  token：前端存 localStorage，后续所有请求带 Authorization: Bearer <token>
    //  userId：前端知道是谁登录了
    //  username：显示在界面右上角
    //  role：0=普通用户，1=管理员——路由守卫和侧边栏菜单根据 role 显示不同内容
    //  Controller 收到后包装成 Result.success(loginResponse) → Jackson 序列化为 JSON 发给前端
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
    if (!BCRYPT_ENCODER.matches(oldPassword, user.getPassword())) {
      throw new BusinessException(ErrorCode.OLD_PASSWORD_ERROR.getCode(), ErrorCode.OLD_PASSWORD_ERROR.getMsg());
    }

    // 校验新密码不能与旧密码相同
    if (BCRYPT_ENCODER.matches(newPassword, user.getPassword())) {
      throw new BusinessException(ErrorCode.SAME_PASSWORD.getCode(), ErrorCode.SAME_PASSWORD.getMsg());
    }

    // 更新密码
    user.setPassword(BCRYPT_ENCODER.encode(newPassword));
    user.setUpdateTime(LocalDateTime.now());
    userMapper.updateById(user);
  }
}
