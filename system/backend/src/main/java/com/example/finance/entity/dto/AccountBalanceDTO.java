package com.example.finance.entity.dto; // 声明该类属于 entity.dto 包（数据传输对象层，AccountController 余额查询 → 前端 AccountPage.vue / DashboardPage.vue JSON 响应）

import lombok.Data; // Lombok: 自动生成 getter/setter/toString/equals/hashCode（减少样板代码）

import java.math.BigDecimal; // Java 精确十进制类型（所有金额字段，禁止 float/double 防精度丢失，对齐 DATABASE_DESIGN.md §3 #6）

/**
 * 账户余额数据传输对象（AccountController.getBalance() → 前端 AccountPage.vue 余额卡片 / DashboardPage.vue 总资产概览）
 *
 * <p>数据库来源：</p>
 * <ul>
 *   <li>account 表：accountId / accountName / accountType / initialBalance / currency</li>
 *   <li>transaction 表聚合：totalIncome（SUM(amount) WHERE type=1 收入）/ totalExpense（SUM(amount) WHERE type=2 支出）</li>
 *   <li>计算公式：currentBalance = initialBalance + totalIncome - totalExpense（实时计算，不做缓存）</li>
 * </ul>
 *
 * <p>性能优化：批量查询消除 N+1 问题——TransactionMapper.selectAccountIncomeBatch / selectAccountExpenseBatch
 * 分别用一条 SQL GROUP BY account_id 批量查出所有账户的收入/支出合计，替代逐账户查询。</p>
 *
 * <p>跨文件引用：被 AccountController.getBalance() → AccountServiceImpl.getBalance() 使用，
 * 前端 AccountPage.vue 账户余额卡片 + DashboardPage.vue 总资产数值消费此 DTO。</p>
 */
@Data // Lombok: 自动生成 getter/setter/toString/equals/hashCode
public class AccountBalanceDTO {

  /** 账户 ID（对应 account 表 id 列，作为 GROUP BY 聚合维度的键） */
  private Long accountId;

  /** 账户名称（对应 account 表 name 列，如「招商银行卡」，前端余额卡片标题） */
  private String accountName;

  /** 账户类型：1=现金 2=银行卡 3=支付宝 4=微信（对应 account 表 type 列，前端按类型显示对应图标/颜色） */
  private Integer accountType;

  /** 初始余额（对应 account 表 initial_balance 列，DECIMAL(12,2)，创建账户时设置的金额） */
  private BigDecimal initialBalance;

  /** 该账户下所有 type=1（收入）记录的金额合计（来源 transaction 表 SUM(amount) 聚合，TransactionMapper.selectAccountIncomeBatch） */
  private BigDecimal totalIncome;

  /** 该账户下所有 type=2（支出）记录的金额合计（来源 transaction 表 SUM(amount) 聚合，TransactionMapper.selectAccountExpenseBatch） */
  private BigDecimal totalExpense;

  /** 当前余额（计算公式：initialBalance + totalIncome - totalExpense，实时计算不做缓存，保证数据一致性） */
  private BigDecimal currentBalance;

  /** 币种代码（对应 account 表 currency 列，如 CNY/USD/EUR，P2-4 多币种支持功能的基础字段） */
  private String currency;

  /** CNY 等值余额（非 CNY 账户按固定汇率换算后的等值人民币金额，CNY 账户 = currentBalance，P2-4 多币种换算功能） */
  private BigDecimal cnyEquivalentBalance;
}
