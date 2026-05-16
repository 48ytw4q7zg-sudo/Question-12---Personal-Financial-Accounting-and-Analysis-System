package com.example.finance.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 账户创建/更新请求
 */
@Data
public class AccountRequest {

  @NotBlank(message = "账户名不能为空")
  @Size(min = 1, max = 20, message = "账户名长度须在1-20之间")
  private String name;

  @NotNull(message = "账户类型不能为空")
  @Min(value = 1, message = "账户类型须在1-4之间")
  @Max(value = 4, message = "账户类型须在1-4之间")
  private Integer type;

  @NotNull(message = "初始余额不能为空")
  @Min(value = 0, message = "初始余额不能为负数")
  private BigDecimal initialBalance;

  /**
   * 币种，默认 CNY
   */
  private String currency;
}
