// 数据传输对象包（DTO 层，用于接收前端请求和返回响应）
package com.example.finance.entity.dto;

// Lombok 自动生成 getter/setter/toString/equals/hashCode
import lombok.Data;

/**
 * 健康检查响应 DTO（替代原始 Map<String, Object>，类型安全）
 *
 * 供运维探活使用：k8s liveness probe / Docker HEALTHCHECK / 前端健康检查页
 * GET /api/health → Result<HealthResponse>（放行，不走 JWT 拦截器）
 *
 * 调用方: controller/HealthController.java（无需认证，WebMvcConfig 配置放行）
 */
// Lombok: 自动生成 getter/setter/toString/equals/hashCode（Lombok 1.18.46）
@Data
// 健康检查响应 DTO 类（Controller 通过 Result<HealthResponse> 返回探活端）
public class HealthResponse {

  /** 服务状态（"UP" 表示正常运行 / "DOWN" 表示不可用，k8s liveness probe 以此判断是否重启 Pod） */
  private String status;

  /** 应用名称（如 "Personal Finance Accounting System"，标识当前服务） */
  private String app;

  /** 当前服务器时间（yyyy-MM-dd HH:mm:ss 格式，验证服务时间是否正确） */
  private String timestamp;

  /** 已运行时长（如 "3d 5h 12m"，由 ManagementFactory.getRuntimeMXBean().getUptime() 计算） */
  private String uptime;
}
