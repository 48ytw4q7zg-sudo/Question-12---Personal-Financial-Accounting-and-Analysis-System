// ╔══════════════════════════════════════════════════════════════════════╗
// ║  📋 答辩文件 ③/⑦ — 核心代码讲解 → WebMvcConfig：拦截器注册 + 白名单       ║
// ║                                                                      ║
// ║  【文件整体实现什么】                                                    ║
// ║  WebMvcConfig.java — Spring MVC 配置类，放在 config/ 目录                   ║
// ║  addInterceptors() 方法（第 78-101 行）注册两个拦截器：                       ║
// ║    ① LoginInterceptor — 拦截 /api/v1/**，白名单放行 4 个接口                    ║
// ║    ② AdminInterceptor — 拦截 /api/v1/admin/**，校验管理员 role=1               ║
// ║                                                                      ║
// ║  【答辩要讲什么】                                                        ║
// ║  重点讲 addInterceptors() 方法（第 78-101 行）——拦截范围 + 白名单 + 执行顺序        ║
// ║  白名单 4 个接口：login / register / health / category（未登录用户需要）         ║
// ║                                                                      ║
// ║  【具体讲稿】                                                           ║
// ║  滚到第 78 行 addInterceptors()："Spring 启动时自动调用，注册拦截器。              ║
// ║    LoginInterceptor 拦截 /api/v1/**，但 4 个接口放行——                             ║
// ║    login/register 用户还没 token，health 运维监控，category 种子数据公开。          ║
// ║    AdminInterceptor 只拦 /api/v1/admin/**，依赖 LoginInterceptor 存的 role。       ║
// ║    执行顺序：LoginInterceptor → AdminInterceptor → Controller。"                 ║
// ╚══════════════════════════════════════════════════════════════════════╝
//
// ▶ 讲完后，下一个文件（★ 30 分重点，按 Ctrl+P 粘贴打开）：
//   system/backend/src/main/java/com/example/finance/service/impl/UserServiceImpl.java
//   （login() 方法 — 答辩要求自选的 1 个后端 Service 方法）
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
 *   <li>/api/v1/category — 分类列表接口（种子数据，公开只读 · Q-CR 修复）</li>
 *   <li>/api/v1/exchange-rate — 汇率查询接口（参考数据，公开只读 · Q-CR 修复）</li>
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
  @Override  // 【做什么】实现 WebMvcConfigurer 接口的 addInterceptors 方法 /【为什么】Spring Boot 启动时自动调用——将拦截器注册到 Spring MVC 的拦截器链中，按注册顺序依次执行 preHandle
  public void addInterceptors(InterceptorRegistry registry) {  // 【做什么】注册拦截器到拦截器链 /【为什么】registry 是 Spring MVC 内部维护的拦截器注册表，addInterceptor 决定拦截器顺序，addPathPatterns 决定拦截范围，excludePathPatterns 决定白名单——三个方法组合实现声明式路由级别的权限控制

    // ========== 拦截器 #1：LoginInterceptor（JWT 校验 + 解析 userId/role） ==========
    // addInterceptor 的执行顺序：Spring MVC 按注册顺序依次调用 preHandle——LoginInterceptor 先注册先执行，
    // 它的预检/校验/解析结果是 AdminInterceptor 的前置条件（role 存入 request 后 AdminInterceptor 才能读）
    registry.addInterceptor(loginInterceptor)  // 【做什么】注册登录拦截器实例 /【为什么】loginInterceptor 由 @Component + @RequiredArgsConstructor 构造器注入——Spring 自动将其注入到 WebMvcConfig，在此注册到拦截器链
        .addPathPatterns("/api/v1/**")  // 【做什么】拦截所有 /api/v1/ 开头的请求 /【为什么】** 是 Ant 风格通配符匹配任意多级路径（如 /api/v1/user/login、/api/v1/transaction/list?page=1、/api/v1/admin/user/1）——不能用 /* 因为 /* 只匹配一级路径（如 /api/v1/user），无法匹配多级（/api/v1/user/login 是两级），会导致大量接口漏拦截
        .excludePathPatterns(  // 【做什么】配置白名单——这些路径不经过 LoginInterceptor 校验 /【为什么】白名单里的接口用户尚未登录就需要访问，如果不放行会造成死循环（没有 token → 被拦 401 → 跳登录页 → 登录接口也被拦 → 永远无法登录）
            "/api/v1/user/login",      // 【为什么放行】登录接口：用户输入用户名密码获取 JWT token——此时用户还没有 token，如果拦截会导致永远无法登录（死循环）
            "/api/v1/user/register",   // 【为什么放行】注册接口：新用户创建账号——尚未拥有 token，必须放行否则系统无法积累用户
            "/api/v1/health",          // 【为什么放行】健康检查接口：Docker healthcheck / K8s liveness probe / 运维监控（如 Prometheus）定期调用——这些调用来自机器而非用户，不带 token，拦截会导致监控系统误判服务不健康、触发自动重启
            "/api/v1/category"         // 【为什么放行】分类列表接口：系统内置的收支分类种子数据（如"餐饮""交通""工资"），公开只读不涉及用户隐私——前端注册页面的分类下拉框也需要在未登录时加载展示选项
            // P0-3 修复(Q-CR Loop1)：/api/v1/exchange-rate 移出白名单,要求登录访问
            // 原因:汇率服务虽是参考数据但属业务接口,公开会被恶意爬取/DDoS;前端用户访问时已登录,无破坏正常使用
            // 后续如需公开,可考虑加 IP 限流或匿名接口移到独立 controller
        );

    // ========== 拦截器 #2：AdminInterceptor（管理员权限校验 · Q-CR HIGH 修复） ==========
    // AdminInterceptor 只拦截 /api/v1/admin/** 路径的管理功能（用户管理、数据维护等）——不拦截 /api/v1/transaction 等普通业务接口，
    // 减少不必要的权限检查开销（每个请求都做 role 检查会多一次 request.getAttribute 调用，虽然开销很小但没必要）
    // 拦截器链执行顺序至关重要：LoginInterceptor 先执行（preHandle 校验 token + 调用 request.setAttribute("role", ...)），
    // AdminInterceptor 后执行（preHandle 调用 request.getAttribute("role") 读取角色）——顺序颠倒会导致 AdminInterceptor 读不到 role，所有请求（含管理员）都被误判为无权限
    // 非管理员访问时在 AdminInterceptor 中抛出 BusinessException(6004, "无管理员权限")，由 GlobalExceptionHandler 的 @ExceptionHandler(BusinessException.class) 统一捕获并转换为 Result.error(6004, "无管理员权限")——Controller 层不需要在每个管理接口方法里手动写 checkAdmin()
    registry.addInterceptor(adminInterceptor)  // 【做什么】注册管理员权限拦截器 /【为什么】继承了 LoginInterceptor 通过 request.setAttribute 存入的 role 属性，通过 addPathPatterns 限定只拦截 admin 路径——最小拦截范围，不干扰普通业务流程
        .addPathPatterns("/api/v1/admin/**");  // 【做什么】仅拦截 /api/v1/admin/ 下的所有接口 /【为什么】** 通配符匹配管理模块所有多级路径（如 /api/v1/admin/user/list、/api/v1/admin/user/delete/1）——普通接口（/api/v1/transaction/** 等）不走 AdminInterceptor，减少不必要的拦截器开销
  }
}
