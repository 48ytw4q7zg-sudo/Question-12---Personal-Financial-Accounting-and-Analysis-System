package com.example.finance.entity.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 账户批量收入汇总 DTO（替代 TransactionMapper.selectAccountIncomeBatch 返回的 Map<String, Object>）
 *
 * <p>用途：AccountServiceImpl.getBalance() 中批量查询各账户收入总额，
 * 替代原先从 Map 手动取值的缺乏类型安全模式。</p>
 *
 * <p>SQL 映射：TransactionMapper.xml selectAccountIncomeBatch</p>
 * <p>SQL: SELECT account_id, SUM(amount) as total_income FROM transaction WHERE user_id=? AND account_id IN (?) AND type=2 GROUP BY account_id</p>
 */
@Data
public class AccountBatchIncomeDTO {

  /** 账户 ID（GROUP BY 维度） */
  private Long accountId;

  /** 该账户下所有收入(type=2)记录的金额合计 */
  private BigDecimal totalIncome;
}