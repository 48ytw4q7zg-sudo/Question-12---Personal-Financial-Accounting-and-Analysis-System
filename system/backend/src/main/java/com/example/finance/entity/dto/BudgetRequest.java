// 数据传输对象包（DTO 层，用于接收前端请求和返回响应）
package com.example.finance.entity.dto;

// Jakarta Bean Validation — 数值下限校验（金额必须 > 0.01）
import jakarta.validation.constraints.DecimalMin;
// Jakarta Bean Validation — 整数下限校验（ID 必须 ≥ 1）
import jakarta.validation.constraints.Min;
// Jakarta Bean Validation — 非空白字符串校验（月份必填不能为空串）
import jakarta.validation.constraints.NotBlank;
// Jakarta Bean Validation — 非空校验（分类ID/金额必填）
import jakarta.validation.constraints.NotNull;
// Jakarta Bean Validation — 正则匹配校验（yyyy-MM 格式约束）
import jakarta.validation.constraints.Pattern;
// Lombok 自动生成 getter/setter/toString/equals/hashCode
import lombok.Data;

// Java 高精度金额类型（禁止 float/double 防精度丢失）
import java.math.BigDecimal;

/**
 * 预算创建/更新请求体 DTO（前端 BudgetPage.vue 弹窗 → POST /api/budget）
 *
 * 同一用户+同一分类+同一月份唯一约束（INSERT ON DUPLICATE KEY UPDATE）
 * 仅支出分类（category.type=1）可设置预算
 *
 * 调用方: controller/BudgetController.java → service/impl/BudgetServiceImpl.java
 */
// Lombok: 自动生成 getter/setter/toString/equals/hashCode（Lombok 1.18.46）
@Data
// 预算请求 DTO 类（前端表单 JSON → Controller @RequestBody @Valid 自动校验）
public class BudgetRequest {

  /** 关联分类 ID（必须存在且类型为支出 type=1，来源于 entity/Category.java 的主键） */
  @NotNull(message = "分类不能为空")
  @Min(value = 1, message = "分类ID必须为正整数")
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
