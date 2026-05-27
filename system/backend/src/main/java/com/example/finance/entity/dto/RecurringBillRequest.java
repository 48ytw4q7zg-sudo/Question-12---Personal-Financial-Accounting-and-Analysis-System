// 数据传输对象包（DTO 层，用于接收前端请求和返回响应）
package com.example.finance.entity.dto;

// Jakarta Bean Validation — 数值下限校验（金额必须 > 0.01）
import jakarta.validation.constraints.DecimalMin;
// Jakarta Bean Validation — 整数上限校验（类型须为 1 或 2）
import jakarta.validation.constraints.Max;
// Jakarta Bean Validation — 整数下限校验（ID ≥ 1 / 类型 ≥ 1）
import jakarta.validation.constraints.Min;
// Jakarta Bean Validation — 非空白字符串校验（名称/周期/到期日必填）
import jakarta.validation.constraints.NotBlank;
// Jakarta Bean Validation — 非空校验（账户/分类/金额/类型必填）
import jakarta.validation.constraints.NotNull;
// Jakarta Bean Validation — 正则匹配校验（周期值枚举 / 日期格式）
import jakarta.validation.constraints.Pattern;
// Jakarta Bean Validation — 字符串长度校验（名称 1-30 字符）
import jakarta.validation.constraints.Size;
// Lombok 自动生成 getter/setter/toString/equals/hashCode
import lombok.Data;

// Java 高精度金额类型（禁止 float/double 防精度丢失）
import java.math.BigDecimal;

/**
 * 周期性账单创建/更新请求体 DTO（前端 RecurringBillPage.vue 弹窗 → POST/PUT /api/recurring-bill）
 *
 * 校验规则：名称 1-30 字符（@Size(max=30)），账户/分类/金额/类型/周期/到期日必填
 * 业务约束：关联账户必须存在且状态=1（Service 层校验），到期日是 @Scheduled 判断依据不可为空
 *
 * 调用方: controller/RecurringBillController.java → service/impl/RecurringBillServiceImpl.java
 */
// Lombok: 自动生成 getter/setter/toString/equals/hashCode（Lombok 1.18.46）
@Data
// 周期账单请求 DTO 类（前端表单 JSON → Controller @RequestBody @Valid 自动校验）
public class RecurringBillRequest {

  /** 账单名称（1-30 字符，如「月房租」「月工资」，对齐 PRD P1-4 数据约束） */
  @NotBlank(message = "名称不能为空")
  @Size(min = 1, max = 30, message = "名称长度须在1-30之间")
  private String name;

  /** 关联账户 ID（必须存在且 status=1，来源于 entity/Account.java 的主键） */
  @NotNull(message = "账户不能为空")
  @Min(value = 1, message = "账户ID必须为正整数")
  private Long accountId;

  /** 关联分类 ID（必须存在，来源于 entity/Category.java 的主键） */
  @NotNull(message = "分类不能为空")
  @Min(value = 1, message = "分类ID必须为正整数")
  private Long categoryId;

  /** 金额（DECIMAL(12,2)，必须 > 0.01） */
  @NotNull(message = "金额不能为空")
  @DecimalMin(value = "0.01", message = "金额必须大于0")
  private BigDecimal amount;

  /**
   * 类型：1=收入 2=支出
   */
  @NotNull(message = "类型不能为空")
  @Min(value = 1, message = "类型须为1(收入)或2(支出)")
  @Max(value = 2, message = "类型须为1(收入)或2(支出)")
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
  @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "到期日格式须为yyyy-MM-dd")
  private String nextDueDate;
}
