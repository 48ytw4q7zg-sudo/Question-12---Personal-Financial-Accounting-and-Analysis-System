package com.example.finance.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易记录创建/更新请求体（前端 TransactionListPage.vue「记一笔」弹窗 → POST/PUT /api/transaction）
 *
 * 校验规则：账户/分类/类型/金额/时间必填，金额 > 0，备注 ≤ 200 字符
 * 特殊约束：修改时如果记录是转账生成的（transferId 非空），仅允许修改备注
 */
@Data
public class TransactionRequest {

  /** 关联账户 ID（必须存在且状态=1 · @Min(1) 拦截 0 和负数传入数据库） */
  @NotNull(message = "账户不能为空")
  @Min(value = 1, message = "账户ID必须为正整数")
  private Long accountId;

  /** 关联分类 ID（必须存在 · @Min(1) 拦截 0 和负数传入数据库） */
  @NotNull(message = "分类不能为空")
  @Min(value = 1, message = "分类ID必须为正整数")
  private Long categoryId;

  // R-05-issue-2: 已修复 - type注释改为与Entity一致"1=收入 2=支出"
  /**
   * 交易类型：1=收入 2=支出
   */
  @NotNull(message = "类型不能为空")
  @Min(value = 1, message = "类型须为1(收入)或2(支出)")
  @Max(value = 2, message = "类型须为1(收入)或2(支出)")
  private Integer type;

  /** 交易金额（DECIMAL(12,2)，必须 > 0.01） */
  @NotNull(message = "金额不能为空")
  @DecimalMin(value = "0.01", message = "金额必须大于0")
  private BigDecimal amount;

  /** 备注（≤200 字符，可选） */
  @Size(max = 200, message = "备注长度不能超过200")
  private String note;

  /** 交易时间（yyyy-MM-dd HH:mm:ss 格式，通过 @JsonFormat 反序列化） */
  @NotNull(message = "交易时间不能为空")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime time;
}
