// ============================================================
// §1.4 数据流 节点 ⑪ — Mapper 数据访问层（MyBatis-Plus → JDBC → MySQL）
//
// 这个文件做什么：交易记录 Mapper 接口——继承 BaseMapper<Transaction>
//                 转账时被调用：SELECT ... FOR UPDATE 锁行 → insert() 两次
//                 复杂聚合查询（SUM/COUNT/GROUP BY）走 XML 映射文件
//
// ★ §1.4 数据流讲稿（节点 ⑪ · 直接念）：
//   "节点⑪，Service 调 Mapper。MyBatis-Plus 的 BaseMapper 生成 SQL——
//    SELECT ... FOR UPDATE 锁两行、INSERT 转出记录、INSERT 转入记录。
//    通过 JDBC 发送到 MySQL。"
//
// ▶ 数据流终点：MySQL InnoDB（节点 ⑫ — FOR UPDATE 行锁 → INSERT → COMMIT 事务提交）
// ============================================================
package com.example.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.dto.AccountBatchExpenseDTO;
import com.example.finance.entity.dto.AccountBatchIncomeDTO;
import com.example.finance.entity.dto.CategorySummaryDTO;
import com.example.finance.entity.dto.MonthlySummaryDTO;
import com.example.finance.entity.dto.MonthlyTrendDTO;
import com.example.finance.entity.dto.TransactionDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.math.BigDecimal;
import java.util.List;

/**
 *  ╔══════════════════════════════════════════════════════════════════════╗
 *  ║  📋 完整数据流 — Mapper 数据访问层（终点：MyBatis-Plus → JDBC → MySQL）    ║
 *  ║                                                                      ║
 *  ║  转账时被调用的方法：                                                    ║
 *  ║    selectAccountIncome() → SELECT SUM 查总收入（余额计算用）                ║
 *  ║    selectAccountExpense() → SELECT SUM 查总支出（余额计算用）               ║
 *  ║    BaseMapper.insert() → INSERT 转出+转入两条记录                        ║
 *  ║                                                                      ║
 *  ║  数据流：TransactionServiceImpl → TransactionMapper → MySQL InnoDB       ║
 *  ╚══════════════════════════════════════════════════════════════════════╝
 *
 *  ▶ 数据流上一步：TransactionServiceImpl.java（transfer() 核心业务）
 *  ▶ 数据流终点：MySQL InnoDB（FOR UPDATE 行锁 → INSERT → COMMIT 事务提交）
 */
// 交易记录 Mapper 接口（含复杂聚合查询 · XML 映射文件: resources/mapper/TransactionMapper.xml）
// 完整数据流 — Mapper 数据访问层（终点：MyBatis-Plus → JDBC → MySQL）
//
// 继承 BaseMapper<Transaction> 提供标准 CRUD（insert/selectById/updateById/deleteById 等）。
// 本接口定义的复杂查询方法（多表 JOIN / GROUP BY 聚合 / 范围统计）使用 XML 映射实现，
// 属于 MyBatis-Plus「复杂查询有限例外」（对齐 CLAUDE.md §二·四）。
//
// 性能优化：所有统计方法使用范围查询（startOfMonth < time < startOfNextMonth）替代 YEAR()/MONTH() 函数，
// 利用 idx_transaction_user_time 索引加速查询（对齐 BudgetServiceImpl/StatisticsServiceImpl 中的范围查询模式）。
//
// 调用方:
//   - TransactionServiceImpl — CRUD + 筛选 + 转账余额校验
//   - AccountServiceImpl — 余额批量查询（selectAccountIncomeBatch/selectAccountExpenseBatch）
//   - BudgetServiceImpl — 预算进度分类支出汇总（selectCategorySummary）
//   - StatisticsServiceImpl — 月度/年度/趋势/分类汇总（selectMonthlySummary 等）
@Mapper
public interface TransactionMapper extends BaseMapper<Transaction> {

  /**
   * 带筛选条件的交易记录分页查询（JOIN account + category 表填充关联名称）
   *
   * <p>对应 PRD P0-4(分页列表) + P1-1(多条件筛选)。</p>
   * <p>XML 动态 SQL：accountId/categoryId/startTime/endTime/keyword 为可选条件，
   * 为空时不过滤该维度；sortBy 白名单校验由 Service 层处理。</p>
   *
   * @param userId     当前用户 ID（JWT 解码，强制过滤确保数据隔离）
   * @param accountId  账户 ID 筛选条件（null = 不过滤）
   * @param categoryId 分类 ID 筛选条件（null = 不过滤）
   * @param startTime  起始时间（yyyy-MM-dd HH:mm:ss，null = 不过滤）
   * @param endTime    结束时间（yyyy-MM-dd HH:mm:ss，null = 不过滤）
   * @param keyword    关键词模糊匹配备注（null = 不过滤，已由 Service 层转义 LIKE 通配符）
   * @param sortBy     排序字段（白名单: time/amount_asc/amount_desc）
   * @param rowBounds  MyBatis 物理分页偏移量（offset = (pageNum-1)*pageSize, limit = pageSize）
   * @return 交易记录 DTO 列表（含 accountName/categoryName 关联名称）
   */
  List<TransactionDTO> selectTransactionList(
      @Param("userId") Long userId,
      @Param("accountId") Long accountId,
      @Param("categoryId") Long categoryId,
      @Param("startTime") String startTime,
      @Param("endTime") String endTime,
      @Param("keyword") String keyword,
      @Param("sortBy") String sortBy,
      RowBounds rowBounds
  );

  /**
   * 查询带筛选条件的交易记录总数（用于分页 total 计算）
   *
   * <p>与 selectTransactionList 使用相同的筛选条件，但仅返回 COUNT(*) 结果。</p>
   * <p>分页模式：selectTransactionList + selectTransactionCount 组合，
   * 替代 MyBatis-Plus IPage 自动 count（因 XML 动态 ORDER BY 不兼容 Page 对象）。</p>
   *
   * @param userId     当前用户 ID
   * @param accountId  账户 ID 筛选条件（null = 不过滤）
   * @param categoryId 分类 ID 筛选条件（null = 不过滤）
   * @param startTime  起始时间（null = 不过滤）
   * @param endTime    结束时间（null = 不过滤）
   * @param keyword    关键词（null = 不过滤）
   * @return 符合条件的记录总数（null 时 Service 层兜底为 0）
   */
  Long selectTransactionCount(
      @Param("userId") Long userId,
      @Param("accountId") Long accountId,
      @Param("categoryId") Long categoryId,
      @Param("startTime") String startTime,
      @Param("endTime") String endTime,
      @Param("keyword") String keyword
  );

  /**
   * 月度收支汇总（范围查询利用 idx_transaction_user_time 索引）
   *
   * <p>对应 PRD P1-2 GET /api/statistics/monthly。</p>
   * <p>SQL: SELECT SUM(income), SUM(expense) FROM transaction WHERE userId=? AND time BETWEEN ? AND ?</p>
   * <p>范围查询替代 YEAR()/MONTH() 函数，利用 idx_transaction_user_time(user_id, time) 索引加速。</p>
   *
   * @param userId            当前用户 ID
   * @param startOfMonth      月起始时间（如 "2026-05-01 00:00:00"）
   * @param startOfNextMonth  下月起始时间（如 "2026-06-01 00:00:00"，用于 < 而非 <= 避免边界问题）
   * @return 月度汇总 DTO（totalIncome/totalExpense/balance），无数据时 Service 层 zeroFill 兜底
   */
  MonthlySummaryDTO selectMonthlySummary(
      @Param("userId") Long userId,
      @Param("startOfMonth") String startOfMonth,
      @Param("startOfNextMonth") String startOfNextMonth
  );

  /**
   * 年度收支汇总（范围查询利用 idx_transaction_user_time 索引）
   *
   * <p>对应 PRD P1-2 GET /api/statistics/yearly。</p>
   * <p>SQL 与 selectMonthlySummary 类似，时间范围扩展为整年。</p>
   *
   * @param userId            当前用户 ID
   * @param startOfYear       年起始时间（如 "2026-01-01 00:00:00"）
   * @param startOfNextYear   下年起始时间（如 "2027-01-01 00:00:00"）
   * @return 年度汇总 DTO（totalIncome/totalExpense/balance）
   */
  MonthlySummaryDTO selectYearlySummary(
      @Param("userId") Long userId,
      @Param("startOfYear") String startOfYear,
      @Param("startOfNextYear") String startOfNextYear
  );

  /**
   * 按分类汇总收支金额（饼图数据源 + 分类浏览页本月消费）
   *
   * <p>对应 PRD P1-6(ECharts 饼图) + P0-6(分类浏览页)。</p>
   * <p>SQL: SELECT category_id, SUM(amount) FROM transaction WHERE userId=? AND time BETWEEN ? AND ? GROUP BY category_id</p>
   * <p>type 参数: 1=收入汇总 / 2=支出汇总 / null=全部类型汇总。</p>
   *
   * @param userId            当前用户 ID
   * @param startOfMonth      月起始时间
   * @param startOfNextMonth  下月起始时间
   * @param type              交易类型（1=收入/2=支出/null=全部）
   * @return 各分类汇总列表（categoryId/categoryName/totalAmount/transactionCount），无数据时返回空列表
   */
  List<CategorySummaryDTO> selectCategorySummary(
      @Param("userId") Long userId,
      @Param("startOfMonth") String startOfMonth,
      @Param("startOfNextMonth") String startOfNextMonth,
      @Param("type") Integer type
  );

  /**
   * 月度收支趋势（12 个月折线图数据源）
   *
   * <p>对应 PRD P2-1 GET /api/statistics/trend。</p>
   * <p>SQL: SELECT MONTH(time) as month, SUM(income), SUM(expense) FROM transaction WHERE userId=? AND time BETWEEN ? AND ? GROUP BY MONTH</p>
   *
   * @param userId            当前用户 ID
   * @param startOfYear       年起始时间
   * @param startOfNextYear   下年起始时间
   * @return 12 个月趋势数据列表（month/totalIncome/totalExpense），无数据月份由 Service 层 zeroFill 补齐
   */
  List<MonthlyTrendDTO> selectTrend(
      @Param("userId") Long userId,
      @Param("startOfYear") String startOfYear,
      @Param("startOfNextYear") String startOfNextYear
  );

  /**
   * 计算单个账户的总收入金额（用于转账余额校验 · 范围: 该账户所有 type=1（收入）的交易）
   *
   * @param userId     当前用户 ID（确保数据隔离）
   * @param accountId  账户 ID
   * @return 收入总额（DECIMAL(12,2)），无数据时返回 null（由 Service 层兜底 BigDecimal.ZERO）
   */
  BigDecimal selectAccountIncome(@Param("userId") Long userId, @Param("accountId") Long accountId);

  /**
   * 计算单个账户的总支出金额（用于转账余额校验 · 范围: 该账户所有 type=2（支出）的交易）
   *
   * @param userId     当前用户 ID
   * @param accountId  账户 ID
   * @return 支出总额（DECIMAL(12,2)），无数据时返回 null（由 Service 层兜底 BigDecimal.ZERO）
   */
  BigDecimal selectAccountExpense(@Param("userId") Long userId, @Param("accountId") Long accountId);

  /**
   * 批量计算多账户的收入汇总（消除 N+1 · GROUP BY accountId 聚合）
   *
   * <p>AccountServiceImpl.getBalance() 使用，替代逐账户 selectAccountIncome 的 N+1 模式。</p>
   * <p>SQL: SELECT account_id, SUM(amount) as totalIncome FROM transaction WHERE userId=? AND account_id IN (?) AND type=1(收入) GROUP BY account_id</p>
   * <p>返回类型化 DTO 替代 Map<String, Object>，提供编译期类型安全。</p>
   *
   * @param userId      当前用户 ID
   * @param accountIds  账户 ID 列表（活跃账户）
   * @return 每账户的收入汇总（accountId + totalIncome）
   */
  List<AccountBatchIncomeDTO> selectAccountIncomeBatch(@Param("userId") Long userId, @Param("accountIds") List<Long> accountIds);

  /**
   * 批量计算多账户的支出汇总（消除 N+1 · GROUP BY accountId 聚合）
   *
   * <p>AccountServiceImpl.getBalance() 使用，替代逐账户 selectAccountExpense 的 N+1 模式。</p>
   * <p>SQL: SELECT account_id, SUM(amount) as totalExpense FROM transaction WHERE userId=? AND account_id IN (?) AND type=2(支出) GROUP BY account_id</p>
   * <p>返回类型化 DTO 替代 Map<String, Object>，提供编译期类型安全。</p>
   *
   * @param userId      当前用户 ID
   * @param accountIds  账户 ID 列表
   * @return 每账户的支出汇总（accountId + totalExpense）
   */
  List<AccountBatchExpenseDTO> selectAccountExpenseBatch(@Param("userId") Long userId, @Param("accountIds") List<Long> accountIds);
}