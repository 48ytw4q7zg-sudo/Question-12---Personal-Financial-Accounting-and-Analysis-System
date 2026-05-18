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
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    // 获取 Authorization 头
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      writeError(response);
      return false;
    }

    // 解析 token
    String token = authHeader.substring(7);
    Long userId = JwtUtils.parseToken(token);
    if (userId == null) {
      writeError(response);
      return false;
    }

    // 将 userId + role 存入请求属性，供 Controller 使用
    request.setAttribute("userId", userId);
    Integer role = JwtUtils.parseRole(token);
    request.setAttribute("role", role != null ? role : 0);
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
