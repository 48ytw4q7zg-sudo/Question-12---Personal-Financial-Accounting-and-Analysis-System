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

  // 出厂占位密钥（明显非生产值 · 已公开在 application.yml 中 · 生产必须用 JWT_SECRET 环境变量覆盖 · Q-CR v11 Loop 4 安全收紧）
  private static final String DEFAULT_SECRET = "CHANGE-ME-IN-PRODUCTION-USE-JWT_SECRET-ENV";

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.expire}")
  private long expire;

  /** 当前激活的 Spring profile（用于区分开发/生产环境） */
  @Value("${spring.profiles.active:dev}")
  private String activeProfile;

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
    // activeProfile 为空时视为 dev（防止 spring.profiles.active=空串误判为非生产环境）
    if (activeProfile == null || activeProfile.isBlank()) {
      log.warn("⚠️ spring.profiles.active 未设置或为空，默认视为 dev 环境。生产部署请显式设置 spring.profiles.active=prod");
      activeProfile = "dev";
    }
    if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
      throw new IllegalStateException(
          "jwt.secret 长度必须 ≥ " + MIN_SECRET_LENGTH + " 字符（HS256 要求 256 bit），当前=" + (secret == null ? 0 : secret.length()));
    }
    if (DEFAULT_SECRET.equals(secret)) {
      // 开发环境允许使用默认密钥（方便本地调试），仅输出 WARN 日志
      // 生产环境（prod profile）严格拒绝默认密钥，阻止启动
      if ("prod".equalsIgnoreCase(activeProfile)) {
        throw new IllegalStateException("⚠️ 检测到 JWT 默认占位密钥，生产环境必须通过 JWT_SECRET 环境变量替换为强随机值，应用拒绝启动");
      }
      log.warn("⚠️ 检测到 JWT 默认占位密钥（当前 profile={}），开发环境允许使用但生产环境必须替换！请设置 JWT_SECRET 环境变量", activeProfile);
    }
    JwtUtils.init(secret, expire);
  }
}
