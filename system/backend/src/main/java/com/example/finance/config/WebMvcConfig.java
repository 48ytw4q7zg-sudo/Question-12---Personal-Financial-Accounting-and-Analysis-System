package com.example.finance.config;

import com.example.finance.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 *
 * <p>注册登录拦截器(LoginInterceptor), 拦截所有 /api/** 请求并放行白名单。</p>
 * <p>拦截路径: /api/** (所有业务接口需JWT鉴权)。</p>
 * <p>白名单: /api/user/login(登录) / /api/user/register(注册) / /api/health(健康检查)。</p>
 *
 * <p>调用方: Spring Boot自动装配, 拦截所有HTTP请求。</p>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

  private final LoginInterceptor loginInterceptor;

  /**
   * 注册登录拦截器（拦截所有 /api/** 请求，白名单路径免鉴权）
   *
   * <p>拦截路径: /api/**（所有业务接口需 JWT 鉴权）</p>
   * <p>白名单免鉴权路径:</p>
   * <ul>
   *   <li>/api/user/login — 登录接口（未登录用户调用）</li>
   *   <li>/api/user/register — 注册接口（未登录用户调用）</li>
   *   <li>/api/health — 健康检查接口（运维监控调用，无需鉴权）</li>
   * </ul>
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(loginInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns(
            "/api/user/login",
            "/api/user/register",
            "/api/health"
        );
  }
}
