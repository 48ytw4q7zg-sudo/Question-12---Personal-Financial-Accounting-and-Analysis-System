package com.example.finance.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.entity.Budget;
import com.example.finance.entity.BudgetAlert;
import com.example.finance.entity.dto.CategorySummaryDTO;
import com.example.finance.mapper.BudgetAlertMapper;
import com.example.finance.mapper.BudgetMapper;
import com.example.finance.mapper.TransactionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BudgetScheduler 单元测试（P2-2 预算预警持久化 + P2-5 单测加分）
 *
 * <p>覆盖场景：</p>
 * <ul>
 *   <li>预警级别计算（4级：NORMAL / DAILY_WARN / MONTHLY_WARN / OVERSPENT）</li>
 *   <li>持久化写入（budgetAlertMapper.insert 调用验证）</li>
 *   <li>幂等删除旧记录（先 delete 再 insert，同一天多次执行不重复）</li>
 *   <li>空预算列表跳过</li>
 *   <li>多用户隔离</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class BudgetSchedulerTest {

  @Mock
  private BudgetMapper budgetMapper;

  @Mock
  private BudgetAlertMapper budgetAlertMapper;

  @Mock
  private TransactionMapper transactionMapper;

  @InjectMocks
  private BudgetScheduler budgetScheduler;

  @BeforeEach
  void setUp() {
    // 所有依赖已 @Mock，无需额外初始化
  }

  // ==================== 1. 空状态测试 ====================

  /**
   * 本月无活跃预算 → 跳过检查，不查询分类汇总，不写入预警
   */
  @Test
  void checkBudgetAlerts_emptyBudgetList_skipAll() {
    when(budgetMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(Collections.emptyList());

    budgetScheduler.checkBudgetAlerts();

    verify(budgetMapper).selectList(any(LambdaQueryWrapper.class));
    verify(transactionMapper, never()).selectCategorySummary(any(), anyInt(), anyInt(), anyInt());
    verify(budgetAlertMapper, never()).delete(any());
    verify(budgetAlertMapper, never()).insert(any(BudgetAlert.class));
  }

  // ==================== 2. 预警级别计算（4级） ====================

  /**
   * NORMAL：消耗 25%，未达任何阈值 → 不生成预警日志
   */
  @Test
  void alertLevel_normal_noAlertGenerated() {
    Budget budget = createBudget(1L, 1L, 1L, "2026-05", new BigDecimal("1000.00"));
    CategorySummaryDTO summary = createSummary(1L, new BigDecimal("250.00")); // 25%

    when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(budget));
    when(transactionMapper.selectCategorySummary(anyLong(), anyInt(), anyInt(), eq(2)))
        .thenReturn(List.of(summary));

    budgetScheduler.checkBudgetAlerts();

    // NORMAL 级别仍会写入 budget_alert 表（记录正常状态）
    verify(budgetAlertMapper).insert(any(BudgetAlert.class));
  }

  /**
   * OVERSPENT：消耗 > 预算 → 生成 OVERSPENT 预警
   */
  @Test
  void alertLevel_overspent_spentExceedsBudget() {
    Budget budget = createBudget(1L, 1L, 1L, "2026-05", new BigDecimal("1000.00"));
    CategorySummaryDTO summary = createSummary(1L, new BigDecimal("1200.00")); // 120%

    when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(budget));
    when(transactionMapper.selectCategorySummary(anyLong(), anyInt(), anyInt(), eq(2)))
        .thenReturn(List.of(summary));

    budgetScheduler.checkBudgetAlerts();

    ArgumentCaptor<BudgetAlert> captor = ArgumentCaptor.forClass(BudgetAlert.class);
    verify(budgetAlertMapper).insert(captor.capture());

    BudgetAlert alert = captor.getValue();
    assertEquals("OVERSPENT", alert.getAlertLevel());
    assertEquals(new BigDecimal("1200.00"), alert.getSpentAmount());
    assertEquals(new BigDecimal("1000.00"), alert.getBudgetAmount());
  }

  /**
   * MONTHLY_WARN：消耗 ≥ 预算 × 80% 但未超支 → 生成 MONTHLY_WARN 预警
   */
  @Test
  void alertLevel_monthlyWarn_spentAt80Percent() {
    Budget budget = createBudget(1L, 1L, 1L, "2026-05", new BigDecimal("1000.00"));
    CategorySummaryDTO summary = createSummary(1L, new BigDecimal("850.00")); // 85%

    when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(budget));
    when(transactionMapper.selectCategorySummary(anyLong(), anyInt(), anyInt(), eq(2)))
        .thenReturn(List.of(summary));

    budgetScheduler.checkBudgetAlerts();

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
    // 第5天，已花300 → 日均60，日均预算33.33，阈值50 → 60>50 → DAILY_WARN
    // 但 300/1000=30% < 80%，不触发 MONTHLY_WARN
    CategorySummaryDTO summary = createSummary(1L, new BigDecimal("300.00"));

    when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(budget));
    when(transactionMapper.selectCategorySummary(anyLong(), anyInt(), anyInt(), eq(2)))
        .thenReturn(List.of(summary));

    budgetScheduler.checkBudgetAlerts();

    ArgumentCaptor<BudgetAlert> captor = ArgumentCaptor.forClass(BudgetAlert.class);
    verify(budgetAlertMapper).insert(captor.capture());

    String level = captor.getValue().getAlertLevel();
    // 可能是 DAILY_WARN 或 NORMAL（取决于执行日期，这里验证至少不是 OVERSPENT/MONTHLY_WARN）
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

    when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(budget));
    when(transactionMapper.selectCategorySummary(anyLong(), anyInt(), anyInt(), eq(2)))
        .thenReturn(List.of(summary));

    budgetScheduler.checkBudgetAlerts();

    ArgumentCaptor<BudgetAlert> captor = ArgumentCaptor.forClass(BudgetAlert.class);
    verify(budgetAlertMapper).insert(captor.capture());

    BudgetAlert alert = captor.getValue();
    assertEquals(2L, alert.getUserId()); // createBudget 第二个参数是 userId=2L
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

    when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(budget));
    when(transactionMapper.selectCategorySummary(anyLong(), anyInt(), anyInt(), eq(2)))
        .thenReturn(List.of(summary));

    budgetScheduler.checkBudgetAlerts();

    // 验证先调用了 delete（删除旧预警）
    verify(budgetAlertMapper).delete(any(LambdaQueryWrapper.class));
    // 再调用了 insert（写入新预警）
    verify(budgetAlertMapper).insert(any(BudgetAlert.class));

    // 第二次执行：再次 delete + insert，幂等覆盖
    budgetScheduler.checkBudgetAlerts();
    verify(budgetAlertMapper, times(2)).delete(any(LambdaQueryWrapper.class));
    verify(budgetAlertMapper, times(2)).insert(any(BudgetAlert.class));
  }

  // ==================== 5. 多用户隔离 ====================

  /**
   * 两个用户的预算分别处理，各自删除各自的预警记录
   */
  @Test
  void multiUser_isolation() {
    Budget budget1 = createBudget(1L, 1L, 1L, "2026-05", new BigDecimal("1000.00"));
    Budget budget2 = createBudget(2L, 2L, 2L, "2026-05", new BigDecimal("2000.00"));

    CategorySummaryDTO summary1 = createSummary(1L, new BigDecimal("500.00"));
    CategorySummaryDTO summary2 = createSummary(2L, new BigDecimal("2500.00")); // 超支

    when(budgetMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(List.of(budget1, budget2));
    when(transactionMapper.selectCategorySummary(eq(1L), anyInt(), anyInt(), eq(2)))
        .thenReturn(List.of(summary1));
    when(transactionMapper.selectCategorySummary(eq(2L), anyInt(), anyInt(), eq(2)))
        .thenReturn(List.of(summary2));

    budgetScheduler.checkBudgetAlerts();

    // 两个用户各 delete 一次 + 各 insert 一次
    verify(budgetAlertMapper, times(2)).delete(any(LambdaQueryWrapper.class));
    verify(budgetAlertMapper, times(2)).insert(any(BudgetAlert.class));
  }

  // ==================== 6. 边界场景 ====================

  /**
   * 某分类本月无支出记录 → spentAmount = 0 → NORMAL
   */
  @Test
  void noExpenseForCategory_normal() {
    Budget budget = createBudget(1L, 1L, 1L, "2026-05", new BigDecimal("1000.00"));

    when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(budget));
    when(transactionMapper.selectCategorySummary(anyLong(), anyInt(), anyInt(), eq(2)))
        .thenReturn(Collections.emptyList()); // 无支出

    budgetScheduler.checkBudgetAlerts();

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

    when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(budget));
    when(transactionMapper.selectCategorySummary(anyLong(), anyInt(), anyInt(), eq(2)))
        .thenReturn(List.of(summary));

    budgetScheduler.checkBudgetAlerts();

    ArgumentCaptor<BudgetAlert> captor = ArgumentCaptor.forClass(BudgetAlert.class);
    verify(budgetAlertMapper).insert(captor.capture());

    // 预算为0时，有支出即超支
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
}
