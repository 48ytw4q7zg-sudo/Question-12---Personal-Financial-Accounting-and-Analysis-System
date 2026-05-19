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
