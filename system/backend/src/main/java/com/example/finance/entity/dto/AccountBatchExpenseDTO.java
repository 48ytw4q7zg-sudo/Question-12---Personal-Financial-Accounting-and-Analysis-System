package com.example.finance.entity.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 账户批量支出汇总 DTO（替代 TransactionMapper.selectAccountExpenseBatch 返回的 Map<String, Object>）
 *
 * <p>用途：AccountServiceImpl.getBalance() 中批量查询各账户支出总额，
 * 替代原先从 Map 手动取值的缺乏类型安全模式。</p>
 *
 * <p>SQL 映射：TransactionMapper.xml selectAccountExpenseBatch</p>
 * <p>SQL: SELECT account_id, SUM(amount) as total_expense FROM transaction WHERE user_id=? AND account_id IN (?) AND type=2 GROUP BY account_id（type=2=支出，TransactionType.EXPENSE）</p>
 */
@Data
public class AccountBatchExpenseDTO {

  /** 账户 ID（GROUP BY 维度） — TransactionMapper.xml selectAccountExpenseBatch */
  private Long accountId;

  /** 该账户下所有支出(type=2)记录的金额合计 — TransactionMapper.xml 使用 t.type=2（支出） */
  private BigDecimal totalExpense;
}