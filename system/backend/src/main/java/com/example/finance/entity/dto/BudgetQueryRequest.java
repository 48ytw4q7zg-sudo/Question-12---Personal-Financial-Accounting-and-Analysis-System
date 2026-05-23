package com.example.finance.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 预算查询参数 DTO（BudgetController GET /api/budget + /api/budget/progress + /api/budget/alert）
 *
 * Q-CR修复：year/month 从 String(@Pattern) 改为 Integer(@Min/@Max)，消除脆弱的正则校验，
 * 与 StatisticsController 的 int year/month 参数风格保持一致。
 *
 * 调用方: controller/BudgetController.java → service/impl/BudgetServiceImpl.java
 */
@Data
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