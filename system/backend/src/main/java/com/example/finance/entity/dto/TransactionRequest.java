package com.example.finance.entity.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易记录创建/更新请求
 */
@Data
public class TransactionRequest {

  @NotNull(message = "账户不能为空")
  private Long accountId;

  @NotNull(message = "分类不能为空")
  private Long categoryId;

  // R-05-issue-2: 已修复 - type注释改为与Entity一致"1=收入 2=支出"
  /**
   * 类型：1=收入 2=支出
   */
  @NotNull(message = "类型不能为空")
  private Integer type;

  @NotNull(message = "金额不能为空")
  @DecimalMin(value = "0.01", message = "金额必须大于0")
  private BigDecimal amount;

  @Size(max = 200, message = "备注长度不能超过200")
  private String note;

  @NotNull(message = "交易时间不能为空")
  private LocalDateTime time;
}
