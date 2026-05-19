package com.example.finance.entity.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 账户余额数据传输对象（前端 AccountPage.vue 余额卡片 / DashboardPage.vue 总资产）
 *
 * 当前余额 = 初始余额 + 总收入 - 总支出（实时计算，不做缓存）
 * 优化：批量查询消除 N+1 问题（TransactionMapper.selectAccountIncomeBatch/ExpenseBatch）
 */
@Data
public class AccountBalanceDTO {

  /** 账户 ID */
  private Long accountId;

  /** 账户名称（如「招商银行卡」） */
  private String accountName;

  /** 账户类型：1=现金 2=银行卡 3=支付宝 4=微信 */
  private Integer accountType;

  /** 初始余额（创建账户时设置的金额） */
  private BigDecimal initialBalance;

  /** 该账户下所有 type=1（收入）记录的金额合计 */
  private BigDecimal totalIncome;

  /** 该账户下所有 type=2（支出）记录的金额合计 */
  private BigDecimal totalExpense;

  /** 当前余额 = initialBalance + totalIncome - totalExpense */
  private BigDecimal currentBalance;
}
