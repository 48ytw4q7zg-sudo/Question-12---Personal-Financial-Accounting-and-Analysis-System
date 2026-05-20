package com.example.finance.scheduler;

import com.example.finance.entity.Budget;
import com.example.finance.entity.dto.CategorySummaryDTO;
import com.example.finance.mapper.BudgetMapper;
import com.example.finance.mapper.TransactionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BudgetScheduler 单元测试 (P2-5 扩展)
 */
@ExtendWith(MockitoExtension.class)
class BudgetSchedulerTest {

  @Mock
  private BudgetMapper budgetMapper;

  @Mock
  private TransactionMapper transactionMapper;

  @InjectMocks
  private BudgetScheduler budgetScheduler;

  @BeforeEach
  void setUp() {
    // No setup needed — all mocked
  }

  /**
   * 空预算列表 — 跳过检查,不报错
   */
  @Test
  void checkBudgetAlerts_emptyBudget_skipWithoutError() {
    when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

    budgetScheduler.checkBudgetAlerts();

    verify(budgetMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    verify(transactionMapper, never()).selectCategorySummary(any(), anyInt(), anyInt(), anyInt());
  }

  /**
   * 单条预算,未超阈值 — 日志无警告
   */
  @Test
  void checkBudgetAlerts_singleBudgetUnderThreshold() {
    Budget budget = new Budget();
    budget.setId(1L);
    budget.setUserId(1L);
    budget.setCategoryId(1L);
    budget.setMonth("2026-05");
    budget.setAmount(new BigDecimal("2000.00"));

    CategorySummaryDTO summary = new CategorySummaryDTO();
    summary.setCategoryId(1L);
    summary.setTotalAmount(new BigDecimal("500.00"));

    when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(budget));
    when(transactionMapper.selectCategorySummary(any(), anyInt(), anyInt(), anyInt()))
        .thenReturn(List.of(summary));

    budgetScheduler.checkBudgetAlerts();

    verify(budgetMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    // Scheduler 调用 2 次 selectCategorySummary：一次全量查询(null userId) + 一次按用户查询
    verify(transactionMapper, times(2)).selectCategorySummary(any(), anyInt(), anyInt(), anyInt());
  }

  /**
   * 预算超支 — 触发日志
   */
  @Test
  void checkBudgetAlerts_overspentBudget() {
    Budget budget = new Budget();
    budget.setId(1L);
    budget.setUserId(1L);
    budget.setCategoryId(1L);
    budget.setMonth("2026-05");
    budget.setAmount(new BigDecimal("1000.00"));

    CategorySummaryDTO summary = new CategorySummaryDTO();
    summary.setCategoryId(1L);
    summary.setTotalAmount(new BigDecimal("1200.00"));

    when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(budget));
    when(transactionMapper.selectCategorySummary(any(), anyInt(), anyInt(), anyInt()))
        .thenReturn(List.of(summary));

    budgetScheduler.checkBudgetAlerts();

    // Scheduler 调用 2 次 selectCategorySummary：一次全量查询(null userId) + 一次按用户查询
    verify(transactionMapper, times(2)).selectCategorySummary(any(), anyInt(), anyInt(), anyInt());
  }
}
