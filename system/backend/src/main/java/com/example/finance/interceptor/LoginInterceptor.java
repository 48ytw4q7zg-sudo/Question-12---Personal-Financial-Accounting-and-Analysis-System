package com.example.finance.interceptor;

import com.example.finance.common.BusinessException;
import com.example.finance.common.ErrorCode;
import com.example.finance.common.Result;
import com.example.finance.common.enums.UserRole;
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

  /** HTTP 状态码：OK（body-code-first 约定，始终返回 HTTP 200 + body code） */
  private static final int HTTP_OK = 200;
  /** 业务状态码：未认证（与 ErrorCode 号段不冲突，LoginInterceptor 专用） */
  private static final int CODE_UNAUTHORIZED = 401;

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
    // CORS 预检请求(OPTIONS)直接放行，避免预检请求被拦截导致前端跨域失败
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {  // 判断是否为OPTIONS预检请求
      return true;  // OPTIONS预检请求直接放行
    }
    // 获取 Authorization 头
    String authHeader = request.getHeader("Authorization");  // 读取Authorization请求头
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {  // 缺失或不以Bearer开头
      writeError(response);  // 写入401错误响应
      return false;  // 中断请求链
    }

    // 一次解析 token，同时提取 userId + role（性能优化：消除双重解析）
    String token = authHeader.substring("Bearer ".length()).trim();  // 截取Bearer后面的token部分
    JwtUtils.JwtPayload payload = JwtUtils.parseTokenPayload(token);  // 一次解析token获取payload
    if (payload == null) {  // token无效或过期
      writeError(response);  // 写入401错误响应
      return false;  // 中断请求链
    }

    // 将 userId + role 存入请求属性，供 Controller 使用
    request.setAttribute("userId", payload.getUserId());  // 将userId存入请求属性
    request.setAttribute("role", payload.getRole() != null ? payload.getRole() : UserRole.NORMAL.getValue());  // 将role存入请求属性(role为空则默认普通用户)
    return true;  // 放行请求
  }

  /**
   * 从请求中提取 userId（供 Controller 便捷调用）
   * 若 userId 为 null（非正常调用路径），抛出业务异常防止 NPE
   */
  public static Long getUserId(HttpServletRequest request) {  // 从请求属性提取userId
    Long userId = (Long) request.getAttribute("userId");  // 读取拦截器存入的userId
    if (userId == null) {  // userId为空(非正常调用路径)
      throw new BusinessException(
          ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMsg());  // 抛出未认证异常防止NPE（使用ErrorCode统一管理）
    }
    return userId;  // 返回userId
  }

  /**
   * 返回 401 JSON 响应（HTTP 200 + body code 401，对齐 GlobalExceptionHandler body-code-first 约定）
   * 前端 axios 拦截器检查 res.data.code 而非 HTTP status，因此统一使用 HTTP 200 + body code 机制
   */
  private void writeError(HttpServletResponse response) throws Exception {  // 写入401错误JSON响应
    response.setStatus(HTTP_OK);  // HTTP状态码设为200(body-code-first约定)
    response.setContentType("application/json;charset=UTF-8");  // 设置响应类型为JSON
    var writer = response.getWriter();  // 获取Writer并复用，避免多次调用getWriter()
    writer.write(  // 写入响应体
        objectMapper.writeValueAsString(Result.error(ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMsg()))  // 序列化为JSON字符串（使用ErrorCode统一管理）
    );
    writer.flush();  // 强制刷新缓冲区，确保401响应立即发送给客户端
    // 注意：Servlet拦截器场景下不手动close Writer，让Servlet容器统一管理，避免后续Filter/Handler无法写入
  }
}
