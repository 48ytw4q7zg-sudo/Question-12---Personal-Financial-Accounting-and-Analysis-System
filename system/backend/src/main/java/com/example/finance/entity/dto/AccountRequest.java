package com.example.finance.entity.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 账户创建/更新请求体（前端 AccountPage.vue 弹窗 → POST/PUT /api/account）
 *
 * 校验规则：名称 1-20 字符，类型 1-4，初始余额 ≥ 0
 * 币种默认 CNY，可选 USD/EUR/JPY/GBP/HKD/KRW
 */
@Data
public class AccountRequest {

  /** 账户名称（1-20 字符，必须非空） */
  @NotBlank(message = "账户名不能为空")
  @Size(min = 1, max = 20, message = "账户名长度须在1-20之间")
  private String name;

  /** 账户类型：1=现金 2=银行卡 3=支付宝 4=微信 */
  @NotNull(message = "账户类型不能为空")
  @Min(value = 1, message = "账户类型须在1-4之间")
  @Max(value = 4, message = "账户类型须在1-4之间")
  private Integer type;

  /** 初始余额（DECIMAL(12,2)，必须 ≥ 0） */
  @NotNull(message = "初始余额不能为空")
  @DecimalMin(value = "0.00", message = "初始余额不能为负数")
  private BigDecimal initialBalance;

  /**
   * 币种代码：CNY/USD/EUR/JPY/GBP/HKD/KRW，默认 CNY
   */
  @Pattern(regexp = "^(CNY|USD|EUR|JPY|GBP|HKD|KRW)$", message = "币种只能是CNY/USD/EUR/JPY/GBP/HKD/KRW")
  private String currency;
}
