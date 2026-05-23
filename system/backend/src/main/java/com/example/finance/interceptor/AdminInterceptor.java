package com.example.finance.interceptor;

// ===== Spring 框架导入 =====
import org.springframework.stereotype.Component;  // Spring Bean 注册注解
import org.springframework.web.servlet.HandlerInterceptor;  // Spring MVC 拦截器接口

// ===== Servlet API 导入 =====
import jakarta.servlet.http.HttpServletRequest;  // HTTP 请求对象
import jakarta.servlet.http.HttpServletResponse;  // HTTP 响应对象

// ===== 项目内部导入 =====
import com.example.finance.common.BusinessException;  // 业务异常类
import com.example.finance.common.ErrorCode;  // 错误码枚举
import com.example.finance.common.enums.UserRole;  // 用户角色枚举

// ===== Java 标准库导入 =====
import java.util.Objects;  // Objects 工具类（用于安全比较 Integer）

/**
 * 管理员权限拦截器 — 拦截 /api/admin/** 请求，校验 JWT 解析出的 role 是否为管理员（role=1）
 *
 * <p>职责：统一处理管理员权限校验，消除 AdminController 中每个接口手动调用 checkAdmin() 的重复代码。</p>
 * <p>注册位置：WebMvcConfig.java 中注册，拦截 /api/admin/**，在 LoginInterceptor 之后执行。</p>
 * <p>执行顺序：LoginInterceptor（校验 JWT）→ AdminInterceptor（校验 role=1）→ Controller。</p>
 *
 * <p>安全设计：LoginInterceptor 已将 userId 和 role 存入 request 属性，本拦截器直接读取 role 属性。</p>
 * <p>非管理员访问时抛出 BusinessException(6004)，由 GlobalExceptionHandler 统一处理为 Result.error(6004, "无管理员权限")。</p>
 *
 * <p>调用方：Spring Boot 自动装配 → WebMvcConfig.addInterceptors() 注册。</p>
 */
@Component  // 注册为 Spring Bean，由 WebMvcConfig 注入
public class AdminInterceptor implements HandlerInterceptor {  // 实现 Spring MVC 拦截器接口

  /**
   * 请求预处理 — 校验当前用户是否为管理员（role=1）
   *
   * <p>执行时机：LoginInterceptor 之后、Controller 之前。</p>
   * <p>校验逻辑：从 request 属性读取 LoginInterceptor 存入的 role → 判断是否等于 UserRole.ADMIN(1)。</p>
   *
   * @param request  HTTP 请求（LoginInterceptor 已注入 userId + role 属性）
   * @param response HTTP 响应（本方法未使用）
   * @param handler  目标处理器（本方法未使用）
   * @return true=放行请求（管理员） / 抛出异常（非管理员）
   * @throws BusinessException 6004=无管理员权限
   */
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {  // 拦截器预处理方法
    // CORS 预检请求(OPTIONS)直接放行，避免预检请求被拦截导致前端跨域失败（与 LoginInterceptor 保持一致）
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {  // 判断是否为OPTIONS预检请求
      return true;  // OPTIONS预检请求直接放行
    }

    // 从 request 属性读取 LoginInterceptor 存入的 role（Integer 类型）
    Integer role = (Integer) request.getAttribute("role");  // 读取拦截器存入的role属性

    // 校验：role 不为 null 且等于 UserRole.ADMIN(1)，Integer 用 Objects.equals 比较值（避免引用比较 bug 和 null NPE）
    if (role == null || !Objects.equals(role, UserRole.ADMIN.getValue())) {  // 非管理员
      throw new BusinessException(  // 抛出业务异常（由 GlobalExceptionHandler 统一处理为 Result.error）
          ErrorCode.ADMIN_ACCESS_DENIED.getCode(),  // 错误码 6004
          ErrorCode.ADMIN_ACCESS_DENIED.getMsg()   // 错误消息 "无管理员权限"
      );
    }

    return true;  // 管理员放行请求
  }
}
