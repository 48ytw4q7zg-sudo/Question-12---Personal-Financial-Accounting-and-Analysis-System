package com.example.finance.config;

import com.example.finance.util.JwtUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置类（从 application.yml 读取密钥和过期时间）
 */
@Slf4j
@Configuration
public class JwtConfig {

  // HS256 要求密钥至少 32 字节（256 bit）
  private static final int MIN_SECRET_LENGTH = 32;

  // 出厂默认密钥（已公开在 application.yml 中，生产必须用 JWT_SECRET 环境变量覆盖）
  private static final String DEFAULT_SECRET = "finance-system-jwt-secret-key-2026";

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.expire}")
  private long expire;

  @PostConstruct
  public void init() {
    if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
      throw new IllegalStateException(
          "jwt.secret 长度必须 ≥ " + MIN_SECRET_LENGTH + " 字符（HS256 要求 256 bit），当前=" + (secret == null ? 0 : secret.length()));
    }
    if (DEFAULT_SECRET.equals(secret)) {
      log.warn("⚠️ 检测到 JWT 默认密钥仍在使用，生产环境请通过 JWT_SECRET 环境变量替换为强随机值");
    }
    JwtUtils.init(secret, expire);
  }
}
