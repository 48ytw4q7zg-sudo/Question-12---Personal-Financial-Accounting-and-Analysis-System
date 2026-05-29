// ============================================================
// §1.4 数据流 节点 ⑥ — 请求进入后端第一个经过的文件（Filter 层 · 比 Interceptor 更早）
// §2.2 逐文件讲解 ①/⑩ — CorsConfig.java
//
// 这个文件做什么：创建 CorsFilter，允许前端 localhost:5173 跨域访问后端 8080
//
// 答辩讲什么：corsFilter() — CORS 为什么必须放 Filter 层（核心论点）
//   OPTIONS 预检没有 Authorization 头，放 Interceptor 层会被 LoginInterceptor 误拦返回 401
//
// ★ §1.4 数据流讲稿（节点 ⑥ · 直接念）：
//   "节点⑥，请求最先到达的是 CorsConfig 配置的 CorsFilter。它在 Filter 层——
//    比 Interceptor 更早执行。检查请求来源是否在允许列表中。为什么 CORS 必须放
//    Filter 层？因为浏览器的 OPTIONS 预检请求不带 Authorization 头——如果放
//    Interceptor 层，LoginInterceptor 发现没 token 就返回 401，预检失败，
//    所有跨域请求都被浏览器拦截。Filter 在协议层处理，预检直接放行。"
//
// ★ §2.2 核心代码讲稿（①/⑩ · 2分钟 · 直接念）：
//   "这是 CorsConfig.java，负责配置 CORS 跨域。前端跑 localhost:5173，
//    后端跑 localhost:8080——端口不同就是跨域，浏览器会拦截。
//    corsFilter() 方法：@Bean 告诉 Spring 这个方法返回一个全局过滤器。
//    配置了 8 项：允许哪些源、哪些 HTTP 方法、哪些请求头、是否允许携带凭证。
//    为什么 CORS 必须放 Filter 层？因为浏览器的 OPTIONS 预检请求不带
//    Authorization 头——如果放 Interceptor 层，LoginInterceptor 发现没
//    token 就返回 401，预检失败，前端所有跨域请求都被浏览器拦截。
//    Filter 比 Interceptor 更早执行，预检在 Filter 层就处理完了。"
//
// ▶ 逐文件讲解下一个（Ctrl+P）：
//   system/backend/src/main/java/com/example/finance/interceptor/LoginInterceptor.java
//   （§1.4 节点 ⑧ · §2.2 逐文件讲解 ②/⑩ — JWT 认证拦截器）
// ============================================================
package com.example.finance.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORS 跨域配置（OWASP A01 安全加固 · 区分开发/生产环境）
 *
 * <p>开发环境：allowedOrigins 默认只允许 localhost 前端端口（5173/5174）。</p>
 * <p>生产环境：必须通过 CORS_ALLOWED_ORIGINS 环境变量限定到具体域名，禁止通配。</p>
 * <p>安全约束：allowCredentials=true 时必须明确指定允许源，禁止 addAllowedOriginPattern("*")。</p>
 * <p>启动验证：生产环境检测到 localhost 默认值时阻止启动（对齐 JwtConfig 安全策略）。</p>
 *
 * <p>调用方：Spring Boot 自动装配，所有 HTTP 请求经过此 CorsFilter。</p>
 */
@Slf4j
@Configuration
public class CorsConfig {

  /** CORS 预检请求缓存时间（秒） */
  private static final long CORS_PREFLIGHT_CACHE_SECONDS = 3600L;
  /** CORS 允许的 HTTP 方法 */
  private static final List<String> ALLOWED_METHODS = Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS");
  /** CORS 允许的请求头 */
  private static final List<String> ALLOWED_HEADERS = Arrays.asList("Authorization", "Content-Type", "Accept");

  // OWASP A01: 必须通过 CORS_ALLOWED_ORIGINS 环境变量显式配置,禁止硬编码默认值
  // 开发环境: 通过 application-dev.yml 或 IDE 环境变量注入 localhost:5173,5174
  // 生产环境: 必须通过服务器环境变量注入具体域名(对齐 CLAUDE.md §一·二 安全规范)
  @Value("${cors.allowed-origins}")
  private String allowedOrigins;

  /** 当前激活的 Spring profile（用于区分开发/生产环境） */
  @Value("${spring.profiles.active:dev}")
  private String activeProfile;

  /** 生产环境启动校验：检测到 localhost 默认 CORS 值时阻止启动（对齐 JwtConfig） */
  @PostConstruct
  public void validateCorsOrigins() {  // 生产环境启动校验CORS配置安全性
    boolean hasLocalhost = Arrays.stream(allowedOrigins.split(","))  // 拆分允许源列表
        .map(String::trim)  // 去除空格
        .anyMatch(o -> o.contains("localhost"));  // 检测是否包含localhost
    if (hasLocalhost && "prod".equalsIgnoreCase(activeProfile)) {  // 生产环境包含localhost
      throw new IllegalStateException(  // 阻止启动(安全红线)
          "⚠️ 检测到 CORS allowedOrigins 包含 localhost，生产环境必须通过 CORS_ALLOWED_ORIGINS 环境变量替换为实际域名，应用拒绝启动");
    }
    if (hasLocalhost) {  // 开发环境包含localhost(允许但警告)
      log.warn("⚠️ 检测到 CORS allowedOrigins 包含 localhost（当前 profile={}），开发环境允许使用但生产环境必须替换！", activeProfile);  // 记录警告日志
    }
  }

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
  @Bean  // 【做什么】@Bean 标注后 Spring Boot 启动时自动调用此方法，返回的 CorsFilter 对象注册到 Spring 容器成为全局过滤器 /【为什么】所有 HTTP 请求在到达 Controller 之前都先经过这个过滤器，CORS 是 HTTP 协议层的事，必须在 Filter 层处理（比 Interceptor 更早执行）
  public CorsFilter corsFilter() {  // 【做什么】返回 Spring 提供的 CorsFilter 过滤器实例 /【为什么】在 Filter 层处理跨域而非 Interceptor 层——OPTIONS 预检请求没有 Authorization 头，如果放 Interceptor 层会被 LoginInterceptor 误拦返回 401，导致前端跨域请求彻底失败
    CorsConfiguration config = new CorsConfiguration();  // 【做什么】创建 CORS 配置对象，逐一设置允许的源/方法/请求头 /【为什么】不用构造器或 builder 一次性设置——每个配置项独立调用，便于在答辩中逐行讲解，配置项的添加顺序也反映了思考过程
    // 安全加固: addAllowedOrigin 逐个明确指定允许的源（application-dev.yml 中配置为 localhost:5173,5174）
    // 为什么不用 addAllowedOriginPattern("*") 通配符：OWASP A01 安全规范规定 allowCredentials=true 时必须逐个指定允许源——
    // 用通配符攻击者可以伪造 Origin 请求头，浏览器会把伪造的 Origin 当作"允许的源"从而绕过 CORS 限制发起 CSRF 攻击
    Arrays.stream(allowedOrigins.split(",")).map(String::trim).forEach(config::addAllowedOrigin);  // 【做什么】从配置文件读取逗号分隔的源列表，trim 去除空格后逐个添加到 CORS 允许列表 /【为什么】配置文件区分 dev/prod 环境——开发环境用 localhost:5173,5174，生产环境通过环境变量注入真实域名（如 https://finance.example.com）
    config.setAllowedMethods(ALLOWED_METHODS);  // 【做什么】设置允许的 HTTP 方法为 GET/POST/PUT/DELETE/OPTIONS 五种 /【为什么】覆盖 RESTful CRUD 完整操作——OPTIONS 必须包含（否则预检请求被拒绝），其他 4 种方法覆盖查询/新增/修改/删除全部业务场景
    config.setAllowedHeaders(ALLOWED_HEADERS);  // 【做什么】设置允许的请求头为 Authorization/Content-Type/Accept 三个 /【为什么】只开放业务必需的三个头，遵循最小暴露原则——Authorization 传 JWT token、Content-Type 标注 JSON 请求体、Accept 做内容协商
    config.setAllowCredentials(true);  // 【做什么】允许跨域请求携带 Cookie 和认证凭证（如 JWT token） /【为什么】前端 axios 在每个请求头里携带 Authorization: Bearer <token>，浏览器需要确认服务端允许接收凭证——设为 false 会导致带 token 的请求被浏览器直接拦截，前端所有接口调用报 CORS 错误
    config.addExposedHeader("Authorization");  // 【做什么】将 Authorization 响应头暴露给前端 JS 代码读取 /【为什么】浏览器默认不暴露 Authorization 头给 XMLHttpRequest/fetch API，不设此行前端 JS 无法通过代码读取这个响应头——虽然本项目 token 从登录响应 body 获取，但此配置为 token 刷新/续期场景预留
    // 预检请求缓存机制：浏览器在发送非简单请求（如带 Authorization 头的 POST）之前会先发一个 OPTIONS 预检请求询问服务端是否允许跨域——
    // 设置 maxAge 后浏览器在缓存有效期内直接用缓存结果，不再每次发预检请求，显著减少网络往返（RTT）
    config.setMaxAge(CORS_PREFLIGHT_CACHE_SECONDS);  // 【做什么】OPTIONS 预检结果缓存 1 小时（3600 秒） /【为什么】减少不必要的预检请求次数——开发调试阶段频繁刷新页面时每次刷新都发预检很浪费，缓存后浏览器直接从本地读取规则，页面响应更快

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();  // 【做什么】创建基于 URL 路径的 CORS 配置源 /【为什么】可以为不同路径设置不同 CORS 规则（如 "/api/public/**" 宽松 + "/api/admin/**" 严格）——本项目统一用一套规则，但保留扩展能力
    source.registerCorsConfiguration("/**", config);  // 【做什么】将 CORS 配置注册到所有路径 /【为什么】/** 是 Ant 风格通配符，匹配根路径及所有多级子路径——确保前端任何页面发起的请求都不会因路径不匹配而被跨域拦截
    return new CorsFilter(source);  // 【做什么】将配置源包装成 CorsFilter 实例返回给 Spring 容器 /【为什么】Spring 管理此对象的生命周期（单例模式），整个应用运行期间只有一个 CorsFilter 实例，所有 HTTP 请求共用同一个 CORS 规则
  }
}