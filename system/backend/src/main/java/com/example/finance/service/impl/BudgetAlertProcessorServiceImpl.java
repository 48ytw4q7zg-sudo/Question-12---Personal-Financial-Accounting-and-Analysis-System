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
  @Transactional(propagation = Propagation.REQUIRES_NEW)                                    // 独立事务：单个用户失败只回滚该用户，不影响其他
  public int processUserBudgetAlerts(Long userId, List<Budget> userBudgets,
      String monthStr, LocalDateTime now, int dayOfMonth, int daysInMonth) {
    // ═══ 阶段一：幂等清理 — 删除该用户本月的旧预警记录 ═══
    // → mapper/BudgetAlertMapper.java 的 delete（继承自 BaseMapper<BudgetAlert>）
    budgetAlertMapper.delete(
        new LambdaQueryWrapper<BudgetAlert>()
            .eq(BudgetAlert::getUserId, userId)                         // 筛选当前用户
            .eq(BudgetAlert::getMonth, monthStr)                        // 筛选指定月份（yyyy-MM 格式）
    );

    // ═══ 阶段二：查询支出汇总 — 一次性查询该用户本月各分类支出 ═══
    // 复用 EntityValidator.buildMonthRange() 构建月份范围（common/EntityValidator.java）
    int yearVal = now.getYear();                                         // 从当前时间提取年份
    int monthVal = now.getMonthValue();                                  // 从当前时间提取月份
    String[] monthRange = EntityValidator.buildMonthRange(yearVal, monthVal);  // → EntityValidator.buildMonthRange() 返回 [startOfMonth, startOfNextMonth]
    // → mapper/TransactionMapper.java 的 selectCategorySummary · XML 映射 · 范围查询利用 idx_transaction_user_time 索引
    List<CategorySummaryDTO> summaryList = transactionMapper.selectCategorySummary(
        userId, monthRange[0], monthRange[1], TransactionType.EXPENSE.getValue()  // 支出类型(type=2)
    );

    // 构建 categoryId → totalAmount 映射（null 保护：XML mapper 无数据返回 null）
    Map<Long, BigDecimal> spentMap = new java.util.HashMap<>();         // 分类ID→支出金额映射
    if (summaryList != null) {                                           // null 保护
      for (CategorySummaryDTO summary : summaryList) {                  // 遍历每类汇总
        spentMap.put(summary.getCategoryId(), summary.getTotalAmount()); // 分类ID → 支出总额
      }
    }

    // ═══ 阶段三：逐条计算预警级别并持久化 ═══
    int alertCount = 0;                                                  // 预警计数器
    for (Budget budget : userBudgets) {                                 // 遍历该用户所有预算
      // 获取该分类实际支出（无数据默认 BigDecimal.ZERO）
      BigDecimal spent = spentMap.getOrDefault(budget.getCategoryId(), BigDecimal.ZERO);
      // 计算预警级别（→ this.calculateAlertLevel() · private 方法 · 第160行）
      String alertLevel = calculateAlertLevel(budget, spent, dayOfMonth, daysInMonth);

      // 计算消耗百分比（已用 ÷ 预算 × 100% · 精度 2 位 · 对齐 BudgetServiceImpl.getProgress()）
      BigDecimal percentage = BigDecimal.ZERO;                          // 百分比默认 0（预算为 0 时）
      if (budget.getAmount().compareTo(BigDecimal.ZERO) > 0) {          // 预算金额 > 0 才计算
        percentage = spent.divide(budget.getAmount(), 4, RoundingMode.HALF_UP)  // 已用÷预算（4位中间精度防丢失）
            .multiply(PERCENTAGE_FACTOR)                                 // ×100 转为百分比
            .setScale(2, RoundingMode.HALF_UP);                         // 保留 2 位小数（对齐 BudgetServiceImpl 精度规范）
      }

      // 持久化预警记录到 budget_alert 表（→ mapper/BudgetAlertMapper.java 的 insert）
      BudgetAlert alert = new BudgetAlert();                            // 创建预警实体
      alert.setUserId(userId);                                          // 用户 ID
      alert.setCategoryId(budget.getCategoryId());                      // 分类 ID
      alert.setMonth(monthStr);                                         // 月份（yyyy-MM）
      alert.setAlertLevel(alertLevel);                                  // 预警级别（OVERSPENT/MONTHLY_WARN/DAILY_WARN/NORMAL）
      alert.setBudgetAmount(budget.getAmount());                        // 预算金额
      alert.setSpentAmount(spent);                                      // 已消耗金额
      alert.setPercentage(percentage);                                  // 消耗百分比
      alert.setCreateTime(now);                                         // 创建时间
      budgetAlertMapper.insert(alert);                                  // → 写入 budget_alert 表

      // 日志分级：超支→error / 预警→warn / 正常→不打印
      if (!ALERT_NORMAL.equals(alertLevel)) {                           // 非正常级别
        alertCount++;                                                   // 预警计数 +1
        if (ALERT_OVERSPENT.equals(alertLevel)) {                       // 超支级别 → error 级日志
          log.error("BudgetScheduler [已超支]: userId={}, categoryId={}, budget={}, spent={}",
              userId, budget.getCategoryId(), budget.getAmount(), spent);
        } else {                                                        // 日预警或月预警 → warn 级日志
          log.warn("BudgetScheduler [{}]: userId={}, categoryId={}, budget={}, spent={}",
              alertLevel, userId, budget.getCategoryId(), budget.getAmount(), spent);
        }
      }
    }
    return alertCount;                                                   // 返回预警数量 → BudgetScheduler 累计全局预警计数
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
    // null安全防护：预算金额可能因手动SQL插入而为NULL（调用 entity/Budget.java 的 getAmount 方法）
    BigDecimal budgetAmount = budget.getAmount() != null ? budget.getAmount() : BigDecimal.ZERO;  // 预算金额null→0兜底
    // 已超支：已消耗 > 预算
    if (spent.compareTo(budgetAmount) > 0) {  // 支出超过预算金额
      return ALERT_OVERSPENT;  // 返回超支级别
    }

    // 月预警：已消耗 ≥ 预算 × 80%
    BigDecimal monthlyThreshold = budgetAmount.multiply(MONTHLY_THRESHOLD_RATE);  // 计算月预警阈值(预算×0.8)
    if (spent.compareTo(monthlyThreshold) >= 0) {  // 支出达到月预警阈值
      return ALERT_MONTHLY_WARN;  // 返回月预警级别
    }

    // 日预警：日均消耗 ≥ 日均预算 × 150%
    if (daysInMonth > 0 && dayOfMonth > 0 && budgetAmount.compareTo(BigDecimal.ZERO) > 0) {  // 前置条件满足
      BigDecimal dailyBudget = budgetAmount.divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);  // 计算日均预算
      BigDecimal dailyThreshold = dailyBudget.multiply(DAILY_THRESHOLD_RATE);  // 计算日预警阈值(日均×1.5)
      BigDecimal dailySpent = spent.divide(BigDecimal.valueOf(dayOfMonth), 2, RoundingMode.HALF_UP);  // 计算日均消耗
      if (dailySpent.compareTo(dailyThreshold) >= 0) {  // 日均消耗达到日预警阈值
        return ALERT_DAILY_WARN;  // 返回日预警级别
      }
    }

    return ALERT_NORMAL;  // 返回正常级别
  }
}