package com.example.finance.entity.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 周期性账单创建/更新请求体（前端 RecurringBillPage.vue 弹窗 → POST/PUT /api/recurring-bill）
 *
 * 校验规则：名称 1-50 字符，账户/分类/金额/类型/周期/到期日必填
 * 业务约束：关联账户必须存在且状态=1，到期日是 @Scheduled 判断依据不可为空
 */
@Data
public class RecurringBillRequest {

  /** 账单名称（1-50 字符，如「月房租」「月工资」） */
  @NotBlank(message = "名称不能为空")
  @Size(min = 1, max = 50, message = "名称长度须在1-50之间")
  private String name;

  /** 关联账户 ID（必须存在且 status=1） */
  @NotNull(message = "账户不能为空")
  private Long accountId;

  /** 关联分类 ID（必须存在） */
  @NotNull(message = "分类不能为空")
  private Long categoryId;

  /** 金额（DECIMAL(12,2)，必须 > 0.01） */
  @NotNull(message = "金额不能为空")
  @DecimalMin(value = "0.01", message = "金额必须大于0")
  private BigDecimal amount;

  /**
   * 类型：1=支出 2=收入
   */
  @NotNull(message = "类型不能为空")
  private Integer type;

  /**
   * 周期：daily=每日, weekly=每周, monthly=每月, yearly=每年（Q-CR v11 acceptance #29）
   */
  @NotBlank(message = "周期不能为空")
  @Pattern(regexp = "^(daily|weekly|monthly|yearly)$",
      message = "周期只能是 daily / weekly / monthly / yearly")
  private String period;

  /** 下次到期日（yyyy-MM-dd 格式，必填，空值会导致 @Scheduled 逻辑异常） */
  @NotBlank(message = "下次到期日不能为空")
  private String nextDueDate;
}
