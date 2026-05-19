package com.example.finance.entity.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 预算创建/更新请求体（前端 BudgetPage.vue 弹窗 → POST /api/budget）
 *
 * 同一用户+同一分类+同一月份唯一约束（INSERT ON DUPLICATE KEY UPDATE）
 * 仅支出分类（category.type=1）可设置预算
 */
@Data
public class BudgetRequest {

  /** 关联分类 ID（必须存在且类型为支出 type=1） */
  @NotNull(message = "分类不能为空")
  private Long categoryId;

  /** 预算月份（yyyy-MM 格式，如 2026-05） */
  @NotBlank(message = "月份不能为空")
  @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "月份格式须为 yyyy-MM")
  private String month;

  /** 预算金额（DECIMAL(12,2)，必须 > 0.01） */
  @NotNull(message = "预算金额不能为空")
  @DecimalMin(value = "0.01", message = "预算金额必须大于0")
  private BigDecimal amount;
}
