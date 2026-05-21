package com.example.finance.controller;

import com.example.finance.common.BusinessException;
import com.example.finance.common.ErrorCode;
import com.example.finance.common.Result;
import com.example.finance.entity.dto.CategorySummaryDTO;
import com.example.finance.entity.dto.MonthlySummaryDTO;
import com.example.finance.entity.dto.MonthlyTrendDTO;
import com.example.finance.interceptor.LoginInterceptor;
import com.example.finance.service.StatisticsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 统计控制器（PRD P1-2 Dashboard 仪表板 + P1-6 月度/年度汇总报表 + P2-1 ECharts 图表）
 *
 * 职责：接收统计分析的 HTTP 请求，转发 StatisticsService 处理
 * 路由前缀：/api/statistics
 * 依赖：→ StatisticsService（业务逻辑层）→ TransactionMapper.xml 的统计 SQL
 *
 * 接口清单：
 *   GET /api/statistics/monthly         — 月度收支汇总（总收入/总支出/结余）
 *   GET /api/statistics/yearly          — 年度收支汇总
 *   GET /api/statistics/category-summary — 分类汇总（按分类统计金额和笔数）
 *   GET /api/statistics/trend           — 月度趋势（12 个月收支折线图数据）
 *
 * 被前端调用：→ api/statistics.js 的 getMonthlySummary/getYearlySummary/getCategorySummary/getTrend
 * 被 DashboardPage.vue（P1-2 月度卡片 + 饼图 + 趋势图）和 AnalyticsPage.vue（P2-1 ECharts 图表）调用
 *
 * SQL 实现：均在 TransactionMapper.xml 中，排除转账记录（transfer_id IS NULL）避免虚增
 */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

  /** → StatisticsService：处理各类统计汇总的业务逻辑 */
  private final StatisticsService statisticsService;

  /** 年份有效范围 */
  private static final int YEAR_MIN = 2000;
  private static final int YEAR_MAX = 2100;

  /**
   * 校验年份和月份参数范围（提取公共校验逻辑，避免4处重复）
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
   * 月度收支汇总接口（Dashboard 月度卡片数据源）
   *
   * 流程：→ TransactionMapper.xml selectMonthlySummary
   *     → SUM(CASE WHEN type=1 THEN amount) AS totalIncome
   *     → SUM(CASE WHEN type=2 THEN amount) AS totalExpense
   *     → balance = totalIncome - totalExpense
   *     → 排除 transfer_id 非空的转账记录
   *
   * @param year    年份（必填，如 2026）
   * @param month   月份（必填，如 5）
   * @param request HTTP 请求
   * @return Result<MonthlySummaryDTO> 月度汇总（year/month/totalIncome/totalExpense/balance）
   *
   * 被前端 DashboardPage.vue 顶部三个摘要卡片（月收入/月支出/月结余）调用
   */
  @GetMapping("/monthly")
  public Result<MonthlySummaryDTO> getMonthlySummary(
      @RequestParam int year, @RequestParam int month,
      HttpServletRequest request) {
    validateYearMonth(year, month);
    Long userId = LoginInterceptor.getUserId(request);
    // → StatisticsService.getMonthlySummary() → TransactionMapper.selectMonthlySummary()
    MonthlySummaryDTO summary = statisticsService.getMonthlySummary(userId, year, month);
    return Result.success(summary);
  }

  /**
   * 年度收支汇总接口
   *
   * 流程：→ TransactionMapper.xml selectYearlySummary（全年汇总，不含月维度）
   *     → 排除转账记录
   *
   * @param year    年份（必填）
   * @param request HTTP 请求
   * @return Result<MonthlySummaryDTO> 年度汇总（month 字段为 null）
   *
   * 被前端 AnalyticsPage.vue 年度汇总卡片调用
   */
  @GetMapping("/yearly")
  public Result<MonthlySummaryDTO> getYearlySummary(@RequestParam int year,
      HttpServletRequest request) {
    validateYearMonth(year, null);
    Long userId = LoginInterceptor.getUserId(request);
    // → StatisticsService.getYearlySummary() → TransactionMapper.selectYearlySummary()
    MonthlySummaryDTO summary = statisticsService.getYearlySummary(userId, year);
    return Result.success(summary);
  }

  /**
   * 分类汇总接口（饼图数据源）
   *
   * 流程：→ TransactionMapper.xml selectCategorySummary
   *     → 按 category_id 分组 → SUM(amount) + COUNT(*) → 按金额降序
   *     → 可按 type 筛选（1=收入 / 2=支出 / null=全部）
   *
   * @param year    年份（必填）
   * @param month   月份（必填）
   * @param type    收支类型筛选（可选：1=收入 2=支出 null=全部）
   * @param request HTTP 请求
   * @return Result<List<CategorySummaryDTO>> 分类汇总列表（含 categoryId/categoryName/totalAmount/count）
   *
   * 被前端 DashboardPage.vue 支出分类饼图 + AnalyticsPage.vue 分类饼图调用
   */
  @GetMapping("/category-summary")
  public Result<List<CategorySummaryDTO>> getCategorySummary(
      @RequestParam int year, @RequestParam int month,
      @RequestParam(required = false) Integer type,
      HttpServletRequest request) {
    validateYearMonth(year, month);
    Long userId = LoginInterceptor.getUserId(request);
    // → StatisticsService.getCategorySummary() → TransactionMapper.selectCategorySummary()
    List<CategorySummaryDTO> list = statisticsService.getCategorySummary(userId, year, month, type);
    return Result.success(list);
  }

  /**
   * 月度趋势接口（折线图数据源）
   *
   * 流程：→ TransactionMapper.xml selectTrend
   *     → 按 DATE_FORMAT(time, '%Y-%m') 分组 → 每月收入/支出
   *     → 排除转账记录 → 按月份升序
   *
   * @param year    年份（必填）
   * @param request HTTP 请求
   * @return Result<List<MonthlyTrendDTO>> 月度趋势列表（含 month/totalIncome/totalExpense）
   *
   * 被前端 DashboardPage.vue 近 12 月趋势折线图 + AnalyticsPage.vue 趋势折线图调用
   */
  @GetMapping("/trend")
  public Result<List<MonthlyTrendDTO>> getTrend(@RequestParam int year,
      HttpServletRequest request) {
    validateYearMonth(year, null);
    Long userId = LoginInterceptor.getUserId(request);
    // → StatisticsService.getTrend() → TransactionMapper.selectTrend()
    List<MonthlyTrendDTO> list = statisticsService.getTrend(userId, year);
    return Result.success(list);
  }
}
