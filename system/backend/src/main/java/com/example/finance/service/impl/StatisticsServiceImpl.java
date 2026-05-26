package com.example.finance.service.impl;

import com.example.finance.common.BusinessException;
import com.example.finance.common.EntityValidator;
import com.example.finance.common.ErrorCode;
import com.example.finance.entity.dto.CategorySummaryDTO;
import com.example.finance.entity.dto.MonthlySummaryDTO;
import com.example.finance.entity.dto.MonthlyTrendDTO;
import com.example.finance.mapper.TransactionMapper;
import com.example.finance.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 统计服务实现
 *
 * <p>对应 PRD 功能:</p>
 * <ul>
 *   <li>P0-5 按账户汇总余额(由AccountServiceImpl实现, 本类不涉及)</li>
 *   <li>P0-6 分类浏览页: getCategorySummary() 各分类本月消费金额</li>
 *   <li>P1-2 月度/年度汇总: getMonthlySummary() / getYearlySummary()</li>
 *   <li>P1-6 ECharts基础图表: getCategorySummary() 饼图数据源</li>
 *   <li>P2-1 多图联动: getTrend() 12个月收支趋势折线图数据源</li>
 * </ul>
 *
 * <p>实现说明: 本类为薄Service层, 所有统计查询委托TransactionMapper的XML SQL执行聚合计算。
 * 无数据时返回零值(收入=0/支出=0/结余=0), 不返回null。</p>
 *
 * <p>调用方: StatisticsController (controller/StatisticsController.java)</p>
 */
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

  /** 年份有效范围（2000=业务起始年，2100=合理上界，防止恶意大年份查询导致性能问题） */
  private static final int YEAR_MIN = 2000;
  private static final int YEAR_MAX = 2100;

  /** → TransactionMapper：所有统计聚合查询委托 TransactionMapper XML SQL 执行 */
  private final TransactionMapper transactionMapper;

  /**
   * 校验年份和月份参数范围
   * @param year 年份
   * @param month 月份（null 表示不需要校验月份，如年度/趋势接口）
   */
  private void validateYearMonth(int year, Integer month) {  // 校验年月参数范围
    if (year < YEAR_MIN || year > YEAR_MAX) {  // 年份超出有效范围
      throw new BusinessException(ErrorCode.YEAR_OUT_OF_RANGE.getCode(), String.format(ErrorCode.YEAR_OUT_OF_RANGE.getMsg(), YEAR_MIN, YEAR_MAX));  // 抛出参数非法异常
    }
    if (month != null && (month < 1 || month > 12)) {  // 月份超出1-12范围
      throw new BusinessException(ErrorCode.MONTH_OUT_OF_RANGE.getCode(), ErrorCode.MONTH_OUT_OF_RANGE.getMsg());  // 抛出参数非法异常
    }
  }

  /**
   * 月度收支汇总
   *
   * <p>对应 PRD P1-2 GET /api/statistics/monthly。</p>
   * <p>统计指定月份的收入总额/支出总额/结余(收入-支出)。</p>
   * <p>无数据时返回零值, 不返回null。</p>
   * <p>性能优化: 使用范围查询(startOfMonth &lt; time &lt; startOfNextMonth)替代 YEAR()/MONTH() 函数，利用 idx_transaction_user_time 索引。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param year 年份(如2026)
   * @param month 月份(1-12)
   * @return 月度汇总DTO(含totalIncome/totalExpense/balance)
   *
   * 调用链路: StatisticsController.getMonthlySummary() → StatisticsService.getMonthlySummary() → TransactionMapper.selectMonthlySummary()
   * SQL 内容: SELECT SUM(income), SUM(expense) FROM transaction WHERE userId=? AND time BETWEEN ? AND ? (排除转账记录)
   */
  @Override
  @Transactional(readOnly = true)                                       // 只读事务：MySQL 跳过锁和 undo log 开销
  public MonthlySummaryDTO getMonthlySummary(Long userId, int year, int month) {  // 月度收支汇总
    // 【步骤①】校验年月参数范围（业务锁: 2000-2100年 / 1-12月，防止恶意大年份查询）
    validateYearMonth(year, month);                                    // → this.validateYearMonth()（private 方法 · 第51行）
    // 【步骤②】构建月份范围时间字符串（复用 EntityValidator 公共方法 · common/EntityValidator.java）
    String[] range = EntityValidator.buildMonthRange(year, month);     // → EntityValidator.buildMonthRange() 返回 [startOfMonth, startOfNextMonth]
    // 【步骤③】查询月度收支汇总（→ mapper/TransactionMapper.java 的 selectMonthlySummary · XML 映射）
    MonthlySummaryDTO summary = transactionMapper.selectMonthlySummary(userId, range[0], range[1]);  // SQL: SUM(type=1 收入) + SUM(type=2 支出) WHERE userId=? AND time BETWEEN ? AND ?
    // 【步骤④】空值填充：聚合查询无匹配行时返回各字段为 BigDecimal.ZERO 的 DTO（防止前端 null 处理）
    summary = zeroFillIfNull(summary, year, month);                    // → this.zeroFillIfNull()（private 方法 · 第111行）
    // 【步骤⑤】手动设置年份/月份（SQL 使用范围查询而非传 year/month 参数）
    summary.setYear(year);                                             // 设置年份
    summary.setMonth(month);                                           // 设置月份
    return summary;                                                    // 返回月度汇总 DTO → StatisticsController → Result.success → 前端 api/statistics.js getMonthlySummary()
  }

  /**
   * 年度收支汇总
   *
   * <p>对应 PRD P1-2 GET /api/statistics/yearly。</p>
   * <p>统计指定年份的收入总额/支出总额/结余。</p>
   * <p>返回类型说明: 复用 MonthlySummaryDTO（month 字段为 null，表示年度汇总不含月维度）。</p>
   * <p>性能优化: 使用范围查询(startOfYear &lt; time &lt; startOfNextYear)替代 YEAR() 函数，利用 idx_transaction_user_time 索引。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param year 年份(如2026)
   * @return 年度汇总DTO（MonthlySummaryDTO 类型，month 字段为 null）
   *
   * 调用链路: StatisticsController.getYearlySummary() → StatisticsService.getYearlySummary() → TransactionMapper.selectYearlySummary()
   * SQL 内容: SELECT SUM(income), SUM(expense) FROM transaction WHERE userId=? AND time BETWEEN ? AND ? (年度边界)
   */
  @Override
  @Transactional(readOnly = true)                                       // 只读事务
  public MonthlySummaryDTO getYearlySummary(Long userId, int year) {  // 年度收支汇总
    // 【步骤①】校验年份参数范围（月份传 null，不校验月份）
    validateYearMonth(year, null);                                    // → this.validateYearMonth()（private 方法 · 第51行 · month=null 时跳过月份校验）
    // 【步骤②】构建年份范围时间字符串（复用 EntityValidator 公共方法 · common/EntityValidator.java）
    String[] range = EntityValidator.buildYearRange(year);            // → EntityValidator.buildYearRange() 返回 [startOfYear, startOfNextYear]
    // 【步骤③】查询年度收支汇总（→ mapper/TransactionMapper.java 的 selectYearlySummary · XML 映射 · SQL 与月度类似但时间范围为整年）
    MonthlySummaryDTO summary = transactionMapper.selectYearlySummary(userId, range[0], range[1]);  // 年度聚合查询
    // 【步骤④】空值填充（无数据时返回零值，month 字段保持 null 表示年度汇总不含月维度）
    summary = zeroFillIfNull(summary, year, null);                    // → this.zeroFillIfNull()（private 方法 · 第111行 · month=null）
    summary.setYear(year);                                             // 设置年份
    return summary;                                                    // 返回年度汇总 DTO → StatisticsController → Result.success → 前端 api/statistics.js getYearlySummary()
  }


  /** 空值填充：聚合查询无匹配行时返回零值 DTO，避免前端 null 处理
   *  <p>SQL 聚合函数 SUM 在没有匹配行时返回 NULL（而非 0），Mapper 返回 null 对象。
   *  本方法创建各字段为 BigDecimal.ZERO 的 DTO 兜底，使前端无需判空。</p>
   *  <p>调用方: getMonthlySummary() / getYearlySummary()（同 Service 内方法 · StatisticsServiceImpl.java）</p> */
  private MonthlySummaryDTO zeroFillIfNull(MonthlySummaryDTO summary, int year, Integer month) {  // 无数据时填充零值
    if (summary == null) {                                           // Mapper 返回 null（SQL 聚合无匹配行）
      summary = new MonthlySummaryDTO();                             // 创建空 DTO 实例
      summary.setYear(year);                                         // 设置年份
      summary.setMonth(month);                                       // 设置月份（年度汇总时为 null）
      summary.setTotalIncome(BigDecimal.ZERO);                       // 总收入设为 0（避免前端 NPE）
      summary.setTotalExpense(BigDecimal.ZERO);                      // 总支出设为 0
      summary.setBalance(BigDecimal.ZERO);                           // 结余设为 0（收入-支出=0）
    }
    return summary;                                                  // 返回汇总（非空则原样返回，为空则返回全零值 DTO）
  }

  /**
   * 按分类汇总收支（饼图数据源 + 分类页本月消费金额）
   *
   * <p>对应 PRD P1-6(ECharts饼图) + P0-6(分类浏览页本月消费)。</p>
   * <p>type参数: 1=收入汇总, 2=支出汇总, null=全部。</p>
   * <p>性能优化: 使用范围查询替代 YEAR()/MONTH()，利用 idx_transaction_user_time 索引。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param year 年份
   * @param month 月份(1-12)
   * @param type 交易类型(1=收入/2=支出/null=全部)
   * @return 各分类汇总列表(含categoryId/categoryName/totalAmount/transactionCount), 无数据时返回空列表
   *
   * 调用链路: StatisticsController.getCategorySummary() → StatisticsService.getCategorySummary() → TransactionMapper.selectCategorySummary()
   * SQL 内容: SELECT category_id, category_name, SUM(amount), COUNT(*) FROM transaction JOIN category WHERE userId=? AND time BETWEEN ? AND ? GROUP BY category_id
   */
  @Override
  @Transactional(readOnly = true)                                       // 只读事务
  public List<CategorySummaryDTO> getCategorySummary(Long userId, int year, int month, Integer type) {  // 分类汇总收支
    // 【步骤①】校验年月参数范围
    validateYearMonth(year, month);                                    // → this.validateYearMonth()（private 方法 · 第51行）
    // 【步骤②】构建月份范围时间字符串（复用 EntityValidator 公共方法 · common/EntityValidator.java）
    String[] range = EntityValidator.buildMonthRange(year, month);     // → EntityValidator.buildMonthRange() 返回 [startOfMonth, startOfNextMonth]
    // 【步骤③】查询分类汇总（→ mapper/TransactionMapper.java 的 selectCategorySummary · XML 映射）
    List<CategorySummaryDTO> result = transactionMapper.selectCategorySummary(userId, range[0], range[1], type);  // GROUP BY category_id 聚合 · JOIN category 填充名称
    // 【步骤④】null 保护：Mapper 返回 null 时（无匹配行）返回空列表而非 null，防止前端 NPE
    return result != null ? result : List.of();                        // null→空列表 → StatisticsController → Result.success → 前端 api/statistics.js getCategorySummary() → DashboardPage.vue / AnalyticsPage.vue 饼图
  }

  /**
   * 月度收支趋势（12个月折线图数据源）
   *
   * <p>对应 PRD P2-1 GET /api/statistics/trend。</p>
   * <p>返回指定年份12个月的收入/支出数据, 用于ECharts折线图。</p>
   * <p>无数据月份返回totalIncome=0/totalExpense=0（由 XML SQL 的 COALESCE 处理）。</p>
   * <p>性能优化: 使用范围查询替代 YEAR()，利用 idx_transaction_user_time 索引。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param year 年份(如2026)
   * @return 12个月趋势数据列表(含month/totalIncome/totalExpense), 无数据时返回空列表
   *
   * 调用链路: StatisticsController.getTrend() → StatisticsService.getTrend() → TransactionMapper.selectTrend()
   * SQL 内容: SELECT MONTH(time) as month, SUM(income), SUM(expense) FROM transaction WHERE userId=? AND time BETWEEN ? AND ? GROUP BY MONTH
   */
  @Override
  @Transactional(readOnly = true)                                       // 只读事务
  public List<MonthlyTrendDTO> getTrend(Long userId, int year) {  // 月度收支趋势
    // 【步骤①】校验年份参数范围（月份传 null，不校验月份）
    validateYearMonth(year, null);                                    // → this.validateYearMonth()（private 方法 · 第51行 · month=null 时跳过月份校验）
    // 【步骤②】构建年份范围时间字符串（复用 EntityValidator 公共方法 · common/EntityValidator.java）
    String[] range = EntityValidator.buildYearRange(year);            // → EntityValidator.buildYearRange() 返回 [startOfYear, startOfNextYear]
    // 【步骤③】查询 12 个月趋势数据（→ mapper/TransactionMapper.java 的 selectTrend · XML 映射）
    List<MonthlyTrendDTO> result = transactionMapper.selectTrend(userId, range[0], range[1]);  // GROUP BY MONTH(time) · 按月聚合收入/支出
    // 【步骤④】null 保护：Mapper 返回 null 时（无匹配行）返回空列表而非 null，防止前端 NPE
    return result != null ? result : List.of();                        // null→空列表 → StatisticsController → Result.success → 前端 api/statistics.js getTrend() → DashboardPage.vue / AnalyticsPage.vue 折线图
  }
}
