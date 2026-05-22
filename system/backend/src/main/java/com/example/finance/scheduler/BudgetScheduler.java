package com.example.finance.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.entity.Budget;
import com.example.finance.mapper.BudgetMapper;
import com.example.finance.service.BudgetAlertProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;

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
 * <p>调用链路：@Scheduled → BudgetScheduler → BudgetAlertProcessorService(REQUIRES_NEW) → TransactionMapper + BudgetAlertMapper</p>
 *
 * <p>架构说明：processUserBudgetAlerts 提取为独立 BudgetAlertProcessorService，
 * 解决原 BudgetScheduler 内部 this 调用 @Transactional(REQUIRES_NEW) 的 Spring AOP 代理自调用失效问题。
 * 通过注入的 Spring 代理对象调用，REQUIRES_NEW 事务传播才能正确创建独立事务。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BudgetScheduler {

  /** → BudgetMapper：查询本月所有预算 */
  private final BudgetMapper budgetMapper;
  /** → BudgetAlertProcessorService：单用户预警处理（REQUIRES_NEW 独立事务，通过 Spring 代理调用） */
  private final BudgetAlertProcessorService budgetAlertProcessorService;

  /**
   * 每日凌晨 2:00 执行预算预警检查
   *
   * <p>执行流程：</p>
   * <ol>
   *   <li>查询本月所有活跃预算</li>
   *   <li>按用户分组，每用户通过 BudgetAlertProcessorService（REQUIRES_NEW）独立事务处理</li>
   *   <li>单用户失败不影响其他用户</li>
   * </ol>
   */
  @Scheduled(cron = "0 0 2 * * ?")
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

    // 2. 按用户分组处理（每个用户独立事务，避免全局回滚）
    Map<Long, List<Budget>> budgetsByUser = budgets.stream()
        .collect(Collectors.groupingBy(Budget::getUserId));

    int alertCount = 0;
    for (Map.Entry<Long, List<Budget>> entry : budgetsByUser.entrySet()) {
      Long userId = entry.getKey();
      List<Budget> userBudgets = entry.getValue();
      try {
        // 通过注入的 Spring 代理调用，REQUIRES_NEW 事务传播正确生效
        alertCount += budgetAlertProcessorService.processUserBudgetAlerts(userId, userBudgets, monthStr, now, dayOfMonth, daysInMonth);
      } catch (Exception e) {
        log.error("BudgetScheduler: 用户 {} 预算预警处理失败", userId, e);
        // 单用户失败不影响其他用户（REQUIRES_NEW 事务已独立回滚）
      }
    }

    log.info("BudgetScheduler: 预算预警检查完成，检查 {} 条预算，生成 {} 条预警", budgets.size(), alertCount);
  }
}