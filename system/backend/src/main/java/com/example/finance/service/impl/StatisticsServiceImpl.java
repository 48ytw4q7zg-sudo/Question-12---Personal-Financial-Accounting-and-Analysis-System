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
   */
  @Override
  @Transactional(readOnly = true)
  public MonthlySummaryDTO getMonthlySummary(Long userId, int year, int month) {  // 月度收支汇总
    validateYearMonth(year, month);  // 校验年月参数
    String[] range = EntityValidator.buildMonthRange(year, month);  // 构建月份范围时间字符串（复用EntityValidator公共方法）
    MonthlySummaryDTO summary = transactionMapper.selectMonthlySummary(userId, range[0], range[1]);  // 查询月度汇总
    summary = zeroFillIfNull(summary, year, month);  // 空值填充(无数据时返回零值)
    // 范围查询不传 year/month 到 SQL，需手动设置
    summary.setYear(year);  // 设置年份
    summary.setMonth(month);  // 设置月份
    return summary;  // 返回月度汇总
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
   */
  @Override
  @Transactional(readOnly = true)
  public MonthlySummaryDTO getYearlySummary(Long userId, int year) {  // 年度收支汇总
    validateYearMonth(year, null);  // 校验年份(月份不校验)
    String[] range = EntityValidator.buildYearRange(year);  // 构建年份范围时间字符串（复用EntityValidator公共方法）
    MonthlySummaryDTO summary = transactionMapper.selectYearlySummary(userId, range[0], range[1]);  // 查询年度汇总
    summary = zeroFillIfNull(summary, year, null);  // 空值填充
    summary.setYear(year);  // 设置年份
    return summary;  // 返回年度汇总
  }


  /** 空值填充：聚合查询无匹配行时返回零值 DTO，避免前端 null 处理 */
  private MonthlySummaryDTO zeroFillIfNull(MonthlySummaryDTO summary, int year, Integer month) {  // 无数据时填充零值
    if (summary == null) {  // 查询结果为空
      summary = new MonthlySummaryDTO();  // 创建空DTO
      summary.setYear(year);  // 设置年份
      summary.setMonth(month);  // 设置月份(年度汇总时为null)
      summary.setTotalIncome(BigDecimal.ZERO);  // 收入设为0
      summary.setTotalExpense(BigDecimal.ZERO);  // 支出设为0
      summary.setBalance(BigDecimal.ZERO);  // 结余设为0
    }
    return summary;  // 返回汇总(非空则原样返回)
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
   */
  @Override
  @Transactional(readOnly = true)
  public List<CategorySummaryDTO> getCategorySummary(Long userId, int year, int month, Integer type) {  // 分类汇总收支
    validateYearMonth(year, month);  // 校验年月参数
    String[] range = EntityValidator.buildMonthRange(year, month);  // 构建月份范围时间字符串（复用EntityValidator公共方法）
    List<CategorySummaryDTO> result = transactionMapper.selectCategorySummary(userId, range[0], range[1], type);  // 查询分类汇总
    // null保护: Mapper返回null时(无数据), 返回空列表而非null, 防止前端NPE
    return result != null ? result : List.of();  // null保护返回空列表
  }

  /**
   * 月度收支趋势（12个月折线图数据源）
   *
   * <p>对应 PRD P2-1 GET /api/statistics/trend。</p>
   * <p>返回指定年份12个月的收入/支出数据, 用于ECharts折线图。</p>
   * <p>无数据月份返回totalIncome=0/totalExpense=0。</p>
   * <p>性能优化: 使用范围查询替代 YEAR()，利用 idx_transaction_user_time 索引。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param year 年份(如2026)
   * @return 12个月趋势数据列表(含month/totalIncome/totalExpense), 无数据时返回空列表
   */
  @Override
  @Transactional(readOnly = true)
  public List<MonthlyTrendDTO> getTrend(Long userId, int year) {  // 月度收支趋势
    validateYearMonth(year, null);  // 校验年份(月份不校验)
    String[] range = EntityValidator.buildYearRange(year);  // 构建年份范围时间字符串（复用EntityValidator公共方法）
    List<MonthlyTrendDTO> result = transactionMapper.selectTrend(userId, range[0], range[1]);  // 查询12个月趋势
    // null保护: Mapper返回null时(无数据), 返回空列表而非null, 防止前端NPE
    return result != null ? result : List.of();  // null保护返回空列表
  }
}
