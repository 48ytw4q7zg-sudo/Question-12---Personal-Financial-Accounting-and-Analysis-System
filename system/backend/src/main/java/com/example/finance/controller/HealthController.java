package com.example.finance.controller;

import com.example.finance.common.Result;
import com.example.finance.entity.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 健康检查控制器
 *
 * <p>职责：提供服务健康状态检查端点（被运维监控 / Docker HEALTHCHECK / 前端运行时探针调用）。</p>
 * <p>路由：/api/v1/health（公开接口，无需 JWT，在 config/WebMvcConfig.java 的 excludePathPatterns 中已排除拦截）。</p>
 * <p>无 Service/Mapper 依赖（直接读取 JVM 运行时信息，零外部依赖，故障隔离）。</p>
 *
 * <p>接口清单:</p>
 * <pre>
 *   GET /api/v1/health — 返回服务状态、应用名、运行时长、时间戳
 * </pre>
 *
 * <p>返回字段:</p>
 * <ul>
 *   <li>status    — "UP" 表示服务正常（如果进程存活即返回 UP，不含 DB/Redis 等深层探针）</li>
 *   <li>app       — 应用名 "finance"</li>
 *   <li>timestamp — 当前服务器时间（yyyy-MM-dd HH:mm:ss）</li>
 *   <li>uptime    — 已运行时长（格式化：3d 5h 12m / 5h 12m / 12m 30s）</li>
 * </ul>
 *
 * <p>调用方:</p>
 * <ul>
 *   <li>前端启动探针: api/request.js 响应拦截器 401 跳转前探活（非 200 说明后端挂了）</li>
 *   <li>Docker HEALTHCHECK: curl http://localhost:8080/api/v1/health</li>
 *   <li>运维监控: ./health.sh 脚本 / K8s livenessProbe / 云平台健康检查</li>
 * </ul>
 *
 * <p>安全设计: 不暴露 Java 版本号、内存使用、线程数等运维信息，防止攻击者针对特定版本漏洞利用。</p>
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

  /** 日期格式化器：yyyy-MM-dd HH:mm:ss（static final 单例复用） */
  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  /** 服务状态常量：正常 */
  private static final String STATUS_UP = "UP";
  /** 应用名常量：finance */
  private static final String APP_NAME = "finance";

  /**
   * 健康检查端点 — 返回服务状态、运行时长、时间戳
   *
   * <p>被 config/WebMvcConfig.java 排除 JWT 拦截（/api/v1/health 在 excludePathPatterns 中）。</p>
   * <p>无需任何参数，进程存活即可返回 200 OK。</p>
   * <p>不查询数据库、不调用外部 API，故障隔离确保自身可用。</p>
   *
   * <p>调用方:</p>
   * <ul>
   *   <li>→ 前端 api/request.js 启动探针（页面加载前确认后端在线）</li>
   *   <li>→ Docker HEALTHCHECK 指令</li>
   *   <li>→ 运维 shell 脚本 curl 探活</li>
   * </ul>
   *
   * @return Result&lt;HealthResponse&gt; 服务状态信息（status="UP", app="finance", timestamp=当前时间, uptime=格式化运行时长）
   */
  @GetMapping
  public Result<HealthResponse> health() {
    // 【步骤①】构造健康检查响应
    HealthResponse info = new HealthResponse();                        // 健康检查响应对象
    info.setStatus(STATUS_UP);                                         // 状态：UP（进程存活）
    info.setApp(APP_NAME);                                             // 应用名：finance
    info.setTimestamp(LocalDateTime.now().format(FMT));                // 时间戳：yyyy-MM-dd HH:mm:ss

    // 【步骤②】获取 JVM 运行时长（→ java.lang.management.ManagementFactory · JDK 标准 API）
    Duration uptime = Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime());
    info.setUptime(formatUptime(uptime));                              // → this.formatUptime() · 格式化为可读字符串

    // 【步骤③】返回（不暴露 Java 版本信息，安全考虑）
    return Result.success(info);                                       // → common/Result.java Result.success()
  }

  /** 将运行时长 Duration 格式化为可读字符串
   *  <p>规则：≥1天 → "3d 5h 12m" / ≥1小时 → "5h 12m" / 其他 → "12m 30s"</p>
   *  <p>调用方: this.health()（同 Controller 内 · HealthController.java 第61行）</p> */
  private String formatUptime(Duration d) {
    long days = d.toDays();                                            // 提取天数
    long hours = d.toHours() % 24;                                     // 提取小时（模24去天）
    long minutes = d.toMinutes() % 60;                                 // 提取分钟（模60去小时）
    if (days > 0) return String.format("%dd %dh %dm", days, hours, minutes);  // ≥1天 → 含天格式
    if (hours > 0) return String.format("%dh %dm", hours, minutes);    // ≥1小时 → 含小时格式
    return String.format("%dm %ds", minutes, d.toSeconds() % 60);     // <1小时 → 含秒格式
  }
}
