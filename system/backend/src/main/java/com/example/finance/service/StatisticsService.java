package com.example.finance.service;

import com.example.finance.entity.dto.CategorySummaryDTO;
import com.example.finance.entity.dto.MonthlySummaryDTO;
import com.example.finance.entity.dto.MonthlyTrendDTO;

import java.util.List;

/**
 * 统计服务接口
 */
public interface StatisticsService {

  /**
   * 月度收支汇总
   */
  MonthlySummaryDTO getMonthlySummary(Long userId, int year, int month);

  /**
   * 年度收支汇总
   */
  MonthlySummaryDTO getYearlySummary(Long userId, int year);

  /**
   * 分类汇总（type 可选，为空时返回全部）
   */
  List<CategorySummaryDTO> getCategorySummary(Long userId, int year, int month, Integer type);

  /**
   * 月度趋势
   */
  List<MonthlyTrendDTO> getTrend(Long userId, int year);
}
