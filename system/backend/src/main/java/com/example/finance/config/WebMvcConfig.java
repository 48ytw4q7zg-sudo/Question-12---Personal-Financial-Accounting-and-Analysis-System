package com.example.finance.config;

// ===== 项目内部导入 =====
import com.example.finance.interceptor.LoginInterceptor;  // JWT 登录拦截器（校验 token + 设置 userId/role）
import com.example.finance.interceptor.AdminInterceptor;  // 管理员权限拦截器（校验 role=1）

// ===== Lombok 导入 =====
import lombok.RequiredArgsConstructor;  // 自动生成构造器注入

// ===== Spring 框架导入 =====
import org.springframework.context.annotation.Configuration;  // 配置类注解
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;  // 拦截器注册器
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;  // Spring MVC 配置接口

/**
 * Web MVC 配置 — 注册所有 Spring MVC 拦截器
 *
 * <p>拦截器执行顺序（按注册顺序调用 preHandle）：</p>
 * <ol>
 *   <li><b>LoginInterceptor</b>：拦截 /api/**，校验 JWT token + 将 userId/role 存入 request 属性</li>
 *   <li><b>AdminInterceptor</b>：拦截 /api/admin/**，校验 role=1（管理员权限 · Q-CR HIGH 修复 · 消除 AdminController 中每个接口手动调用 checkAdmin() 的重复代码）</li>
 * </ol>
 *
 * <p>白名单（仅 LoginInterceptor 放行）：</p>
 * <ul>
 *   <li>/api/v1/user/login — 登录接口（未登录用户调用）</li>
 *   <li>/api/v1/user/register — 注册接口（未登录用户调用）</li>
 *   <li>/api/v1/health — 健康检查接口（运维监控调用，无需鉴权）</li>
 * </ul>
 *
 * <p>调用方：Spring Boot 自动装配，所有 HTTP 请求按注册顺序经过拦截器链。</p>
 */
@Configuration  // Spring 配置类注解（Spring Boot 自动装配 WebMvcConfigurer 实现类）
@RequiredArgsConstructor  // Lombok 自动生成包含 final 字段的构造器（用于依赖注入）
public class WebMvcConfig implements WebMvcConfigurer {  // 实现 Spring MVC 配置接口

  /** → LoginInterceptor：JWT 登录拦截器（校验 Authorization 头中的 Bearer token + 解析 userId/role） */
  private final LoginInterceptor loginInterceptor;

  /** → AdminInterceptor：管理员权限拦截器（校验 role=1 · Q-CR HIGH 修复 · 消除 AdminController 中的重复权限校验代码） */
  private final AdminInterceptor adminInterceptor;

  /**
   * 注册拦截器到 Spring MVC 拦截器链
   *
   * <p>执行顺序：LoginInterceptor.preHandle() → AdminInterceptor.preHandle() → Controller。</p>
   * <p>任意拦截器返回 false 或抛异常 → 后续拦截器和 Controller 不再执行。</p>
   *
   * @param registry 拦截器注册器（Spring MVC 注入）
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {  // 注册拦截器

    // ========== 拦截器 #1：LoginInterceptor（JWT 校验 + 解析 userId/role） ==========
    registry.addInterceptor(loginInterceptor)  // 添加登录拦截器到注册表
        .addPathPatterns("/api/v1/**")  // 拦截所有 /api/v1/** 路径
        .excludePathPatterns(  // 白名单路径免鉴权（未登录用户可访问）
            "/api/v1/user/login",      // 登录接口
            "/api/v1/user/register",   // 注册接口
            "/api/v1/health"           // 健康检查接口（运维监控）
        );

    // ========== 拦截器 #2：AdminInterceptor（管理员权限校验 · Q-CR HIGH 修复） ==========
    // 拦截 /api/v1/admin/** 请求，校验 LoginInterceptor 解析出的 role 是否为 1（管理员）
    // 非管理员访问时抛出 BusinessException(6004)，由 GlobalExceptionHandler 统一处理为 Result.error(6004, "无管理员权限")
    // 注意：AdminInterceptor 在 LoginInterceptor 之后执行，依赖 LoginInterceptor 存入的 role 属性
    registry.addInterceptor(adminInterceptor)  // 添加管理员拦截器到注册表
        .addPathPatterns("/api/v1/admin/**");  // 仅拦截管理员接口路径
  }
}
