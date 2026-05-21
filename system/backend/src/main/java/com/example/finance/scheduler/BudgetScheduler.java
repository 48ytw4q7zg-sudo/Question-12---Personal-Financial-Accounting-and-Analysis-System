package com.example.finance.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.entity.Budget;
import com.example.finance.entity.BudgetAlert;
import com.example.finance.entity.dto.CategorySummaryDTO;
import com.example.finance.mapper.BudgetAlertMapper;
import com.example.finance.mapper.BudgetMapper;
import com.example.finance.mapper.TransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 预算预警定时任务（P2-2 多维度预算预警）
 *
 * <p>每日凌晨 2:00 执行，检查所有用户的各分类预算消耗情况，将预警状态持久化到 budget_alert 表。</p>
 *
 * <p>预警级别（4 级）：</p>
 * <ul>
 *   <li>NORMAL：正常，消耗未达月预算 80%</li>
 *   <li>DAILY_WARN：日预警，日均消耗 ≥ 日均预算 × 150%</li>
 *   <li>MONTHLY_WARN：月预警，总消耗 ≥ 月预算 × 80%</li>
 *   <li>OVERSPENT：已超支，总消耗 > 月预算</li>
 * </ul>
 *
 * <p>性能优化：按用户分组查询分类汇总（消除 N+1），每日先删除旧预警再写入新预警（幂等）。</p>
 *
 * <p>调用链路：@Scheduled → BudgetScheduler → TransactionMapper.selectCategorySummary → BudgetAlertMapper</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BudgetScheduler {

  /** 日阈值：日均消耗 ≥ 日均预算 × 150% 触发日预警 */
  private static final BigDecimal DAILY_THRESHOLD_RATE = new BigDecimal("1.50");
  /** 月阈值：总消耗 ≥ 月预算 × 80% 触发月预警 */
  private static final BigDecimal MONTHLY_THRESHOLD_RATE = new BigDecimal("0.80");

  /** → BudgetMapper：查询本月所有预算 */
  private final BudgetMapper budgetMapper;
  /** → BudgetAlertMapper：持久化预警记录（先删旧再写新，幂等） */
  private final BudgetAlertMapper budgetAlertMapper;
  /** → TransactionMapper：查询各分类支出汇总 */
  private final TransactionMapper transactionMapper;

  /**
   * 每日凌晨 2:00 执行预算预警检查
   *
   * <p>执行流程：</p>
   * <ol>
   *   <li>查询本月所有活跃预算</li>
   *   <li>按用户分组，每用户查询一次分类支出汇总（消除 N+1）</li>
   *   <li>对每条预算计算消耗率，判定预警级别</li>
   *   <li>先删除该用户该月的旧预警记录，再写入新预警（幂等）</li>
   * </ol>
   */
  @Scheduled(cron = "0 0 2 * * ?")
  @Transactional
  public void checkBudgetAlerts() {
    log.info("BudgetScheduler: 开始每日预算预警检查");

    LocalDateTime now = LocalDateTime.now();
    String monthStr = String.format("%d-%02d", now.getYear(), now.getMonthValue());
    int dayOfMonth = now.getDayOfMonth();
    int daysInMonth = now.toLocalDate().lengthOfMonth();

    // 1. 查询本月所有预算
    List<Budget> budgets = budgetMapper.selectList(
        new LambdaQueryWrapper<Budget>().eq(Budget::getMonth, monthStr)
    );

    if (budgets.isEmpty()) {
      log.info("BudgetScheduler: 本月无活跃预算，跳过检查");
      return;
    }

    // 2. 按用户分组处理
    Map<Long, List<Budget>> budgetsByUser = budgets.stream()
        .collect(Collectors.groupingBy(Budget::getUserId));

    int alertCount = 0;
    for (Map.Entry<Long, List<Budget>> entry : budgetsByUser.entrySet()) {
      Long userId = entry.getKey();
      List<Budget> userBudgets = entry.getValue();

      // 2.1 先删除该用户本月的旧预警记录（幂等：同一天多次执行覆盖）
      budgetAlertMapper.delete(
          new LambdaQueryWrapper<BudgetAlert>()
              .eq(BudgetAlert::getUserId, userId)
              .eq(BudgetAlert::getMonth, monthStr)
      );

      // 2.2 查询该用户本月各分类支出汇总（一次性查询，消除 N+1）
      List<CategorySummaryDTO> summaryList = transactionMapper.selectCategorySummary(
          userId, now.getYear(), now.getMonthValue(), 2 // type=2 支出
      );

      // 2.3 构建 categoryId → totalAmount 映射
      Map<Long, BigDecimal> spentMap = new java.util.HashMap<>();
      if (summaryList != null) {
        for (CategorySummaryDTO summary : summaryList) {
          spentMap.put(summary.getCategoryId(), summary.getTotalAmount());
        }
      }

      // 2.4 对每条预算计算预警级别并持久化
      for (Budget budget : userBudgets) {
        BigDecimal spent = spentMap.getOrDefault(budget.getCategoryId(), BigDecimal.ZERO);
        String alertLevel = calculateAlertLevel(budget, spent, dayOfMonth, daysInMonth);

        // 计算百分比
        BigDecimal percentage = BigDecimal.ZERO;
        if (budget.getAmount().compareTo(BigDecimal.ZERO) > 0) {
          percentage = spent.divide(budget.getAmount(), 4, RoundingMode.HALF_UP)
              .multiply(BigDecimal.valueOf(100));
        }

        // 持久化预警记录到数据库
        BudgetAlert alert = new BudgetAlert();
        alert.setUserId(userId);
        alert.setCategoryId(budget.getCategoryId());
        alert.setMonth(monthStr);
        alert.setAlertLevel(alertLevel);
        alert.setBudgetAmount(budget.getAmount());
        alert.setSpentAmount(spent);
        alert.setPercentage(percentage);
        alert.setCreateTime(now);
        budgetAlertMapper.insert(alert);

        if (!"NORMAL".equals(alertLevel)) {
          alertCount++;
          // 日志仍保留，便于实时排查
          if ("OVERSPENT".equals(alertLevel)) {
            log.error("BudgetScheduler [已超支]: userId={}, categoryId={}, budget={}, spent={}",
                userId, budget.getCategoryId(), budget.getAmount(), spent);
          } else {
            log.warn("BudgetScheduler [{}]: userId={}, categoryId={}, budget={}, spent={}",
                alertLevel, userId, budget.getCategoryId(), budget.getAmount(), spent);
          }
        }
      }
    }

    log.info("BudgetScheduler: 预算预警检查完成，检查 {} 条预算，生成 {} 条预警", budgets.size(), alertCount);
  }

  /**
   * 计算预警级别
   *
   * <p>判定优先级（从高到低）：</p>
   * <ol>
   *   <li>OVERSPENT：已消耗 > 预算金额</li>
   *   <li>MONTHLY_WARN：已消耗 ≥ 预算 × 80%</li>
   *   <li>DAILY_WARN：日均消耗 ≥ (预算/月天数) × 150%</li>
   *   <li>NORMAL：正常</li>
   * </ol>
   *
   * @param budget 预算记录
   * @param spent  已消耗金额
   * @param dayOfMonth 当前日期（月内第几天）
   * @param daysInMonth 本月总天数
   * @return 预警级别字符串
   */
  private String calculateAlertLevel(Budget budget, BigDecimal spent, int dayOfMonth, int daysInMonth) {
    // 已超支：已消耗 > 预算
    if (spent.compareTo(budget.getAmount()) > 0) {
      return "OVERSPENT";
    }

    // 月预警：已消耗 ≥ 预算 × 80%
    BigDecimal monthlyThreshold = budget.getAmount().multiply(MONTHLY_THRESHOLD_RATE);
    if (spent.compareTo(monthlyThreshold) >= 0) {
      return "MONTHLY_WARN";
    }

    // 日预警：日均消耗 ≥ 日均预算 × 150%
    if (daysInMonth > 0 && dayOfMonth > 0 && budget.getAmount().compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal dailyBudget = budget.getAmount().divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);
      BigDecimal dailyThreshold = dailyBudget.multiply(DAILY_THRESHOLD_RATE);
      BigDecimal dailySpent = spent.divide(BigDecimal.valueOf(dayOfMonth), 2, RoundingMode.HALF_UP);
      if (dailySpent.compareTo(dailyThreshold) >= 0) {
        return "DAILY_WARN";
      }
    }

    return "NORMAL";
  }
}
