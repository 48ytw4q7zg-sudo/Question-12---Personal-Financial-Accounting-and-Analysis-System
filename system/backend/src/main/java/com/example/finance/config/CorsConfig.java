package com.example.finance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * CORS 跨域配置
 */
@Configuration
public class CorsConfig {

  // OWASP A01: 生产环境必须通过 CORS_ALLOWED_ORIGINS 环境变量限定到具体域名
  // 默认 * 仅用于开发,allowCredentials=true 时浏览器会自动拒绝 * 通配
  @Value("${cors.allowed-origins:*}")
  private String allowedOrigins;

  @Bean
  public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    // R-05-issue-1: 已修复 - 通过环境变量 CORS_ALLOWED_ORIGINS 区分开发/生产
    if ("*".equals(allowedOrigins)) {
      config.addAllowedOriginPattern("*");
    } else {
      Arrays.stream(allowedOrigins.split(",")).map(String::trim).forEach(config::addAllowedOrigin);
    }
    config.addAllowedMethod("*");
    config.addAllowedHeader("*");
    config.setAllowCredentials(true);
    config.addExposedHeader("Authorization");

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
  }
}
