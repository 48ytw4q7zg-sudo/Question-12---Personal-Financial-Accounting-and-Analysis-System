package com.example.finance.service;

import com.example.finance.entity.Budget;

import java.time.LocalDateTime;

/**
 * 预算预警单用户处理接口（独立事务，从 BudgetScheduler 提取以解决 Spring AOP 代理自调用问题）
 *
 * <p>为什么独立 Service：BudgetScheduler 内部调用 @Transactional(REQUIRES_NEW) 的方法
 * 属于 Spring 代理自调用（this.processUserBudgetAlerts()），AOP 拦截不到，REQUIRES_NEW 无效。
 * 提取为独立 Service 后，BudgetScheduler 通过注入的代理调用，事务传播才能正确生效。</p>
 *
 * <p>调用方: BudgetScheduler.checkBudgetAlerts() → budgetAlertProcessorService.processUserBudgetAlerts()</p>
 */
public interface BudgetAlertProcessorService {

  /**
   * 单用户预算预警处理（独立事务，单用户失败不影响其他用户）
   *
   * @param userId 用户 ID
   * @param userBudgets 该用户的预算列表
   * @param monthStr 月份字符串（yyyy-MM 格式）
   * @param now 当前时间
   * @param dayOfMonth 月内第几天
   * @param daysInMonth 本月总天数
   * @return 预警数量
   */
  int processUserBudgetAlerts(Long userId, java.util.List<Budget> userBudgets,
      String monthStr, LocalDateTime now, int dayOfMonth, int daysInMonth);
}