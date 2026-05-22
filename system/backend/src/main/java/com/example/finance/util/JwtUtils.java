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

  // 密钥和过期时间初始为 null/0，必须由 JwtConfig.init() 初始化后方可使用
  private static String SECRET = null;
  // 默认过期时间：7天（可通过 JWT_EXPIRE 环境变量覆盖，单位毫秒）
  private static long EXPIRE = 0L;

  /** HMAC-SHA256 签名密钥（由 init() 方法从配置文件初始化，未初始化时为 null） */
  private static SecretKey KEY = null;

  /**
   * JWT payload 封装（一次解析返回 userId + username + role，避免双重解析）
   */
  public static class JwtPayload {
    private final Long userId;
    private final String username;
    private final Integer role;

    public JwtPayload(Long userId, String username, Integer role) {
      this.userId = userId;
      this.username = username;
      this.role = role;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public Integer getRole() { return role; }
  }

  /**
   * 初始化密钥配置（由 JwtConfig @PostConstruct 调用）
   * 必须在 generateToken / parseTokenPayload 之前调用，否则抛 IllegalStateException
   */
  public static void init(String secret, long expire) {
    if (secret == null || secret.isEmpty()) {
      throw new IllegalStateException("JwtUtils 初始化失败：jwt.secret 不能为空");
    }
    SECRET = secret;
    KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    if (expire > 0) {
      EXPIRE = expire;
    }
  }

  /** 检查 KEY 是否已初始化，未初始化时抛 IllegalStateException */
  private static void ensureInitialized() {
    if (KEY == null) {
      throw new IllegalStateException("JwtUtils 未初始化，请确保 JwtConfig 已正确配置 jwt.secret");
    }
  }

  /**
   * 生成 JWT token（含 userId + username + role）
   * @throws IllegalStateException JwtUtils 未初始化时抛出
   */
  public static String generateToken(Long userId, String username, Integer role) {
    ensureInitialized();
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim("username", username)
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
   * @throws IllegalStateException JwtUtils 未初始化时抛出
   */
  public static JwtPayload parseTokenPayload(String token) {
    ensureInitialized();
    try {
      Claims claims = Jwts.parser()
          .verifyWith(KEY)
          .build()
          .parseSignedClaims(token)
          .getPayload();
      Long userId = Long.parseLong(claims.getSubject());
      String username = claims.get("username", String.class);
      Integer role = claims.get("role", Integer.class);
      return new JwtPayload(userId, username, role);
    } catch (ExpiredJwtException e) {
      log.warn("JWT token 已过期, subject: {}", e.getClaims().getSubject());
      return null;
    } catch (Exception e) {
      log.warn("JWT token 解析失败: {}", e.getMessage());
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
