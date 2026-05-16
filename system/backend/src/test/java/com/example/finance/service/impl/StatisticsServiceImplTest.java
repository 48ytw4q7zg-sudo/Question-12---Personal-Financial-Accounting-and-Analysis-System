package com.example.finance.service.impl;

import com.example.finance.entity.dto.MonthlySummaryDTO;
import com.example.finance.mapper.TransactionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

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
    when(transactionMapper.selectMonthlySummary(1L, 2026, 5)).thenReturn(null);

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
    when(transactionMapper.selectMonthlySummary(1L, 2026, 5)).thenReturn(dto);

    MonthlySummaryDTO result = statisticsService.getMonthlySummary(1L, 2026, 5);
    assertEquals(new BigDecimal("8000.00"), result.getTotalIncome());
    assertEquals(5330.00, result.getBalance().doubleValue(), 0.01);
  }

  @Test
  @DisplayName("年度汇总 - 无数据时返回零值")
  void yearlySummary_nullReturnsZero() {
    when(transactionMapper.selectYearlySummary(1L, 2026)).thenReturn(null);

    MonthlySummaryDTO result = statisticsService.getYearlySummary(1L, 2026);
    assertNotNull(result);
    assertEquals(BigDecimal.ZERO, result.getBalance());
  }
}
