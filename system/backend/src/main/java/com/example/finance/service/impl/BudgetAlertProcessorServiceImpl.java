package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.common.EntityValidator;
import com.example.finance.common.enums.TransactionType;
import com.example.finance.entity.Budget;
import com.example.finance.entity.BudgetAlert;
import com.example.finance.entity.dto.CategorySummaryDTO;
import com.example.finance.mapper.BudgetAlertMapper;
import com.example.finance.mapper.TransactionMapper;
import com.example.finance.service.BudgetAlertProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 预算预警单用户处理实现（独立事务，REQUIRES_NEW 通过 Spring 代理正确生效）
 *
 * <p>从 BudgetScheduler 提取：原 BudgetScheduler 内部 this 调用 @Transactional(REQUIRES_NEW)
 * 无法被 AOP 代理拦截，导致 REQUIRES_NEW 无效（同类事务传播失效）。提取为独立 Service 后，
 * BudgetScheduler 通过注入的 Spring 代理对象调用，REQUIRES_NEW 传播才能正确创建新事务。</p>
 *
 * <p>事务隔离效果：每个用户独立事务，单用户处理失败只回滚该用户的预警记录，不影响其他用户。</p>
 *
 * <p>调用链路: BudgetScheduler.checkBudgetAlerts() → budgetAlertProcessorService.processUserBudgetAlerts()</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetAlertProcessorServiceImpl implements BudgetAlertProcessorService {

  /** 预警级别常量 — 替代硬编码字符串，防止拼写错误 */
  private static final String ALERT_OVERSPENT = "OVERSPENT";
  private static final String ALERT_MONTHLY_WARN = "MONTHLY_WARN";
  private static final String ALERT_DAILY_WARN = "DAILY_WARN";
  private static final String ALERT_NORMAL = "NORMAL";
  /** 日阈值：日均消耗 ≥ 日均预算 × 150% 触发日预警 */
  private static final BigDecimal DAILY_THRESHOLD_RATE = new BigDecimal("1.50");
  /** 月阈值：总消耗 ≥ 月预算 × 80% 触发月预警 */
  private static final BigDecimal MONTHLY_THRESHOLD_RATE = new BigDecimal("0.80");
  /** 百分比转换因子：小数转百分比（×100） */
  private static final BigDecimal PERCENTAGE_FACTOR = BigDecimal.valueOf(100);

  /** → BudgetAlertMapper：持久化预警记录（先删旧再写新，幂等） */
  private final BudgetAlertMapper budgetAlertMapper;
  /** → TransactionMapper：查询各分类支出汇总 */
  private final TransactionMapper transactionMapper;

  /**
   * 单用户预算预警处理（独立事务，单用户失败不影响其他用户）
   *
   * <p>流程：</p>
   * <ol>
   *   <li>删除该用户该月的旧预警记录（幂等：同一天多次执行覆盖）</li>
   *   <li>查询该用户本月各分类支出汇总（一次性查询，消除 N+1）</li>
   *   <li>对每条预算计算预警级别并持久化</li>
   * </ol>
   *
   * @param userId 用户 ID
   * @param userBudgets 该用户的预算列表
   * @param monthStr 月份字符串（yyyy-MM 格式）
   * @param now 当前时间
   * @param dayOfMonth 月内第几天
   * @param daysInMonth 本月总天数
   * @return 预警数量
   */
  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int processUserBudgetAlerts(Long userId, List<Budget> userBudgets,
      String monthStr, LocalDateTime now, int dayOfMonth, int daysInMonth) {
    // 先删除该用户本月的旧预警记录（幂等：同一天多次执行覆盖）
    budgetAlertMapper.delete(  // 删除旧预警记录(幂等)
        new LambdaQueryWrapper<BudgetAlert>()
            .eq(BudgetAlert::getUserId, userId)  // 筛选当前用户
            .eq(BudgetAlert::getMonth, monthStr)  // 筛选指定月份
    );

    // 查询该用户本月各分类支出汇总（一次性查询，消除 N+1 · 范围查询利用 idx_transaction_user_time 索引）
    // 复用 EntityValidator.buildMonthRange() 消除重复代码
    int yearVal = now.getYear();  // 从当前时间提取年份
    int monthVal = now.getMonthValue();  // 从当前时间提取月份
    String[] monthRange = EntityValidator.buildMonthRange(yearVal, monthVal);  // 构建月份范围时间字符串（复用EntityValidator公共方法）
    List<CategorySummaryDTO> summaryList = transactionMapper.selectCategorySummary(  // 批量查询各分类支出汇总
        userId, monthRange[0], monthRange[1], TransactionType.EXPENSE.getValue() // 支出类型
    );

    // 构建 categoryId → totalAmount 映射
    Map<Long, BigDecimal> spentMap = new java.util.HashMap<>();  // 分类ID→支出金额映射
    if (summaryList != null) {  // null保护
      for (CategorySummaryDTO summary : summaryList) {  // 遍历汇总结果
        spentMap.put(summary.getCategoryId(), summary.getTotalAmount());  // 填充映射
      }
    }

    int alertCount = 0;  // 预警计数器
    // 对每条预算计算预警级别并持久化
    for (Budget budget : userBudgets) {  // 遍历用户所有预算
      BigDecimal spent = spentMap.getOrDefault(budget.getCategoryId(), BigDecimal.ZERO);  // 获取该分类实际支出(默认0)
      String alertLevel = calculateAlertLevel(budget, spent, dayOfMonth, daysInMonth);  // 计算预警级别

      // 计算百分比
      BigDecimal percentage = BigDecimal.ZERO;  // 百分比默认0
      if (budget.getAmount().compareTo(BigDecimal.ZERO) > 0) {  // 预算金额>0才计算
        percentage = spent.divide(budget.getAmount(), 4, RoundingMode.HALF_UP)  // 已用/预算(保留4位)
            .multiply(PERCENTAGE_FACTOR);  // ×100转为百分比（使用常量消除魔法数字）
      }

      // 持久化预警记录到数据库
      BudgetAlert alert = new BudgetAlert();  // 创建预警记录实体
      alert.setUserId(userId);  // 设置用户ID
      alert.setCategoryId(budget.getCategoryId());  // 设置分类ID
      alert.setMonth(monthStr);  // 设置月份
      alert.setAlertLevel(alertLevel);  // 设置预警级别
      alert.setBudgetAmount(budget.getAmount());  // 设置预算金额
      alert.setSpentAmount(spent);  // 设置已用金额
      alert.setPercentage(percentage);  // 设置百分比
      alert.setCreateTime(now);  // 设置创建时间
      budgetAlertMapper.insert(alert);  // 插入数据库

      if (!ALERT_NORMAL.equals(alertLevel)) {  // 非正常级别则计入预警
        alertCount++;  // 预警计数+1
        if (ALERT_OVERSPENT.equals(alertLevel)) {  // 超支级别
          log.error("BudgetScheduler [已超支]: userId={}, categoryId={}, budget={}, spent={}",  // 超支用error级别
              userId, budget.getCategoryId(), budget.getAmount(), spent);
        } else {  // 日预警或月预警
          log.warn("BudgetScheduler [{}]: userId={}, categoryId={}, budget={}, spent={}",  // 其他预警用warn级别
              alertLevel, userId, budget.getCategoryId(), budget.getAmount(), spent);
        }
      }
    }
    return alertCount;  // 返回预警数量
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
  private String calculateAlertLevel(Budget budget, BigDecimal spent, int dayOfMonth, int daysInMonth) {  // 计算预警级别
    // 已超支：已消耗 > 预算
    if (spent.compareTo(budget.getAmount()) > 0) {  // 支出超过预算金额
      return ALERT_OVERSPENT;  // 返回超支级别
    }

    // 月预警：已消耗 ≥ 预算 × 80%
    BigDecimal monthlyThreshold = budget.getAmount().multiply(MONTHLY_THRESHOLD_RATE);  // 计算月预警阈值(预算×0.8)
    if (spent.compareTo(monthlyThreshold) >= 0) {  // 支出达到月预警阈值
      return ALERT_MONTHLY_WARN;  // 返回月预警级别
    }

    // 日预警：日均消耗 ≥ 日均预算 × 150%
    if (daysInMonth > 0 && dayOfMonth > 0 && budget.getAmount().compareTo(BigDecimal.ZERO) > 0) {  // 前置条件满足
      BigDecimal dailyBudget = budget.getAmount().divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);  // 计算日均预算
      BigDecimal dailyThreshold = dailyBudget.multiply(DAILY_THRESHOLD_RATE);  // 计算日预警阈值(日均×1.5)
      BigDecimal dailySpent = spent.divide(BigDecimal.valueOf(dayOfMonth), 2, RoundingMode.HALF_UP);  // 计算日均消耗
      if (dailySpent.compareTo(dailyThreshold) >= 0) {  // 日均消耗达到日预警阈值
        return ALERT_DAILY_WARN;  // 返回日预警级别
      }
    }

    return ALERT_NORMAL;  // 返回正常级别
  }
}