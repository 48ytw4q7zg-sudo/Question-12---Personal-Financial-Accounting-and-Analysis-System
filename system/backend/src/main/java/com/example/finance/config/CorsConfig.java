package com.example.finance.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * CORS 跨域配置（OWASP A01 安全加固 · 区分开发/生产环境）
 *
 * <p>开发环境：allowedOrigins 默认只允许 localhost 前端端口（5173/5174）。</p>
 * <p>生产环境：必须通过 CORS_ALLOWED_ORIGINS 环境变量限定到具体域名，禁止通配。</p>
 * <p>安全约束：allowCredentials=true 时必须明确指定允许源，禁止 addAllowedOriginPattern("*")。</p>
 * <p>启动验证：生产环境检测到 localhost 默认值时阻止启动（对齐 JwtConfig 安全策略）。</p>
 *
 * <p>调用方：Spring Boot 自动装配，所有 HTTP 请求经过此 CorsFilter。</p>
 */
@Slf4j
@Configuration
public class CorsConfig {

  /** CORS 预检请求缓存时间（秒） */
  private static final long CORS_PREFLIGHT_CACHE_SECONDS = 3600L;
  /** CORS 允许的 HTTP 方法 */
  private static final java.util.List<String> ALLOWED_METHODS = Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS");
  /** CORS 允许的请求头 */
  private static final java.util.List<String> ALLOWED_HEADERS = Arrays.asList("Authorization", "Content-Type", "Accept");

  // OWASP A01: 开发默认 localhost, 生产必须通过 CORS_ALLOWED_ORIGINS 环境变量限定
  @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:5174}")
  private String allowedOrigins;

  /** 当前激活的 Spring profile（用于区分开发/生产环境） */
  @Value("${spring.profiles.active:dev}")
  private String activeProfile;

  /** 生产环境启动校验：检测到 localhost 默认 CORS 值时阻止启动（对齐 JwtConfig） */
  @PostConstruct
  public void validateCorsOrigins() {
    boolean hasLocalhost = Arrays.stream(allowedOrigins.split(","))
        .map(String::trim)
        .anyMatch(o -> o.contains("localhost"));
    if (hasLocalhost && "prod".equalsIgnoreCase(activeProfile)) {
      throw new IllegalStateException(
          "⚠️ 检测到 CORS allowedOrigins 包含 localhost，生产环境必须通过 CORS_ALLOWED_ORIGINS 环境变量替换为实际域名，应用拒绝启动");
    }
    if (hasLocalhost) {
      log.warn("⚠️ 检测到 CORS allowedOrigins 包含 localhost（当前 profile={}），开发环境允许使用但生产环境必须替换！", activeProfile);
    }
  }

  /**
   * 创建 CORS 过滤器 Bean
   *
   * <p>配置逻辑：</p>
   * <ul>
   *   <li>所有源都必须明确指定（逗号分隔），开发默认 localhost:5173/5174</li>
   *   <li>生产环境必须通过 CORS_ALLOWED_ORIGINS 环境变量设置具体域名</li>
   * </ul>
   * <p>允许所有 HTTP 方法 + 所有 Header + credentials=true + 暴露 Authorization Header。</p>
   *
   * @return CorsFilter 实例（注册到 Spring 容器）
   */
  @Bean
  public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    // 安全加固: 所有源必须明确指定, 禁止通配符（OWASP A01）
    Arrays.stream(allowedOrigins.split(",")).map(String::trim).forEach(config::addAllowedOrigin);
    config.setAllowedMethods(ALLOWED_METHODS);
    config.setAllowedHeaders(ALLOWED_HEADERS);
    config.setAllowCredentials(true);
    config.addExposedHeader("Authorization");
    // 缓存预检请求 1 小时，减少 OPTIONS 请求频率
    config.setMaxAge(CORS_PREFLIGHT_CACHE_SECONDS);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
  }
}