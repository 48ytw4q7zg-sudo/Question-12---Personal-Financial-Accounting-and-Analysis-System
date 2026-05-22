package com.example.finance.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 预算查询参数 DTO（BudgetController GET /api/budget + /api/budget/progress + /api/budget/alert）
 *
 * 校验规则：year 须为 2000-2100 的合法数字，month 须为 1-12 的合法数字
 * 前端传入格式：year=2026&month=5（均为可选参数）
 */
@Data
public class BudgetQueryRequest {

  /** 年份（2000-2100，可选，不提供时默认当前年） */
  @Pattern(regexp = "^$|^(200[0-9]|20[1-9][0-9]|2100)$", message = "year需在2000-2100之间")
  private String year;

  /** 月份（1-12，可选，不提供时默认当前月） */
  @Pattern(regexp = "^$|^([1-9]|10|11|12)$", message = "month需在1-12之间")
  private String month;
}