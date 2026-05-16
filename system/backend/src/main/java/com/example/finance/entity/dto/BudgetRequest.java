package com.example.finance.entity.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 预算创建/更新请求
 */
@Data
public class BudgetRequest {

  @NotNull(message = "分类不能为空")
  private Long categoryId;

  @NotBlank(message = "月份不能为空")
  @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "月份格式须为 yyyy-MM")
  private String month;

  @NotNull(message = "预算金额不能为空")
  @DecimalMin(value = "0.01", message = "预算金额必须大于0")
  private BigDecimal amount;
}
