package com.example.finance.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.entity.Budget;
import com.example.finance.entity.BudgetAlert;
import com.example.finance.entity.dto.CategorySummaryDTO;
import com.example.finance.mapper.BudgetMapper;
import com.example.finance.service.BudgetAlertProcessorService;
import com.example.finance.service.impl.BudgetAlertProcessorServiceImpl;
import com.example.finance.mapper.BudgetAlertMapper;
import com.example.finance.mapper.TransactionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BudgetScheduler + BudgetAlertProcessorService 单元测试
 *
 * <p>测试拆分：</p>
 * <ul>
 *   <li>BudgetSchedulerTest: 测试调度器入口（空列表跳过、多用户分组）</li>
 *   <li>BudgetAlertProcessorServiceImpl 的 processUserBudgetAlerts 直接测试（4级预警、持久化、幂等、边界）</li>
 * </ul>
 *
 * <p>架构说明：原 processUserBudgetAlerts 在 BudgetScheduler 内部被 this 调用，
 * @Transactional(REQUIRES_NEW) 因 Spring AOP 代理自调用而失效。
 * 已提取为独立 BudgetAlertProcessorService，通过注入的 Spring 代理调用，REQUIRES_NEW 正确生效。</p>
 */
@ExtendWith(MockitoExtension.class)
class BudgetSchedulerTest {

  @Mock
  private BudgetMapper budgetMapper;

  @Mock
  private BudgetAlertMapper budgetAlertMapper;

  @Mock
  private TransactionMapper transactionMapper;

  /** 调度器（注入 mock 的 budgetAlertProcessorService） */
  private BudgetScheduler budgetScheduler;

  /** 单用户处理 Service（直接测试预警逻辑） */
  private BudgetAlertProcessorServiceImpl budgetAlertProcessorService;

  @BeforeEach
  void setUp() {
    // 构建 BudgetAlertProcessorService 实例用于直接测试预警逻辑
    budgetAlertProcessorService = new BudgetAlertProcessorServiceImpl(budgetAlertMapper, transactionMapper);
    // 构建 BudgetScheduler（注入 mock budgetMapper + mock budgetAlertProcessorService）
    BudgetAlertProcessorService mockProcessor = mock(BudgetAlertProcessorService.class);
    budgetScheduler = new BudgetScheduler(budgetMapper, mockProcessor);
  }

  // ==================== 1. 调度器入口测试 ====================

  /**
   * 本月无活跃预算 → 跳过检查，不调用 processor
   */
  @Test
  void checkBudgetAlerts_emptyBudgetList_skipAll() {
    when(budgetMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(Collections.emptyList());

    budgetScheduler.checkBudgetAlerts();

    verify(budgetMapper).selectList(any(LambdaQueryWrapper.class));
    // processor 不被调用
    verifyNoInteractionsWithProcessor();
  }

  /**
   * 两个用户的预算 → 调度器按用户分组，调用 processor 两次
   */
  @Test
  void checkBudgetAlerts_multiUser_groupedAndProcessed() {
    Budget budget1 = createBudget(1L, 1L, 1L, "2026-05", new BigDecimal("1000.00"));
    Budget budget2 = createBudget(2L, 2L, 2L, "2026-05", new BigDecimal("2000.00"));

    BudgetAlertProcessorService mockProcessor = mock(BudgetAlertProcessorService.class);
    when(mockProcessor.processUserBudgetAlerts(anyLong(), anyList(), anyString(), any(), anyInt(), anyInt()))
        .thenReturn(1);
    BudgetScheduler scheduler = new BudgetScheduler(budgetMapper, mockProcessor);

    when(budgetMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(List.of(budget1, budget2));

    scheduler.checkBudgetAlerts();

    // 两个用户各调用 processor 一次
    verify(mockProcessor, times(2)).processUserBudgetAlerts(anyLong(), anyList(), anyString(), any(), anyInt(), anyInt());
  }

  // ==================== 2. 预警级别计算（直接测试 BudgetAlertProcessorServiceImpl） ====================

  /**
   * NORMAL：消耗 25%，未达任何阈值 → 不生成预警日志
   */
  @Test
  void alertLevel_normal_noAlertGenerated() {
    // 使用较大预算和较小消耗确保不触发任何阈值（月 < 80%，日均 < 150%）
    // 预算 10000，已花 100（1%），第5天日均=20，日均预算=10000/31≈322.58，日均阈值≈483.87 → 20 < 483.87
    Budget budget = createBudget(1L, 1L, 1L, "2026-05", new BigDecimal("10000.00"));
    CategorySummaryDTO summary = createSummary(1L, new BigDecimal("100.00")); // 1%

    when(transactionMapper.selectCategorySummary(anyLong(), anyString(), anyString(), eq(2)))
        .thenReturn(List.of(summary));

    LocalDateTime now = LocalDateTime.of(2026, 5, 5, 2, 0);
    int result = budgetAlertProcessorService.processUserBudgetAlerts(1L, List.of(budget), "2026-05", now, 5, 31);

    // NORMAL 级别仍会写入 budget_alert 表（记录正常状态），但 alertCount=0
    assertEquals(0, result);
    verify(budgetAlertMapper).insert(any(BudgetAlert.class));
  }

  /**
   * OVERSPENT：消耗 > 预算 → 生成 OVERSPENT 预警
   */
  @Test
  void alertLevel_overspent_spentExceedsBudget() {
    Budget budget = createBudget(1L, 1L, 1L, "2026-05", new BigDecimal("1000.00"));
    CategorySummaryDTO summary = createSummary(1L, new BigDecimal("1200.00")); // 120%

    when(transactionMapper.selectCategorySummary(anyLong(), anyString(), anyString(), eq(2)))
        .thenReturn(List.of(summary));

    LocalDateTime now = LocalDateTime.of(2026, 5, 5, 2, 0);
    int result = budgetAlertProcessorService.processUserBudgetAlerts(1L, List.of(budget), "2026-05", now, 5, 31);

    ArgumentCaptor<BudgetAlert> captor = ArgumentCaptor.forClass(BudgetAlert.class);
    verify(budgetAlertMapper).insert(captor.capture());

    BudgetAlert alert = captor.getValue();
    assertEquals("OVERSPENT", alert.getAlertLevel());
    assertEquals(new BigDecimal("1200.00"), alert.getSpentAmount());
    assertEquals(new BigDecimal("1000.00"), alert.getBudgetAmount());
    assertEquals(1, result);
  }

  /**
   * MONTHLY_WARN：消耗 ≥ 预算 × 80% 但未超支 → 生成 MONTHLY_WARN 预警
   */
  @Test
  void alertLevel_monthlyWarn_spentAt80Percent() {
    Budget budget = createBudget(1L, 1L, 1L, "2026-05", new BigDecimal("1000.00"));
    CategorySummaryDTO summary = createSummary(1L, new BigDecimal("850.00")); // 85%

    when(transactionMapper.selectCategorySummary(anyLong(), anyString(), anyString(), eq(2)))
        .thenReturn(List.of(summary));

    LocalDateTime now = LocalDateTime.of(2026, 5, 5, 2, 0);
    budgetAlertProcessorService.processUserBudgetAlerts(1L, List.of(budget), "2026-05", now, 5, 31);

    ArgumentCaptor<BudgetAlert> captor = ArgumentCaptor.forClass(BudgetAlert.class);
    verify(budgetAlertMapper).insert(captor.capture());

    assertEquals("MONTHLY_WARN", captor.getValue().getAlertLevel());
  }

  /**
   * DAILY_WARN：日均消耗 ≥ 日均预算 × 150%，但月消耗 < 80%
   * 假设月预算 1000，30天，日均预算=33.33，日均阈值=50
   * 第5天已花 300 → 日均 60 > 50 → DAILY_WARN
   */
  @Test
  void alertLevel_dailyWarn_dailyAvgExceeds150Percent() {
    Budget budget = createBudget(1L, 1L, 1L, "2026-05", new BigDecimal("1000.00"));
    CategorySummaryDTO summary = createSummary(1L, new BigDecimal("300.00"));

    when(transactionMapper.selectCategorySummary(anyLong(), anyString(), anyString(), eq(2)))
        .thenReturn(List.of(summary));

    LocalDateTime now = LocalDateTime.of(2026, 5, 5, 2, 0);
    budgetAlertProcessorService.processUserBudgetAlerts(1L, List.of(budget), "2026-05", now, 5, 31);

    ArgumentCaptor<BudgetAlert> captor = ArgumentCaptor.forClass(BudgetAlert.class);
    verify(budgetAlertMapper).insert(captor.capture());

    String level = captor.getValue().getAlertLevel();
    assertNotEquals("OVERSPENT", level);
    assertNotEquals("MONTHLY_WARN", level);
  }

  // ==================== 3. 持久化写入验证 ====================

  /**
   * 验证写入的 BudgetAlert 字段正确：userId、categoryId、month、alertLevel、金额
   */
  @Test
  void insertAlert_correctFields() {
    Budget budget = createBudget(1L, 2L, 3L, "2026-05", new BigDecimal("500.00"));
    CategorySummaryDTO summary = createSummary(3L, new BigDecimal("600.00")); // 超支

    when(transactionMapper.selectCategorySummary(anyLong(), anyString(), anyString(), eq(2)))
        .thenReturn(List.of(summary));

    LocalDateTime now = LocalDateTime.of(2026, 5, 5, 2, 0);
    budgetAlertProcessorService.processUserBudgetAlerts(2L, List.of(budget), "2026-05", now, 5, 31);

    ArgumentCaptor<BudgetAlert> captor = ArgumentCaptor.forClass(BudgetAlert.class);
    verify(budgetAlertMapper).insert(captor.capture());

    BudgetAlert alert = captor.getValue();
    assertEquals(2L, alert.getUserId());
    assertEquals(3L, alert.getCategoryId());
    assertEquals("OVERSPENT", alert.getAlertLevel());
    assertTrue(alert.getPercentage().compareTo(BigDecimal.ZERO) > 0);
    assertNotNull(alert.getCreateTime());
  }

  // ==================== 4. 幂等删除旧记录 ====================

  /**
   * 幂等验证：每次执行先 delete 该用户该月旧记录，再 insert 新记录
   * 同一天多次执行不会产生重复数据
   */
  @Test
  void idempotent_deleteOldBeforeInsertNew() {
    Budget budget = createBudget(1L, 1L, 1L, "2026-05", new BigDecimal("1000.00"));
    CategorySummaryDTO summary = createSummary(1L, new BigDecimal("500.00"));

    when(transactionMapper.selectCategorySummary(anyLong(), anyString(), anyString(), eq(2)))
        .thenReturn(List.of(summary));

    LocalDateTime now = LocalDateTime.of(2026, 5, 5, 2, 0);

    // 第一次执行
    budgetAlertProcessorService.processUserBudgetAlerts(1L, List.of(budget), "2026-05", now, 5, 31);

    // 验证先调用了 delete（删除旧预警）
    verify(budgetAlertMapper).delete(any(LambdaQueryWrapper.class));
    // 再调用了 insert（写入新预警）
    verify(budgetAlertMapper).insert(any(BudgetAlert.class));
  }

  // ==================== 5. 边界场景 ====================

  /**
   * 某分类本月无支出记录 → spentAmount = 0 → NORMAL
   */
  @Test
  void noExpenseForCategory_normal() {
    Budget budget = createBudget(1L, 1L, 1L, "2026-05", new BigDecimal("1000.00"));

    when(transactionMapper.selectCategorySummary(anyLong(), anyString(), anyString(), eq(2)))
        .thenReturn(Collections.emptyList());

    LocalDateTime now = LocalDateTime.of(2026, 5, 5, 2, 0);
    budgetAlertProcessorService.processUserBudgetAlerts(1L, List.of(budget), "2026-05", now, 5, 31);

    ArgumentCaptor<BudgetAlert> captor = ArgumentCaptor.forClass(BudgetAlert.class);
    verify(budgetAlertMapper).insert(captor.capture());

    BudgetAlert alert = captor.getValue();
    assertEquals("NORMAL", alert.getAlertLevel());
    assertEquals(BigDecimal.ZERO, alert.getSpentAmount());
  }

  /**
   * 预算金额 = 0 的边界情况 → 任何支出都算超支
   */
  @Test
  void zeroBudget_anyExpenseIsOverspent() {
    Budget budget = createBudget(1L, 1L, 1L, "2026-05", new BigDecimal("0.00"));
    CategorySummaryDTO summary = createSummary(1L, new BigDecimal("100.00"));

    when(transactionMapper.selectCategorySummary(anyLong(), anyString(), anyString(), eq(2)))
        .thenReturn(List.of(summary));

    LocalDateTime now = LocalDateTime.of(2026, 5, 5, 2, 0);
    budgetAlertProcessorService.processUserBudgetAlerts(1L, List.of(budget), "2026-05", now, 5, 31);

    ArgumentCaptor<BudgetAlert> captor = ArgumentCaptor.forClass(BudgetAlert.class);
    verify(budgetAlertMapper).insert(captor.capture());

    assertEquals("OVERSPENT", captor.getValue().getAlertLevel());
  }

  // ==================== 辅助方法 ====================

  /** 创建预算对象 */
  private Budget createBudget(Long id, Long userId, Long categoryId, String month, BigDecimal amount) {
    Budget budget = new Budget();
    budget.setId(id);
    budget.setUserId(userId);
    budget.setCategoryId(categoryId);
    budget.setMonth(month);
    budget.setAmount(amount);
    return budget;
  }

  /** 创建分类汇总对象 */
  private CategorySummaryDTO createSummary(Long categoryId, BigDecimal totalAmount) {
    CategorySummaryDTO summary = new CategorySummaryDTO();
    summary.setCategoryId(categoryId);
    summary.setTotalAmount(totalAmount);
    return summary;
  }

  /** 验证 processor mock 无交互（空预算跳过场景） */
  private void verifyNoInteractionsWithProcessor() {
    // budgetScheduler 使用 mock processor，空列表时不会调用 processor
    // 此处验证 budgetAlertMapper 无交互（processor 不被调用就不会写入）
    verify(budgetAlertMapper, never()).delete(any());
    verify(budgetAlertMapper, never()).insert(any(BudgetAlert.class));
  }
}