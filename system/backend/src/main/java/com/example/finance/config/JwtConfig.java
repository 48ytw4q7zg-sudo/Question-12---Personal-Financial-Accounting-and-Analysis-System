package com.example.finance.config;

import com.example.finance.util.JwtUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置类（从 application.yml 读取密钥和过期时间，启动时校验密钥长度并初始化 JwtUtils）
 *
 * <p>关键安全约束：</p>
 * <ul>
 *   <li>HS256 要求密钥 ≥ 32 字节（256 bit），不满足则抛 IllegalStateException 阻止启动</li>
 *   <li>检测到默认占位密钥时输出 WARN 日志，提示生产环境必须用 JWT_SECRET 环境变量替换</li>
 * </ul>
 *
 * <p>调用方：Spring Boot @PostConstruct 自动执行 → JwtUtils.init(secret, expire) 初始化静态工具类。</p>
 */
@Slf4j
@Configuration
public class JwtConfig {

  // HS256 要求密钥至少 32 字节（256 bit）
  private static final int MIN_SECRET_LENGTH = 32;

  // 出厂占位密钥列表(P0-2 修复 · Q-CR Loop1)：
  // 旧实现仅比对一个常量,但 application.yml 默认值是小写连字符版本,导致生产环境用默认密钥不会被拦截。
  // 修复:列出所有历史/当前的占位密钥变体,并在比对时忽略大小写,确保 prod profile 全部拦截。
  // 调用方:本类 init() 方法第 65 行 isPlaceholderSecret() 检查
  private static final java.util.Set<String> PLACEHOLDER_SECRETS = java.util.Set.of(
      "CHANGE-ME-IN-PRODUCTION-USE-JWT_SECRET-ENV",                  // 历史大写连字符版本
      "change-me-in-production-use-jwt-secret-env",                  // application.yml 当前默认值(全小写)
      "CHANGE-ME-IN-PRODUCTION-USE-JWT-SECRET-ENV",                  // 大写带连字符变体
      "your-jwt-secret-key-change-in-production-min-32-chars-long",  // 项目其他文档示例值
      "default-secret-key",                                          // 早期版本默认值
      "your-secret-key-here"                                         // 通用占位
  );

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.expire}")
  private long expire;

  /** 当前激活的 Spring profile（用于区分开发/生产环境） */
  @Value("${spring.profiles.active:dev}")
  private String activeProfile;

  /**
   * 判断给定密钥是否为占位密钥（忽略大小写比对所有历史变体）
   * P0-2 修复 (Q-CR Loop1)：替代单一字符串比对,增强对默认值变体的检测能力
   *
   * @param candidate 待检测的密钥字符串
   * @return true=是占位密钥需替换,false=非占位密钥
   */
  private static boolean isPlaceholderSecret(String candidate) {
    if (candidate == null) return false;                              // null 不算占位
    String lower = candidate.toLowerCase().trim();                    // 转小写并去空白做容错比对
    for (String placeholder : PLACEHOLDER_SECRETS) {                  // 遍历所有占位密钥变体
      if (placeholder.toLowerCase().equals(lower)) {                  // 忽略大小写比对
        return true;                                                  // 命中占位密钥
      }
    }
    return false;                                                     // 非占位密钥
  }

  /**
   * 启动时初始化 JWT 配置（校验密钥长度 + 检测占位密钥 + 初始化 JwtUtils）
   *
   * <p>执行顺序：</p>
   * <ol>
   *   <li>校验密钥长度 ≥ 32 字节，不满足则抛 IllegalStateException 阻止应用启动</li>
   *   <li>检测是否为默认占位密钥：开发环境仅输出 WARN 日志提醒；生产环境抛异常阻止启动</li>
   *   <li>调用 JwtUtils.init(secret, expire) 初始化静态工具类</li>
   * </ol>
   *
   * @throws IllegalStateException 密钥长度不足 32 字节时阻止启动；生产环境使用默认密钥时阻止启动
   */
  @PostConstruct
  public void init() {
    if (activeProfile == null || activeProfile.isBlank()) {
      log.warn("⚠️ spring.profiles.active 未设置或为空，默认视为 dev 环境。生产部署请显式设置 spring.profiles.active=prod");
      activeProfile = "dev";
    }
    if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
      throw new IllegalStateException(
          "jwt.secret 长度必须 ≥ " + MIN_SECRET_LENGTH + " 字符（HS256 要求 256 bit），当前=" + (secret == null ? 0 : secret.length()));
    }
    // P0-2 修复(Q-CR Loop1)：用 isPlaceholderSecret() 检测所有占位密钥变体,而非仅比对单一常量
    if (isPlaceholderSecret(secret)) {                                // 检测是否为已知的占位密钥
      if ("prod".equalsIgnoreCase(activeProfile)) {                   // 生产环境用占位密钥 → 阻止启动
        throw new IllegalStateException("⚠️ 检测到 JWT 默认占位密钥，生产环境必须通过 JWT_SECRET 环境变量替换为强随机值，应用拒绝启动");
      }
      // 开发环境用占位密钥 → 仅警告
      log.warn("⚠️ 检测到 JWT 默认占位密钥（当前 profile={}），开发环境允许使用但生产环境必须替换！请设置 JWT_SECRET 环境变量", activeProfile);
    }
    JwtUtils.init(secret, expire);
  }
}
