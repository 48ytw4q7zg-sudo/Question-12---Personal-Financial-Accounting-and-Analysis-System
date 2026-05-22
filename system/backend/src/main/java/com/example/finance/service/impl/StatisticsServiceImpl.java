package com.example.finance.service.impl;

import com.example.finance.common.BusinessException;
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

  /** 年份有效范围 */
  private static final int YEAR_MIN = 2000;
  private static final int YEAR_MAX = 2100;

  /** → TransactionMapper：所有统计聚合查询委托 TransactionMapper XML SQL 执行 */
  private final TransactionMapper transactionMapper;

  /**
   * 校验年份和月份参数范围
   * @param year 年份
   * @param month 月份（null 表示不需要校验月份，如年度/趋势接口）
   */
  private void validateYearMonth(int year, Integer month) {
    if (year < YEAR_MIN || year > YEAR_MAX) {
      throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "year需在" + YEAR_MIN + "-" + YEAR_MAX + "之间");
    }
    if (month != null && (month < 1 || month > 12)) {
      throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "month需在1-12之间");
    }
  }

  /**
   * 月度收支汇总
   *
   * <p>对应 PRD P1-2 GET /api/statistics/monthly。</p>
   * <p>统计指定月份的收入总额/支出总额/结余(收入-支出)。</p>
   * <p>无数据时返回零值, 不返回null。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param year 年份(如2026)
   * @param month 月份(1-12)
   * @return 月度汇总DTO(含totalIncome/totalExpense/balance)
   */
  @Override
  @Transactional(readOnly = true)
  public MonthlySummaryDTO getMonthlySummary(Long userId, int year, int month) {
    validateYearMonth(year, month);
    MonthlySummaryDTO summary = transactionMapper.selectMonthlySummary(userId, year, month);
    return zeroFillIfNull(summary, year, month);
  }

  /**
   * 年度收支汇总
   *
   * <p>对应 PRD P1-2 GET /api/statistics/yearly。</p>
   * <p>统计指定年份的收入总额/支出总额/结余。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param year 年份(如2026)
   * @return 年度汇总DTO
   */
  @Override
  @Transactional(readOnly = true)
  public MonthlySummaryDTO getYearlySummary(Long userId, int year) {
    validateYearMonth(year, null);
    MonthlySummaryDTO summary = transactionMapper.selectYearlySummary(userId, year);
    return zeroFillIfNull(summary, year, null);
  }

  /** 空值填充：聚合查询无匹配行时返回零值 DTO，避免前端 null 处理 */
  private MonthlySummaryDTO zeroFillIfNull(MonthlySummaryDTO summary, int year, Integer month) {
    if (summary == null) {
      summary = new MonthlySummaryDTO();
      summary.setYear(year);
      summary.setMonth(month);
      summary.setTotalIncome(BigDecimal.ZERO);
      summary.setTotalExpense(BigDecimal.ZERO);
      summary.setBalance(BigDecimal.ZERO);
    }
    return summary;
  }

  /**
   * 按分类汇总收支（饼图数据源 + 分类页本月消费金额）
   *
   * <p>对应 PRD P1-6(ECharts饼图) + P0-6(分类浏览页本月消费)。</p>
   * <p>type参数: 1=收入汇总, 2=支出汇总, null=全部。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param year 年份
   * @param month 月份(1-12)
   * @param type 交易类型(1=收入/2=支出/null=全部)
   * @return 各分类汇总列表(含categoryId/categoryName/totalAmount/transactionCount), 无数据时返回空列表
   */
  @Override
  @Transactional(readOnly = true)
  public List<CategorySummaryDTO> getCategorySummary(Long userId, int year, int month, Integer type) {
    validateYearMonth(year, month);
    List<CategorySummaryDTO> result = transactionMapper.selectCategorySummary(userId, year, month, type);
    // null保护: Mapper返回null时(无数据), 返回空列表而非null, 防止前端NPE
    return result != null ? result : List.of();
  }

  /**
   * 月度收支趋势（12个月折线图数据源）
   *
   * <p>对应 PRD P2-1 GET /api/statistics/trend。</p>
   * <p>返回指定年份12个月的收入/支出数据, 用于ECharts折线图。</p>
   * <p>无数据月份返回totalIncome=0/totalExpense=0。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param year 年份(如2026)
   * @return 12个月趋势数据列表(含month/totalIncome/totalExpense), 无数据时返回空列表
   */
  @Override
  @Transactional(readOnly = true)
  public List<MonthlyTrendDTO> getTrend(Long userId, int year) {
    validateYearMonth(year, null);
    List<MonthlyTrendDTO> result = transactionMapper.selectTrend(userId, year);
    // null保护: Mapper返回null时(无数据), 返回空列表而非null, 防止前端NPE
    return result != null ? result : List.of();
  }
}
