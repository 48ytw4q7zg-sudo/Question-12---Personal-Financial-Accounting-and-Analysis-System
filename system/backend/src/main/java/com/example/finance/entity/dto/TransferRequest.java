package com.example.finance.entity.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 转账请求
 */
@Data
public class TransferRequest {

  @NotNull(message = "转出账户不能为空")
  private Long fromAccountId;

  @NotNull(message = "转入账户不能为空")
  private Long toAccountId;

  @NotNull(message = "转账金额不能为空")
  @DecimalMin(value = "0.01", message = "转账金额必须大于0")
  private BigDecimal amount;

  @Size(max = 200, message = "备注长度不能超过200")
  private String note;
}
