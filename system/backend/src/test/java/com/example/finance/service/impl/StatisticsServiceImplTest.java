package com.example.finance.service.impl;

import com.example.finance.entity.dto.CategorySummaryDTO;
import com.example.finance.entity.dto.MonthlySummaryDTO;
import com.example.finance.entity.dto.MonthlyTrendDTO;
import com.example.finance.mapper.TransactionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceImplTest {

  @Mock
  private TransactionMapper transactionMapper;

  @InjectMocks
  private StatisticsServiceImpl statisticsService;

  @Test
  @DisplayName("月度汇总 - 无数据时返回零值而非null")
  void monthlySummary_nullReturnsZero() {
    when(transactionMapper.selectMonthlySummary(1L, "2026-05-01 00:00:00", "2026-06-01 00:00:00")).thenReturn(null);

    MonthlySummaryDTO result = statisticsService.getMonthlySummary(1L, 2026, 5);
    assertNotNull(result);
    assertEquals(BigDecimal.ZERO, result.getTotalIncome());
    assertEquals(BigDecimal.ZERO, result.getTotalExpense());
    assertEquals(BigDecimal.ZERO, result.getBalance());
  }

  @Test
  @DisplayName("月度汇总 - 正常数据透传")
  void monthlySummary_withData() {
    MonthlySummaryDTO dto = new MonthlySummaryDTO();
    dto.setYear(2026);
    dto.setMonth(5);
    dto.setTotalIncome(new BigDecimal("8000.00"));
    dto.setTotalExpense(new BigDecimal("2670.00"));
    dto.setBalance(new BigDecimal("5330.00"));
    when(transactionMapper.selectMonthlySummary(1L, "2026-05-01 00:00:00", "2026-06-01 00:00:00")).thenReturn(dto);

    MonthlySummaryDTO result = statisticsService.getMonthlySummary(1L, 2026, 5);
    assertEquals(new BigDecimal("8000.00"), result.getTotalIncome());
    assertEquals(5330.00, result.getBalance().doubleValue(), 0.01);
    assertEquals(2026, result.getYear());
    assertEquals(5, result.getMonth());
  }

  @Test
  @DisplayName("年度汇总 - 无数据时返回零值")
  void yearlySummary_nullReturnsZero() {
    when(transactionMapper.selectYearlySummary(1L, "2026-01-01 00:00:00", "2027-01-01 00:00:00")).thenReturn(null);

    MonthlySummaryDTO result = statisticsService.getYearlySummary(1L, 2026);
    assertNotNull(result);
    assertEquals(BigDecimal.ZERO, result.getBalance());
    assertEquals(2026, result.getYear());
  }

  @Test
  @DisplayName("分类汇总 - 带类型筛选正常透传")
  void categorySummary_withTypeFilter() {
    CategorySummaryDTO dto = new CategorySummaryDTO();
    dto.setCategoryId(3L);
    dto.setCategoryName("餐饮");
    dto.setType(2);
    dto.setTotalAmount(new BigDecimal("500.00"));
    dto.setTransactionCount(10L);
    when(transactionMapper.selectCategorySummary(1L, "2026-05-01 00:00:00", "2026-06-01 00:00:00", 2)).thenReturn(List.of(dto));

    List<CategorySummaryDTO> result = statisticsService.getCategorySummary(1L, 2026, 5, 2);
    assertEquals(1, result.size());
    assertEquals("餐饮", result.get(0).getCategoryName());
    assertEquals(new BigDecimal("500.00"), result.get(0).getTotalAmount());
    assertEquals(10L, result.get(0).getTransactionCount());
  }

  @Test
  @DisplayName("分类汇总 - 空数据返回空列表")
  void categorySummary_emptyReturnsEmptyList() {
    when(transactionMapper.selectCategorySummary(1L, "2026-05-01 00:00:00", "2026-06-01 00:00:00", null)).thenReturn(List.of());

    List<CategorySummaryDTO> result = statisticsService.getCategorySummary(1L, 2026, 5, null);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("趋势查询 - 正常数据透传")
  void trend_withData() {
    MonthlyTrendDTO dto = new MonthlyTrendDTO();
    dto.setMonth("2026-05");
    dto.setTotalIncome(new BigDecimal("8000.00"));
    dto.setTotalExpense(new BigDecimal("3000.00"));
    when(transactionMapper.selectTrend(1L, "2026-01-01 00:00:00", "2027-01-01 00:00:00")).thenReturn(List.of(dto));

    List<MonthlyTrendDTO> result = statisticsService.getTrend(1L, 2026);
    assertEquals(1, result.size());
    assertEquals("2026-05", result.get(0).getMonth());
    assertEquals(new BigDecimal("8000.00"), result.get(0).getTotalIncome());
    assertEquals(new BigDecimal("3000.00"), result.get(0).getTotalExpense());
  }

  @Test
  @DisplayName("趋势查询 - 空数据返回空列表")
  void trend_emptyReturnsEmptyList() {
    when(transactionMapper.selectTrend(1L, "2026-01-01 00:00:00", "2027-01-01 00:00:00")).thenReturn(List.of());

    List<MonthlyTrendDTO> result = statisticsService.getTrend(1L, 2026);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("月度汇总 - 12月边界正确跨越到次年1月")
  void monthlySummary_decemberBoundary() {
    when(transactionMapper.selectMonthlySummary(1L, "2026-12-01 00:00:00", "2027-01-01 00:00:00")).thenReturn(null);

    MonthlySummaryDTO result = statisticsService.getMonthlySummary(1L, 2026, 12);
    assertNotNull(result);
    assertEquals(2026, result.getYear());
    assertEquals(12, result.getMonth());
  }

  @Test
  @DisplayName("参数校验 - 年份超出范围抛异常")
  void validateYearMonth_outOfRange() {
    assertThrows(com.example.finance.common.BusinessException.class,
        () -> statisticsService.getMonthlySummary(1L, 1999, 5));
    assertThrows(com.example.finance.common.BusinessException.class,
        () -> statisticsService.getMonthlySummary(1L, 2101, 5));
  }
}