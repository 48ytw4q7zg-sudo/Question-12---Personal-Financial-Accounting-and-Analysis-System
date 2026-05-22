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
import org.springframework.transaction.annotation.Transactional;

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
   *
   * @param userId 当前用户ID（从JWT token解析）
   * @param year   年份，空时默认当前年
   * @param month  月份，空时默认当前月
   * @return 预警记录列表
   */
  @Override
  @Transactional(readOnly = true)
  public List<BudgetAlertDTO> getAlerts(Long userId, String year, String month) {
    String monthStr = EntityValidator.defaultAndFormatYearMonth(year, month);

    // 查询该用户本月预警记录
    List<BudgetAlert> alerts = budgetAlertMapper.selectList(
        new LambdaQueryWrapper<BudgetAlert>()
            .eq(BudgetAlert::getUserId, userId)
            .eq(BudgetAlert::getMonth, monthStr)
    );

    // 批量加载分类名称，消除 N+1 查询
    Set<Long> categoryIds = alerts.stream().map(BudgetAlert::getCategoryId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
    Map<Long, String> categoryNameMap = categoryIds.isEmpty()
        ? Map.of()
        : categoryMapper.selectByIds(categoryIds).stream()
            .collect(Collectors.toMap(Category::getId, Category::getName));

    // 转换为 DTO
    return alerts.stream().map(alert -> {
      BudgetAlertDTO dto = new BudgetAlertDTO();
      dto.setId(alert.getId());
      dto.setCategoryId(alert.getCategoryId());
      dto.setCategoryName(categoryNameMap.getOrDefault(alert.getCategoryId(), ""));
      dto.setMonth(alert.getMonth());
      dto.setAlertLevel(alert.getAlertLevel());
      dto.setBudgetAmount(alert.getBudgetAmount());
      dto.setSpentAmount(alert.getSpentAmount());
      dto.setPercentage(alert.getPercentage());
      return dto;
    }).toList();
  }
}
