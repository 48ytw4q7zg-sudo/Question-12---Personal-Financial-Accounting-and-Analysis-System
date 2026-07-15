package com.example.finance.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
  // volatile 关键字确保多线程可见性（JwtConfig 在主线程初始化，请求线程在 Tomcat 线程池读取）
  // 引用：JwtConfig.java 第 52 行 @PostConstruct 调用 init() 写入值
  private static volatile String SECRET = null;
  // 默认过期时间：7天（可通过 JWT_EXPIRE 环境变量覆盖，单位毫秒）
  private static volatile long EXPIRE = 0L;

  /** HMAC-SHA256 签名密钥（由 init() 方法从配置文件初始化，未初始化时为 null） */
  // volatile 保证 SecretKey 对象在所有线程立即可见（JMM 线程安全修复）
  private static volatile SecretKey KEY = null;

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
        .issuedAt(Date.from(Instant.now()))
        .expiration(Date.from(Instant.now().plusMillis(EXPIRE)))
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
   * 解析 JWT Token 获取 userId（实体类）。
   * <p>调用方: 测试代码（CrossModuleIntegrationTest.java 第 75 行 · OrthogonalAndUserScenarioTest.java 第 207/473 行）</p>
   * <p>生产代码已迁移至 parseTokenPayload()（LoginInterceptor.java 第65行），此方法仅保留供测试兼容。</p>
   *
   * @param token JWT token 字符串
   * @return userId 或 null（解析失败）
   */
  public static Long parseToken(String token) {
    JwtPayload payload = parseTokenPayload(token);
    return payload != null ? payload.getUserId() : null;
  }

  /**
   * 解析 JWT Token 获取 role（Integer）。
   * <p>调用方: 测试代码（CrossModuleIntegrationTest.java 第 77/88 行 · OrthogonalAndUserScenarioTest.java 第 474 行）</p>
   * <p>生产代码已迁移至 parseTokenPayload()（LoginInterceptor.java 第65行），此方法仅保留供测试兼容。</p>
   *
   * @param token JWT token 字符串
   * @return role 或 null（解析失败）
   */
  public static Integer parseRole(String token) {
    JwtPayload payload = parseTokenPayload(token);
    return payload != null ? payload.getRole() : null;
  }

}
