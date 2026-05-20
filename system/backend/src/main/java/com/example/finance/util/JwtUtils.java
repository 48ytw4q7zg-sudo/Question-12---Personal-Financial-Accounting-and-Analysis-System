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
 *
 * 性能优化：parseTokenPayload() 一次解析同时返回 userId + role，
 * 避免 LoginInterceptor 中调用 parseToken() + parseRole() 各解析一次 token（原 Issue 2.2）
 */
@Slf4j
public class JwtUtils {

  // 出厂占位密钥（明显非生产值 · 必须通过 JWT_SECRET 环境变量覆盖 · Q-CR v11 Loop 4 安全收紧）
  private static String SECRET = "CHANGE-ME-IN-PRODUCTION-USE-JWT_SECRET-ENV";
  // 默认过期时间：7天（可通过 JWT_EXPIRE 环境变量覆盖，单位毫秒）
  private static long EXPIRE = 7 * 24 * 60 * 60 * 1000L;

  private static SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

  /**
   * JWT payload 封装（一次解析返回 userId + role，避免双重解析）
   */
  public static class JwtPayload {
    private final Long userId;
    private final Integer role;

    public JwtPayload(Long userId, Integer role) {
      this.userId = userId;
      this.role = role;
    }

    public Long getUserId() { return userId; }
    public Integer getRole() { return role; }
  }

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
   * 解析 JWT token，一次提取 userId + role（性能优化：替代 parseToken + parseRole 双重解析）
   *
   * @param token JWT token 字符串
   * @return JwtPayload（userId + role），token 无效或过期时返回 null
   */
  public static JwtPayload parseTokenPayload(String token) {
    try {
      Claims claims = Jwts.parser()
          .verifyWith(KEY)
          .build()
          .parseSignedClaims(token)
          .getPayload();
      Long userId = Long.parseLong(claims.getSubject());
      Integer role = claims.get("role", Integer.class);
      return new JwtPayload(userId, role);
    } catch (ExpiredJwtException e) {
      log.debug("JWT token 已过期");
      return null;
    } catch (Exception e) {
      log.debug("JWT token 解析失败: {}", e.getMessage());
      return null;
    }
  }

  /**
   * 解析 JWT token，返回 userId（向后兼容，内部调用 parseTokenPayload）
   */
  public static Long parseToken(String token) {
    JwtPayload payload = parseTokenPayload(token);
    return payload != null ? payload.getUserId() : null;
  }

  /**
   * 解析 JWT token，返回 role（向后兼容，内部调用 parseTokenPayload）
   */
  public static Integer parseRole(String token) {
    JwtPayload payload = parseTokenPayload(token);
    return payload != null ? payload.getRole() : null;
  }
}
