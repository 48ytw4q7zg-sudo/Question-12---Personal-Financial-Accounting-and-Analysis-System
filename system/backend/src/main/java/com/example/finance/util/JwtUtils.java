package com.example.finance.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 */
@Slf4j
public class JwtUtils {

  // 出厂占位密钥（明显非生产值 · 必须通过 JWT_SECRET 环境变量覆盖 · Q-CR v11 Loop 4 安全收紧）
  private static String SECRET = "CHANGE-ME-IN-PRODUCTION-USE-JWT_SECRET-ENV";
  // 默认过期时间：7天（可通过 JWT_EXPIRE 环境变量覆盖，单位毫秒）
  private static long EXPIRE = 7 * 24 * 60 * 60 * 1000L;

  private static SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

  /**
   * 初始化密钥配置（由配置类调用）
   */
  public static void init(String secret, long expire) {
    if (secret != null && !secret.isEmpty()) {
      SECRET = secret;
      KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }
    if (expire > 0) {
      EXPIRE = expire;
    }
  }

  /**
   * 生成 JWT token（含 userId + role）
   */
  public static String generateToken(Long userId, Integer role) {
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim("role", role)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + EXPIRE))
        .signWith(KEY)
        .compact();
  }

  /**
   * 解析 JWT token，返回 userId + role（无效或过期返回 null）
   */
  public static Long parseToken(String token) {
    try {
      Claims claims = Jwts.parser()
          .verifyWith(KEY)
          .build()
          .parseSignedClaims(token)
          .getPayload();
      return Long.parseLong(claims.getSubject());
    } catch (ExpiredJwtException e) {
      log.debug("JWT token 已过期");
      return null;
    } catch (Exception e) {
      log.debug("JWT token 解析失败: {}", e.getMessage());
      return null;
    }
  }

  /**
   * 解析 JWT token，返回 role（无效或过期返回 null）
   */
  public static Integer parseRole(String token) {
    try {
      Claims claims = Jwts.parser()
          .verifyWith(KEY)
          .build()
          .parseSignedClaims(token)
          .getPayload();
      return claims.get("role", Integer.class);
    } catch (Exception e) {
      log.debug("JWT role 解析失败: {}", e.getMessage());
      return null;
    }
  }
}
