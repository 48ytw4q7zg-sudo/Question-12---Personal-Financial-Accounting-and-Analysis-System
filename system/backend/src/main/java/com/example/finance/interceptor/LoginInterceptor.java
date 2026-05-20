package com.example.finance.interceptor;

import com.example.finance.common.Result;
import com.example.finance.util.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器 - 校验 JWT token
 *
 * 职责：拦截 /api/** 请求（白名单除外），校验 Authorization 头中的 JWT token，
 *       解析后将 userId 和 role 存入 request 属性，供 Controller 层使用。
 *
 * 注册位置：WebMvcConfig.java 中注册，拦截 /api/**，白名单放行 /api/user/login、/api/user/register、/api/health
 *
 * 安全关键方法：preHandle() 是认证入口，token 无效或过期时返回 401 JSON 并中断请求链
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

  /** JSON 序列化器（用于构造 401 错误响应） */
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * 请求预处理（JWT 认证核心方法）
   *
   * 流程：
   *   1. 从 Authorization 头提取 "Bearer <token>"
   *   2. 调用 JwtUtils.parseTokenPayload() 一次解析 token，同时提取 userId + role（消除双重解析）
   *   3. 将 userId 和 role 存入 request 属性，供 Controller 通过 LoginInterceptor.getUserId() 获取
   *   4. token 缺失/过期/解析失败 → 返回 401 JSON，返回 false 中断请求链
   *
   * 性能优化：原实现调用 parseToken() + parseRole() 各解析一次 token（2 次 HMAC 验签），
   * 现改为 parseTokenPayload() 一次解析（1 次 HMAC 验签），减少 50% 密码学运算。
   *
   * @param request  HTTP 请求
   * @param response HTTP 响应（用于 401 错误时写入 JSON）
   * @param handler  目标处理器（本方法未使用）
   * @return true=放行请求 / false=中断请求（401）
   */
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    // 获取 Authorization 头
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      writeError(response);
      return false;
    }

    // 一次解析 token，同时提取 userId + role（性能优化：消除双重解析）
    String token = authHeader.substring(7);
    JwtUtils.JwtPayload payload = JwtUtils.parseTokenPayload(token);
    if (payload == null) {
      writeError(response);
      return false;
    }

    // 将 userId + role 存入请求属性，供 Controller 使用
    request.setAttribute("userId", payload.getUserId());
    request.setAttribute("role", payload.getRole() != null ? payload.getRole() : 0);
    return true;
  }

  /**
   * 从请求中提取 userId（供 Controller 便捷调用）
   */
  public static Long getUserId(HttpServletRequest request) {
    return (Long) request.getAttribute("userId");
  }

  /**
   * 返回 401 JSON 响应
   */
  private void writeError(HttpServletResponse response) throws Exception {
    response.setStatus(401);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(
        objectMapper.writeValueAsString(Result.error(401, "未登录或 token 已过期"))
    );
  }
}
