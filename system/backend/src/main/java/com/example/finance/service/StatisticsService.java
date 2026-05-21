package com.example.finance.service;

import com.example.finance.entity.dto.CategorySummaryDTO;
import com.example.finance.entity.dto.MonthlySummaryDTO;
import com.example.finance.entity.dto.MonthlyTrendDTO;

import java.util.List;

/**
 * 统计服务接口（PRD P1-2 月度/年度汇总 + P1-6/P2-1 ECharts 图表分析）
 */
public interface StatisticsService {

  /**
   * 月度收支汇总（排除转账记录避免虚增）
   *
   * @param userId 当前用户 ID（JWT 解码获取）
   * @param year   年份（如 2026）
   * @param month  月份（1-12）
   * @return 月度汇总数据（总收入、总支出、结余）
   */
  MonthlySummaryDTO getMonthlySummary(Long userId, int year, int month);

  /**
   * 年度收支汇总（排除转账记录避免虚增）
   *
   * @param userId 当前用户 ID（JWT 解码获取）
   * @param year   年份（如 2026）
   * @return 年度汇总数据（总收入、总支出、结余，month 字段为 null）
   */
  MonthlySummaryDTO getYearlySummary(Long userId, int year);

  /**
   * 分类汇总（按 category_id 分组聚合，排除转账记录）
   *
   * @param userId 当前用户 ID（JWT 解码获取）
   * @param year   年份（如 2026）
   * @param month  月份（1-12）
   * @param type   分类类型筛选（1=支出 2=收入 null=全部）
   * @return 分类汇总列表（含分类名称、金额合计、交易笔数）
   */
  List<CategorySummaryDTO> getCategorySummary(Long userId, int year, int month, Integer type);

  /**
   * 月度收支趋势（按月份分组，最多 12 个月，排除转账记录）
   *
   * @param userId 当前用户 ID（JWT 解码获取）
   * @param year   年份（如 2026）
   * @return 月度趋势列表（含月份标签、该月总收入、该月总支出）
   */
  List<MonthlyTrendDTO> getTrend(Long userId, int year);
}
