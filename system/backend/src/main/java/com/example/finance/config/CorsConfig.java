package com.example.finance.config;

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
 *
 * <p>调用方：Spring Boot 自动装配，所有 HTTP 请求经过此 CorsFilter。</p>
 */
@Configuration
public class CorsConfig {

  // OWASP A01: 开发默认 localhost,生产必须通过 CORS_ALLOWED_ORIGINS 环境变量限定
  @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:5174}")
  private String allowedOrigins;

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
    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
    config.setAllowCredentials(true);
    config.addExposedHeader("Authorization");

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
  }
}
