package com.example.finance.controller;

import com.example.finance.common.Result;
import com.example.finance.entity.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 健康检查控制器
 *
 * 职责：提供服务健康状态检查端点（被运维监控 / Docker HEALTHCHECK 调用）
 * 路由：/api/health（公开接口，无需 JWT，在 WebMvcConfig 中已排除拦截）
 * 无 Service/Mapper 依赖（直接读取 JVM 运行时信息）
 *
 * 接口清单：
 *   GET /api/health — 返回服务状态、应用名、运行时长
 *
 * 返回字段：
 *   status    — "UP" 表示服务正常
 *   app       — 应用名 "finance"
 *   timestamp — 当前服务器时间（yyyy-MM-dd HH:mm:ss）
 *   uptime    — 已运行时长（格式化：Xd Xh Xm / Xh Xm / Xm Xs）
 */
@RestController
public class HealthController {

  /** 日期格式化器：yyyy-MM-dd HH:mm:ss */
  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  /**
   * 健康检查端点 — 返回服务状态、运行时长、时间戳
   *
   * 被 WebMvcConfig 排除 JWT 拦截（/api/health 在 excludePathPatterns 中）
   * 被前端 / 后运维脚本 / Docker HEALTHCHECK 调用
   *
   * @return Result<HealthResponse> 服务状态信息
   */
  @GetMapping("/api/health")
  public Result<HealthResponse> health() {
    HealthResponse info = new HealthResponse();
    info.setStatus("UP");
    info.setApp("finance");
    info.setTimestamp(LocalDateTime.now().format(FMT));

    Duration uptime = Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime());
    info.setUptime(formatUptime(uptime));

    // 不暴露 Java 版本信息（安全考虑，防止攻击者针对特定版本漏洞利用）
    return Result.success(info);
  }

  /** 将运行时长格式化为可读字符串（如 "3d 5h 12m" / "5h 12m" / "12m 30s"） */
  private String formatUptime(Duration d) {
    long days = d.toDays();
    long hours = d.toHours() % 24;
    long minutes = d.toMinutes() % 60;
    if (days > 0) return String.format("%dd %dh %dm", days, hours, minutes);
    if (hours > 0) return String.format("%dh %dm", hours, minutes);
    return String.format("%dm %ds", minutes, d.toSeconds() % 60);
  }
}
