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
  /**
   * 每日凌晨 2:00 执行预算预警检查（cron: 0 0 2 * * ?）
   *
   * <p>执行流程：</p>
   * <ol>
   *   <li>查询本月所有活跃预算（→ mapper/BudgetMapper.java selectList · budget 表按 month 筛选）</li>
   *   <li>按用户分组（Collectors.groupingBy），每组独立事务处理</li>
   *   <li>每用户委托 BudgetAlertProcessorService（REQUIRES_NEW · 独立事务）计算预警级别并持久化到 budget_alert 表</li>
   *   <li>单用户失败只回滚该用户的预警记录，不影响其他用户</li>
   * </ol>
   *
   * <p>为何提取为独立 Service：Scheduler 内部 this 调用 @Transactional 不走 AOP 代理，
   * REQUIRES_NEW 传播无效。提取为独立 BudgetAlertProcessorService 后通过注入的代理调用，传播才能正确生效。</p>
   *
   * <p>调用链: Spring @Scheduled → BudgetScheduler.checkBudgetAlerts() → BudgetAlertProcessorService.processUserBudgetAlerts()
   *   → TransactionMapper.selectCategorySummary() + BudgetAlertMapper.insert()</p>
   */
  @Scheduled(cron = "0 0 2 * * ?")                                      // Spring 定时：每天凌晨 2:00 整触发
  public void checkBudgetAlerts() {
    long startTime = System.currentTimeMillis();                         // 记录执行开始时间（执行耗时监控）
    log.info("BudgetScheduler: 开始每日预算预警检查");

    // ─── 步骤一：准备时间参数 ───
    LocalDateTime now = LocalDateTime.now();                             // 当前时间（用于记录 createdAt）
    String monthStr = String.format("%d-%02d", now.getYear(), now.getMonthValue());  // 本月 yyyy-MM 格式
    int dayOfMonth = now.getDayOfMonth();                                // 月内第几天（用于日预警计算）
    int daysInMonth = now.toLocalDate().lengthOfMonth();                 // 本月总天数（用于日均计算）

    // ─── 步骤二：查询本月所有预算 ───
    // → mapper/BudgetMapper.java 的 selectList（继承自 BaseMapper<Budget>）· budget 表按 month 筛选
    List<Budget> budgets = budgetMapper.selectList(
        new LambdaQueryWrapper<Budget>().eq(Budget::getMonth, monthStr)
    );

    if (budgets.isEmpty()) {                                             // 本月无预算
      log.info("BudgetScheduler: 本月无活跃预算，跳过检查");
      return;
    }

    // ─── 步骤三：按用户分组处理 ───
    // Collectors.groupingBy 按 userId 分组，每个用户独立事务（通过 BudgetAlertProcessorService 注入的代理调用）
    Map<Long, List<Budget>> budgetsByUser = budgets.stream()
        .collect(Collectors.groupingBy(Budget::getUserId));             // userId → 该用户的所有预算列表

    int alertCount = 0;                                                  // 全局预警累计计数器
    for (Map.Entry<Long, List<Budget>> entry : budgetsByUser.entrySet()) {
      Long userId = entry.getKey();                                      // 当前用户 ID
      List<Budget> userBudgets = entry.getValue();                       // 该用户的本月预算列表
      try {
        // → service/BudgetAlertProcessorService.java 的 processUserBudgetAlerts（REQUIRES_NEW 独立事务）
        // 通过注入的 Spring 代理调用，REQUIRES_NEW 事务传播正确生效
        alertCount += budgetAlertProcessorService.processUserBudgetAlerts(
            userId, userBudgets, monthStr, now, dayOfMonth, daysInMonth);
      } catch (Exception e) {                                            // 单用户处理异常
        log.error("BudgetScheduler: 用户 {} 预算预警处理失败", userId, e);
        // 不抛异常：single-user-fail 不阻断其他用户（REQUIRES_NEW 事务已独立回滚该用户的预警记录）
      }
    }

    // ─── 步骤四：执行报告 ───
    long elapsed = System.currentTimeMillis() - startTime;               // 计算执行耗时（毫秒）
    log.info("BudgetScheduler: 预算预警检查完成，检查 {} 条预算，生成 {} 条预警，耗时 {} ms",
        budgets.size(), alertCount, elapsed);                            // 生产环境性能监控必备
  }
}