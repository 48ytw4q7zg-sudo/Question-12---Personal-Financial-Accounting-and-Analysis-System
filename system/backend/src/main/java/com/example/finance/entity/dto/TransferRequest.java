// 数据传输对象包（DTO 层，用于接收前端请求和返回响应）
package com.example.finance.entity.dto;

// Jakarta Bean Validation — 数值下限校验（金额必须 > 0）
import jakarta.validation.constraints.DecimalMin;
// Jakarta Bean Validation — 整数下限校验（ID 必须 ≥ 1）
import jakarta.validation.constraints.Min;
// Jakarta Bean Validation — 非空校验（必填字段）
import jakarta.validation.constraints.NotNull;
// Jakarta Bean Validation — 字符串长度校验（备注 ≤ 200 字符）
import jakarta.validation.constraints.Size;
// Lombok 自动生成 getter/setter/toString/equals/hashCode
import lombok.Data;

// Java 高精度金额类型（禁止 float/double 防精度丢失）
import java.math.BigDecimal;

/**
 * 转账请求体 DTO（前端 TransferPage.vue → POST /api/transaction/transfer）
 *
 * 校验规则：转出/转入账户必填且不可相同，金额 > 0
 * 业务约束：转出账户余额必须 ≥ 转账金额（Service 层校验）
 *
 * 调用方: controller/TransactionController.java → service/impl/TransactionServiceImpl.java
 */
// Lombok: 自动生成 getter/setter/toString/equals/hashCode（Lombok 1.18.46）
@Data
// 转账请求 DTO 类（前端表单 JSON → Controller @RequestBody 自动反序列化）
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
