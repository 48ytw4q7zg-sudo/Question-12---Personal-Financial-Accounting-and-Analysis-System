package com.example.finance.entity.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 周期性账单创建/更新请求
 */
@Data
public class RecurringBillRequest {

  @NotBlank(message = "名称不能为空")
  @Size(min = 1, max = 50, message = "名称长度须在1-50之间")
  private String name;

  @NotNull(message = "账户不能为空")
  private Long accountId;

  @NotNull(message = "分类不能为空")
  private Long categoryId;

  @NotNull(message = "金额不能为空")
  @DecimalMin(value = "0.01", message = "金额必须大于0")
  private BigDecimal amount;

  /**
   * 类型：1=支出 2=收入
   */
  @NotNull(message = "类型不能为空")
  private Integer type;

  /**
   * 周期：daily weekly monthly yearly
   */
  @NotBlank(message = "周期不能为空")
  private String period;

  @NotBlank(message = "下次到期日不能为空")
  private String nextDueDate;
}
