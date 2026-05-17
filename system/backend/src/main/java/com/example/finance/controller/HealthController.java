package com.example.finance.controller;

import com.example.finance.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查控制器
 */
@RestController
public class HealthController {

  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  /**
   * 健康检查 — 返回服务状态、运行时长、时间戳
   */
  @GetMapping("/api/health")
  public Result<Map<String, Object>> health() {
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("status", "UP");
    info.put("app", "finance");
    info.put("timestamp", LocalDateTime.now().format(FMT));

    Duration uptime = Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime());
    info.put("uptime", formatUptime(uptime));

    info.put("java", System.getProperty("java.version"));
    return Result.success(info);
  }

  private String formatUptime(Duration d) {
    long days = d.toDays();
    long hours = d.toHours() % 24;
    long minutes = d.toMinutes() % 60;
    if (days > 0) return String.format("%dd %dh %dm", days, hours, minutes);
    if (hours > 0) return String.format("%dh %dm", hours, minutes);
    return String.format("%dm %ds", minutes, d.toSeconds() % 60);
  }
}
