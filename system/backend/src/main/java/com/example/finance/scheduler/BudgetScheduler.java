package com.example.finance.scheduler;

import com.example.finance.entity.Budget;
import com.example.finance.entity.dto.BudgetProgressDTO;
import com.example.finance.mapper.BudgetMapper;
import com.example.finance.mapper.TransactionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预算预警定时任务 (P2-2)
 * 每日检查预算消耗: 日阈值 150% 日均消耗 · 月阈值 80% 月预算
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BudgetScheduler {

  private static final BigDecimal DAILY_THRESHOLD_RATE = new BigDecimal("1.50");
  private static final BigDecimal MONTHLY_THRESHOLD_RATE = new BigDecimal("0.80");

  private final BudgetMapper budgetMapper;
  private final TransactionMapper transactionMapper;

  /**
   * 每日凌晨 2:00 执行预算消耗检查
   * 对每个活跃预算,对比实际支出与预算,超过阈值写入日志
   */
  @Scheduled(cron = "0 0 2 * * ?")
  public void checkBudgetAlerts() {
    log.info("BudgetScheduler: 开始每日预算预警检查");

    LocalDateTime now = LocalDateTime.now();
    String monthStr = String.format("%d-%02d", now.getYear(), now.getMonthValue());

    List<Budget> budgets = budgetMapper.selectList(
        new LambdaQueryWrapper<Budget>().eq(Budget::getMonth, monthStr)
    );

    if (budgets.isEmpty()) {
      log.info("BudgetScheduler: 本月无活跃预算,跳过检查");
      return;
    }

    int dayOfMonth = now.getDayOfMonth();
    int daysInMonth = now.toLocalDate().lengthOfMonth();

    for (Budget budget : budgets) {
      // 查询该分类本月实际支出
      var summaryList = transactionMapper.selectCategorySummary(
          budget.getUserId(),
          now.getYear(),
          now.getMonthValue(),
          1 // type=1 支出
      );
      BigDecimal spent = summaryList.stream()
          .filter(s -> s.getCategoryId().equals(budget.getCategoryId()))
          .findFirst()
          .map(com.example.finance.entity.dto.CategorySummaryDTO::getTotalAmount)
          .orElse(BigDecimal.ZERO);

      // 月阈值检查: 实际支出 > 月预算 × 80%
      BigDecimal monthlyThreshold = budget.getAmount().multiply(MONTHLY_THRESHOLD_RATE);
      if (spent.compareTo(monthlyThreshold) >= 0) {
        log.warn("BudgetScheduler [月预警]: userId={}, categoryId={}, budget={}, spent={}, threshold={} (80%)",
            budget.getUserId(), budget.getCategoryId(), budget.getAmount(), spent, monthlyThreshold);
      }

      // 日阈值检查: 实际支出 > (月预算/本月天数) × 日均 150%
      if (daysInMonth > 0 && budget.getAmount().compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal dailyAvg = budget.getAmount().divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);
        BigDecimal dailyThreshold = dailyAvg.multiply(DAILY_THRESHOLD_RATE);
        BigDecimal dailySpent = spent.divide(BigDecimal.valueOf(dayOfMonth), 2, RoundingMode.HALF_UP);
        if (dailySpent.compareTo(dailyThreshold) >= 0) {
          log.warn("BudgetScheduler [日预警]: userId={}, categoryId={}, dailySpent={}, dailyThreshold={} (150% of daily avg {})",
              budget.getUserId(), budget.getCategoryId(), dailySpent, dailyThreshold, dailyAvg);
        }
      }

      // 已超支检查
      if (spent.compareTo(budget.getAmount()) > 0) {
        log.error("BudgetScheduler [已超支]: userId={}, categoryId={}, budget={}, spent={}",
            budget.getUserId(), budget.getCategoryId(), budget.getAmount(), spent);
      }
    }

    log.info("BudgetScheduler: 预算预警检查完成, 检查 {} 条预算", budgets.size());
  }
}
