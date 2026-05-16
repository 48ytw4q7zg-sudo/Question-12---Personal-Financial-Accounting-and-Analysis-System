package com.example.finance.service.impl;

import com.example.finance.entity.dto.CategorySummaryDTO;
import com.example.finance.entity.dto.MonthlySummaryDTO;
import com.example.finance.entity.dto.MonthlyTrendDTO;
import com.example.finance.mapper.TransactionMapper;
import com.example.finance.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 统计服务实现
 */
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

  private final TransactionMapper transactionMapper;

  /**
   * 月度收支汇总
   */
  @Override
  public MonthlySummaryDTO getMonthlySummary(Long userId, int year, int month) {
    MonthlySummaryDTO summary = transactionMapper.selectMonthlySummary(userId, year, month);
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
   * 年度收支汇总
   */
  @Override
  public MonthlySummaryDTO getYearlySummary(Long userId, int year) {
    MonthlySummaryDTO summary = transactionMapper.selectYearlySummary(userId, year);
    if (summary == null) {
      summary = new MonthlySummaryDTO();
      summary.setYear(year);
      summary.setTotalIncome(BigDecimal.ZERO);
      summary.setTotalExpense(BigDecimal.ZERO);
      summary.setBalance(BigDecimal.ZERO);
    }
    return summary;
  }

  /**
   * 分类汇总（type 可选，为空时返回全部）
   */
  @Override
  public List<CategorySummaryDTO> getCategorySummary(Long userId, int year, int month, Integer type) {
    return transactionMapper.selectCategorySummary(userId, year, month, type);
  }

  /**
   * 月度趋势
   */
  @Override
  public List<MonthlyTrendDTO> getTrend(Long userId, int year) {
    return transactionMapper.selectTrend(userId, year);
  }
}
