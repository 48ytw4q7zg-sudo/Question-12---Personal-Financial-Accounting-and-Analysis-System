package com.example.finance.controller;

import com.example.finance.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查控制器
 */
@RestController
public class HealthController {

  /**
   * 健康检查
   */
  @GetMapping("/api/health")
  public Result<Map<String, String>> health() {
    return Result.success(Map.of("status", "UP"));
  }
}
