package com.example.finance.config;

import com.example.finance.util.JwtUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置类（从 application.yml 读取密钥和过期时间）
 */
@Configuration
public class JwtConfig {

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.expire}")
  private long expire;

  @PostConstruct
  public void init() {
    JwtUtils.init(secret, expire);
  }
}
