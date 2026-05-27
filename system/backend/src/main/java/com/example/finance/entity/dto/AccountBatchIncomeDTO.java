package com.example.finance.entity.dto; // 声明该类属于 entity.dto 包（数据传输对象层，TransactionMapper.xml selectAccountIncomeBatch 的 MyBatis 结果映射）

import lombok.Data; // Lombok: 自动生成 getter/setter/toString/equals/hashCode（减少样板代码）

import java.math.BigDecimal; // Java 精确十进制类型（totalIncome 金额字段，禁止 float/double 防精度丢失）

/**
 * 账户批量收入汇总 DTO（替代 TransactionMapper.selectAccountIncomeBatch 返回的 Map&lt;String, Object&gt;）
 *
 * <p>用途：AccountServiceImpl.getBalance() 中批量查询各账户收入总额，</p>
 * <p>替代原先从 Map 手动取值（map.get("totalIncome")）的缺乏类型安全模式。</p>
 *
 * <p>SQL 映射：TransactionMapper.xml selectAccountIncomeBatch</p>
 * <p>SQL 语句：<pre>
 * SELECT account_id, SUM(amount) AS total_income
 * FROM transaction
 * WHERE user_id = #{userId} AND account_id IN (...)
 *   AND type = 1
 * GROUP BY account_id
 * </pre>
 * type=1 对应 TransactionType.INCOME（收入）。</p>
 *
 * <p>跨文件引用：AccountServiceImpl.getBalance() → TransactionMapper.selectAccountIncomeBatch() → List&lt;AccountBatchIncomeDTO&gt;
 * 与 AccountBatchExpenseDTO 配对使用，分别获取收入/支出合计后计算 currentBalance。</p>
 */
@Data // Lombok: 自动生成 getter/setter/toString/equals/hashCode
public class AccountBatchIncomeDTO {

  /** 账户 ID（GROUP BY 聚合维度的键 — 对应 TransactionMapper.xml selectAccountIncomeBatch 结果集中的 account_id 列） */
  private Long accountId;

  /** 该账户下所有收入(type=1)记录的金额合计 — TransactionMapper.xml 使用 t.type=1（收入，TransactionType.INCOME）过滤 */
  private BigDecimal totalIncome;
}
