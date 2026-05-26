package com.example.finance.entity.dto; // 声明该类属于 entity.dto 包（数据传输对象层，TransactionMapper.xml selectAccountExpenseBatch 的 MyBatis 结果映射）

import lombok.Data; // Lombok: 自动生成 getter/setter/toString/equals/hashCode（减少样板代码）

import java.math.BigDecimal; // Java 精确十进制类型（totalExpense 金额字段，禁止 float/double 防精度丢失）

/**
 * 账户批量支出汇总 DTO（替代 TransactionMapper.selectAccountExpenseBatch 返回的 Map&lt;String, Object&gt;）
 *
 * <p>用途：AccountServiceImpl.getBalance() 中批量查询各账户支出总额，</p>
 * <p>替代原先从 Map 手动取值（map.get("totalExpense")）的缺乏类型安全模式。</p>
 *
 * <p>SQL 映射：TransactionMapper.xml selectAccountExpenseBatch</p>
 * <p>SQL 语句：<pre>
 * SELECT account_id, SUM(amount) AS total_expense
 * FROM transaction
 * WHERE user_id = #{userId} AND account_id IN (...)
 *   AND type = 2
 * GROUP BY account_id
 * </pre>
 * type=2 对应 TransactionType.EXPENSE（支出）。</p>
 *
 * <p>跨文件引用：AccountServiceImpl.getBalance() → TransactionMapper.selectAccountExpenseBatch() → List&lt;AccountBatchExpenseDTO&gt;。</p>
 */
@Data // Lombok: 自动生成 getter/setter/toString/equals/hashCode
public class AccountBatchExpenseDTO {

  /** 账户 ID（GROUP BY 聚合维度的键 — 对应 TransactionMapper.xml selectAccountExpenseBatch 结果集中的 account_id 列） */
  private Long accountId;

  /** 该账户下所有支出(type=2)记录的金额合计 — TransactionMapper.xml 使用 t.type=2（支出，TransactionType.EXPENSE）过滤 */
  private BigDecimal totalExpense;
}
