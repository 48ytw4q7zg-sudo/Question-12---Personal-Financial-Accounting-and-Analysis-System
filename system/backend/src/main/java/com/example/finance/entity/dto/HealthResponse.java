package com.example.finance.entity.dto;

import lombok.Data;

/**
 * 健康检查响应 DTO（替代 Map<String, Object>）
 */
@Data
public class HealthResponse {

  /** 服务状态: "UP" */
  private String status;

  /** 应用名 */
  private String app;

  /** 当前服务器时间 (yyyy-MM-dd HH:mm:ss) */
  private String timestamp;

  /** 已运行时长 (如 "3d 5h 12m") */
  private String uptime;
}
