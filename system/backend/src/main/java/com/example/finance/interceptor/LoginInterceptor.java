// ╔══════════════════════════════════════════════════════════════════════╗
// ║  📋 答辩文件 ②/⑦ — 核心代码讲解 → LoginInterceptor：JWT 认证拦截器       ║
// ║                                                                      ║
// ║  【文件整体实现什么】                                                    ║
// ║  LoginInterceptor.java — JWT 认证拦截器，放在 interceptor/ 目录              ║
// ║  preHandle() 拦截所有 /api/v1/** 请求，校验 Authorization 头中的 JWT token   ║
// ║  解析出 userId 和 role 存入 request，供下游 Controller 使用                   ║
// ║  token 缺失/无效/过期时返回 HTTP 200 + body.code=401，中断请求链              ║
// ║                                                                      ║
// ║  【答辩要讲什么】                                                        ║
// ║  重点讲 preHandle() 方法（当前文件第 86-152 行）— 4 步 JWT 校验流程              ║
// ║  补充讲 writeError() 方法（第 148-169 行）— body-code-first 约定              ║
// ║  核心论点：为什么需要拦截器 / Filter vs Interceptor / request.setAttribute 选型 ║
// ║                                                                      ║
// ║  【讲解步骤】                                                           ║
// ║  1. 先说职责（10秒）：校验 JWT token，没 token 返回 401                         ║
// ║  2. 说为什么需要拦截器（20秒）：10个Controller都写JWT校验→重复→AOP分离           ║
// ║  3. 滚到第 86 行 preHandle()，逐行讲 4 步（OPTIONS放行→提取头→解析→存储）       ║
// ║  4. 滚到第 148 行 writeError()，讲 body-code-first（HTTP200 + body 401）       ║
// ║                                                                      ║
// ║  【具体讲稿】                                                           ║
// ║  "LoginInterceptor 校验 JWT token。10个Controller如果每个都写一遍JWT校验，       ║
// ║   重复且易遗漏。拦截器把'校验身份'和'处理业务'分离——AOP思想。                       ║
// ║   第128行preHandle()：第1步OPTIONS放行(预检没Authorization头)；                    ║
// ║   第2步提取Authorization: Bearer <token>（RFC6750标准）；                         ║
// ║   第3步parseTokenPayload()一次解析（优化：旧代码两次验签，合并减少50%运算）；         ║
// ║   第4步setAttribute存userId+role（为什么不用ThreadLocal？Tomcat线程池复用会串号）。  ║
// ║   第190行writeError()：HTTP200+body 401——前端axios检查body.code而非HTTP status。"  ║
// ╚══════════════════════════════════════════════════════════════════════╝
//
// ▶ 讲完后，下一个文件（按 Ctrl+P 粘贴打开）：
//   system/backend/src/main/java/com/example/finance/config/WebMvcConfig.java
//   （拦截器注册 + 白名单配置 — 配置拦截哪些路径、放行哪些路径）
package com.example.finance.interceptor;

import com.example.finance.common.BusinessException;
import com.example.finance.common.ErrorCode;
import com.example.finance.common.Result;
import com.example.finance.common.enums.UserRole;
import com.example.finance.util.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

  /** HTTP 状态码：OK（body-code-first 约定，始终返回 HTTP 200 + body code） */
  private static final int HTTP_OK = 200;

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
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {  // 【做什么】HandlerInterceptor 接口的 preHandle 方法——在请求到达 Controller 之前执行，是 JWT 认证的入口 /【为什么】返回 true=放行继续执行后续拦截器和 Controller，返回 false=中断请求链不再向下传递——三个参数：request(浏览器原始请求)、response(用于写 401 错误 JSON)、handler(当前匹配到的目标 Controller 方法，未使用)
    // OPTIONS 预检请求直接放行：CORS 协议规定预检请求不携带用户凭证（不含 Authorization 头），
    // 如果 LoginInterceptor 校验 OPTIONS 的 token，会因缺失 token 返回 401——
    // 浏览器收到 401 预检响应后认为跨域被拒绝，不会发送正式请求，导致前后端完全无法通信
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {  // 【做什么】equalsIgnoreCase 大小写不敏感比较，判断是否为 OPTIONS 预检请求 /【为什么】兼容浏览器发送的 "OPTIONS" / "options" / "Options" 等各种大小写变体，防御性编程
      return true;  // 【做什么】返回 true 直接放行 /【为什么】预检请求只问服务端"允不允许跨域"，走 CorsFilter 逻辑后正常响应——不校验 token 因为 OPTIONS 根本不带 Authorization 头
    }
    // 从 HTTP 请求头提取 JWT token——前端 axios 请求拦截器在每个请求中拼接 "Bearer " + token 放入 Authorization 头
    String authHeader = request.getHeader("Authorization");  // 【做什么】读取 HTTP Authorization 请求头的值 /【为什么】前端在 axios 请求拦截器中统一添加，值格式为 "Bearer eyJhbGciOi..."——这是前端与后端约定的唯一 token 传递位置
    // 校验请求头格式：必须存在且以 "Bearer " 开头（RFC 6750 OAuth 2.0 Bearer Token Usage 标准 §2.1）
    // 为什么必须检查 "Bearer " 前缀而非直接截取：防止攻击者用其他认证方案（如 "Basic base64encoded"）绕过 JWT 校验——
    // 不检查前缀直接调用 parseTokenPayload() 会导致非 JWT 格式的字符串被当作 token 解析，虽然会失败但增加无效计算开销
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {  // 【做什么】判断 Authorization 头是否缺失或格式不符合 RFC 6750 /【为什么】短路求值：先判 null 防止 NPE，再判前缀——null 时不执行 startsWith 避免了空指针异常
      writeError(response);  // 【做什么】写入 401 认证失败 JSON 响应（HTTP 200 + body.code=401）/【为什么】统一使用 body-code-first 约定而非 HTTP 401 状态码——前端 axios 检查 body.code 而非 HTTP status
      return false;  // 【做什么】返回 false 中断请求链 /【为什么】后续的 AdminInterceptor 和目标 Controller 都不再执行——没有合法 token 的用户不应访问任何业务接口
    }

    // 截取 "Bearer " 之后的纯 token 字符串——"Bearer ".length() 值为 7（含空格），trim() 去除意外首尾空格
    String token = authHeader.substring("Bearer ".length()).trim();  // 【做什么】去掉 "Bearer " 前缀和首尾空格得到纯净的 JWT 字符串 /【为什么】trim() 防御多余空格（如前端代码拼写失误多加了空格），substring 比 split(" ")[1] 更精确高效——不需要创建数组
    // parseTokenPayload() 一次 HMAC-SHA256 验签同时提取 userId 和 role——
    // 旧实现先调 parseToken() 取 userId 再调 parseRole() 取 role，每次调用都做一次完整的 HMAC 验签（密码学运算消耗 CPU）——
    // 合并为一次解析后减少 50% 密码学运算量，在高并发场景下显著降低 CPU 开销（每 1000 请求少做 1000 次 HMAC 运算）
    JwtUtils.JwtPayload payload = JwtUtils.parseTokenPayload(token);  // 【做什么】一次调用完成 token 签名验证 + 载荷（claim）提取 /【为什么】返回 JwtPayload 对象（包含 userId + role），必须用一次性解析——旧方案两次解析返回不同的中间对象，合并后统一返回 JwtPayload 同时承载两个字段
    if (payload == null) {  // 【做什么】判断 token 是否无效 /【为什么】返回 null 的三种情况：token 过期（exp 已过）、签名被篡改（攻击者修改 payload 后未通过 HMAC 验证）、格式错误（不是合法 JWT）
      writeError(response);  // 【做什么】返回统一 401 错误——前端 axios 拦截器读到 body.code=401 后清空 localStorage token 并跳转 /login 页面 /【为什么】token 过期/无效说明用户身份已不可信，必须重新登录获取新 token
      return false;  // 【做什么】中断请求链 /【为什么】无效身份等同于未登录，不应访问任何需要认证的资源
    }

    // request.setAttribute 是 Java Servlet 标准 API，将数据绑定到当前 HttpServletRequest 对象上，生命周期 = 本次 HTTP 请求
    // 为什么用 request.setAttribute 而不是 ThreadLocal：Tomcat 使用线程池复用线程处理不同请求——
    // 如果线程 A 的 ThreadLocal 没清理干净（如异常中断），线程 B 复用时可能读到线程 A 的用户 ID——这就是"串号"安全漏洞。
    // setAttribute 每个请求创建独立的 request 对象，天然隔离，即便线程复用也不会串号
    request.setAttribute("userId", payload.getUserId());  // 【做什么】将解析出的 userId 存入当前请求的属性 Map /【为什么】下游 Controller 通过 LoginInterceptor.getUserId(request) 静态方法取出——Controller 不需要直接依赖 HttpServletRequest 的 getHeader("Authorization") 再重复解析 token
    // 安全降级：token 中 role 字段为 null 时（旧版本 token 不含 role / token 被篡改后 role 字段丢失），
    // 绝不能默认提升为管理员——遵循最小权限原则（Principle of Least Privilege）：宁可降低权限（普通用户=能看的少），
    // 绝不提升权限（管理员=能改系统设置、看所有用户数据，权限泄露后果严重）
    if (payload.getRole() == null) {  // 【做什么】检查 role 字段是否缺失 /【为什么】可能是用户用旧版 token（登录时未签发 role）或 token 被篡改——role 缺失意味着无法确定用户权限级别
      log.warn("JWT token 缺少 role 声明，默认降级为普通用户, userId={}", payload.getUserId());  // 【做什么】记录 WARN 级别日志 /【为什么】日志包含 userId 便于安全审计追踪——WARN 而非 ERROR 因为已做安全降级处理，系统仍可正常运行（只是此用户暂时看不到管理员功能）
    }
    request.setAttribute("role", payload.getRole() != null ? payload.getRole() : UserRole.NORMAL.getValue());  // 【做什么】将 role 存入请求属性，null 时默认赋值为普通用户(role=0) /【为什么】三元表达式 + UserRole.NORMAL.getvalue()：AdminInterceptor 后续从此属性读取 role 判断是否为管理员——赋普通用户确保该用户无法访问管理接口
    return true;  // 【做什么】返回 true，请求继续往下走 /【为什么】token 合法、身份确认、userId 和 role 已存入 request，后续流程（AdminInterceptor、Controller、Service、Mapper）可以安全使用这些信息
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
   *
   * P2-3 修复(Q-CR Loop1)：增加 isCommitted() 兜底,如前置 Filter 已 commit 响应则跳过避免 IllegalStateException
   */
  private void writeError(HttpServletResponse response) throws Exception {  // 【做什么】私有工具方法：向客户端写入统一的 401 认证失败 JSON 响应 /【为什么】抽取为独立方法——preHandle 中有两处需要返回 401（缺 token / token 无效），抽出来避免重复代码，改响应格式时只改一处
    // P2-3 防御性检查：如果前置 Filter（如 CorsFilter 处理 OPTIONS 失败时已提交响应）已经调用了 response 的 commit 操作，
    // 此时再调用 setStatus / setContentType / getWriter 会抛 IllegalStateException——Tomcat 规范禁止在响应头已写出后修改
    if (response.isCommitted()) {  // 【做什么】HttpServletResponse.isCommitted() 检查响应头是否已发送到客户端 /【为什么】isCommitted=true 意味着响应已部分写出，无法再修改——跳过写入避免抛 IllegalStateException 导致 500 错误覆盖原始响应
      log.warn("LoginInterceptor: 响应已 committed,跳过 401 错误写入(可能是前置 Filter 已写入响应)");  // 【做什么】记录 WARN 日志说明为什么没写 401 /【为什么】这个日志在生产环境排查"用户明明没 token 却没收到 401"问题时很关键——能定位是哪个前置组件提前提交了响应
      return;  // 【做什么】安全返回不抛异常 /【为什么】比抛 500 错误更合理——客户端已收到前置 Filter 的响应，不应该被一个 500 错误覆盖
    }
    // 为什么 HTTP 状态码写 200 而不是 401（Unauthorized）：
    // 前端 axios 响应拦截器（src/api/request.js）统一检查 res.data.code（即 body 里的业务状态码）而非 HTTP status——
    // 原因：如果用 HTTP 401，axios 走 catch/error 回调而非 then/response 回调，后端返回的中文错误消息 "未登录" 会被 HttpError 对象吞掉，
    // 前端只能拿到 "Request failed with status code 401" 这种英文默认消息，用户看不懂。
    // 全系统统一约定 body-code-first：HTTP 始终返回 200，业务状态由 body.code 区分（200=成功、401=未认证、403=无权限、500=服务器错误）
    response.setStatus(HTTP_OK);  // 【做什么】HTTP 状态码固定设为 200 /【为什么】遵循 body-code-first 全栈约定——前端通过 res.data.code 判断（200/401/403/500），而非 HTTP status——保持与 GlobalExceptionHandler 一致
    response.setContentType("application/json;charset=UTF-8");  // 【做什么】声明响应内容类型为 JSON 格式 + UTF-8 字符编码 /【为什么】前端 axios 默认期望 JSON 响应（自动 JSON.parse），UTF-8 确保中文错误消息 "未登录" 不乱码
    var writer = response.getWriter();  // 【做什么】获取 PrintWriter 输出流，var 关键字简化类型声明（Java 21 特性） /【为什么】提前获取并复用 Writer 对象——getWriter() 每次调用都从 response 内部获取，多次调用虽返回同一对象但有微小性能损耗
    writer.write(  // 【做什么】将 JSON 字符串写入响应体 /【为什么】write 是 PrintWriter 的标准方法，内容会通过 HTTP 响应体发送给前端
        objectMapper.writeValueAsString(Result.error(ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMsg()))  // 【做什么】Jackson ObjectMapper 序列化 Result 对象为 JSON 字符串：{"code":401,"message":"未登录","data":null} /【为什么】使用 ErrorCode 枚举（Unauthorized=401,"未登录"）统一管理错误码——避免在拦截器里硬编码 401 和 "未登录"，错误码集中管理便于全局搜索和修改
    );
    writer.flush();  // 【做什么】强制将缓冲区中的数据立即发送到客户端（TCP 写出）/【为什么】不 flush 数据可能滞留在缓冲区——客户端迟迟收不到响应会触发 axios timeout（10 秒），用户体验差且浪费连接资源
    // Servlet 拦截器场景下不手动 close Writer：Tomcat 在请求结束时统一管理 ServletOutputStream/PrintWriter 的生命周期——
    // 手动 close 会导致后续 Filter 或 Handler 尝试写入时发现 Writer 已关闭而抛异常，引发连接中断
  }
}
