// 数据传输对象包（DTO 层，用于接收前端请求和返回响应）
package com.example.finance.entity.dto;

// Jakarta Bean Validation — 整数上限校验（年份 ≤ 2100 / 月份 ≤ 12）
import jakarta.validation.constraints.Max;
// Jakarta Bean Validation — 整数下限校验（年份 ≥ 2000 / 月份 ≥ 1）
import jakarta.validation.constraints.Min;
// Lombok 自动生成 getter/setter/toString/equals/hashCode
import lombok.Data;

/**
 * 预算查询参数 DTO（BudgetController GET /api/budget + /api/budget/progress + /api/budget/alert）
 *
 * Q-CR修复：year/month 从 String(@Pattern) 改为 Integer(@Min/@Max)，消除脆弱的正则校验，
 * 与 StatisticsController 的 int year/month 参数风格保持一致。
 *
 * 调用方: controller/BudgetController.java → service/impl/BudgetServiceImpl.java
 */
// Lombok: 自动生成 getter/setter/toString/equals/hashCode（Lombok 1.18.46）
@Data
// 预算查询请求 DTO 类（Controller 通过 @ModelAttribute 或 @RequestParam 自动绑定 GET 参数）
public class BudgetQueryRequest {

  /** 年份（2000-2100，可选，不提供时由 Service 层默认当前年 · null 表示未传入） */
  @Min(value = 2000, message = "年份需在2000-2100之间")
  @Max(value = 2100, message = "年份需在2000-2100之间")
  private Integer year;

  /** 月份（1-12，可选，不提供时由 Service 层默认当前月 · null 表示未传入） */
  @Min(value = 1, message = "月份需在1-12之间")
  @Max(value = 12, message = "月份需在1-12之间")
  private Integer month;
}