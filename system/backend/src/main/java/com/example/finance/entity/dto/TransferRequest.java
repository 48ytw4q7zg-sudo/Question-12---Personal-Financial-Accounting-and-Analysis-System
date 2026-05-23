package com.example.finance.entity.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 转账请求体（前端 TransferPage.vue → POST /api/transaction/transfer）
 *
 * 校验规则：转出/转入账户必填且不可相同，金额 > 0
 * 业务约束：转出账户余额必须 ≥ 转账金额（Service 层校验）
 */
@Data
public class TransferRequest {

  /** 转出账户 ID（必须存在且余额充足 · @Min(1) 拦截 0 和负数传入数据库） */
  @NotNull(message = "转出账户不能为空")
  @Min(value = 1, message = "转出账户ID必须为正整数")
  private Long fromAccountId;

  /** 转入账户 ID（必须存在且 ≠ fromAccountId · @Min(1) 拦截 0 和负数传入数据库） */
  @NotNull(message = "转入账户不能为空")
  @Min(value = 1, message = "转入账户ID必须为正整数")
  private Long toAccountId;

  /** 转账金额（DECIMAL(12,2)，必须 > 0.01） */
  @NotNull(message = "转账金额不能为空")
  @DecimalMin(value = "0.01", message = "转账金额必须大于0")
  private BigDecimal amount;

  /** 转账备注（≤200 字符，可选） */
  @Size(max = 200, message = "备注长度不能超过200")
  private String note;
}
