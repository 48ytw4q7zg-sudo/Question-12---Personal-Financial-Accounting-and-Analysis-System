package com.example.finance.service.impl;

import com.example.finance.common.EntityValidator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.entity.BudgetAlert;
import com.example.finance.entity.Category;
import com.example.finance.entity.dto.BudgetAlertDTO;
import com.example.finance.mapper.BudgetAlertMapper;
import com.example.finance.mapper.CategoryMapper;
import com.example.finance.service.BudgetAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;  // 只读事务注解

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 预算预警服务实现（P2-2）
 *
 * <p>查询 BudgetScheduler 持久化到 budget_alert 表的预警记录，填充分类名称后返回。</p>
 *
 * <p>调用方: BudgetController GET /api/budget/alert</p>
 */
@Service
@RequiredArgsConstructor
public class BudgetAlertServiceImpl implements BudgetAlertService {

  /** → BudgetAlertMapper：查询预警记录 */
  private final BudgetAlertMapper budgetAlertMapper;
  /** → CategoryMapper：批量加载分类名称 */
  private final CategoryMapper categoryMapper;

  /**
   * 查询当前用户本月的预警记录
   *
   * <p>流程：查询预警记录 → 批量加载分类名称 → 填充 categoryName → 返回。</p>
   * <p>性能优化：批量加载分类名称消除 N+1 查询。</p>
   * <p>预警记录由 BudgetScheduler 每日凌晨 2:00 定时生成并持久化到 budget_alert 表。</p>
   *
   * <p>调用链: BudgetController.getAlert() → BudgetAlertService.getAlerts() → BudgetAlertMapper.selectList() + CategoryMapper.selectByIds()
   *   → 前端 api/budget.js getBudgetAlert() → BudgetPage.vue / DashboardPage.vue</p>
   *
   * @param userId 当前用户ID（从JWT token解析）
   * @param year   年份，空时默认当前年
   * @param month  月份，空时默认当前月
   * @return 预警记录列表（含 4 级预警级别: OVERSPENT/MONTHLY_WARN/DAILY_WARN/NORMAL）
   */
  @Override
  @Transactional(readOnly = true)                                         // 只读事务（与项目其他只读方法一致，如 AccountServiceImpl.list() 第77行、CategoryServiceImpl.list() 第34行）
  public List<BudgetAlertDTO> getAlerts(Long userId, String year, String month) {
    // 【步骤①】格式化年月参数（空值默认当前年月 · → common/EntityValidator.java 的 defaultAndFormatYearMonth）
    String monthStr = EntityValidator.defaultAndFormatYearMonth(year, month);

    // 【步骤②】查询预算预警记录（→ mapper/BudgetAlertMapper.java 的 selectList · budget_alert 表）
    List<BudgetAlert> alerts = budgetAlertMapper.selectList(
        new LambdaQueryWrapper<BudgetAlert>()
            .eq(BudgetAlert::getUserId, userId)                           // 筛选当前用户
            .eq(BudgetAlert::getMonth, monthStr)                          // 筛选指定月份
    );

    // 【步骤③】批量加载分类名称（消除 N+1 查询 · → mapper/CategoryMapper.java 的 selectByIds）
    Set<Long> categoryIds = alerts.stream().map(BudgetAlert::getCategoryId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
    Map<Long, String> categoryNameMap = categoryIds.isEmpty()
        ? Map.of()                                                         // 无预警记录 → 空 Map 兜底
        : categoryMapper.selectByIds(categoryIds).stream()                 // → CategoryMapper.java selectByIds() · 一次性查询所有分类
            .collect(Collectors.toMap(Category::getId, Category::getName)); // 分类 ID → 分类名称映射

    // 【步骤④】Entity 转 DTO（逐条填充分类名称 + 预警数据）
    return alerts.stream().map(alert -> {
      BudgetAlertDTO dto = new BudgetAlertDTO();
      dto.setId(alert.getId());                                            // 预警记录 ID
      dto.setCategoryId(alert.getCategoryId());                            // 分类 ID
      dto.setCategoryName(categoryNameMap.getOrDefault(alert.getCategoryId(), ""));  // 分类名称（从预加载 Map 获取）
      dto.setMonth(alert.getMonth());                                      // 月份（yyyy-MM）
      dto.setAlertLevel(alert.getAlertLevel());                            // 预警级别（OVERSPENT/MONTHLY_WARN/DAILY_WARN/NORMAL）
      dto.setBudgetAmount(alert.getBudgetAmount());                        // 预算金额
      dto.setSpentAmount(alert.getSpentAmount());                          // 已消耗金额
      dto.setPercentage(alert.getPercentage());                            // 消耗百分比
      return dto;
    }).toList();                                                           // Java 16+ 不可变列表 → BudgetController → 前端 BudgetPage.vue
  }
}
