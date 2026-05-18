package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.common.BusinessException;
import com.example.finance.entity.Budget;
import com.example.finance.entity.Category;
import com.example.finance.entity.dto.BudgetDTO;
import com.example.finance.entity.dto.BudgetProgressDTO;
import com.example.finance.entity.dto.BudgetRequest;
import com.example.finance.mapper.BudgetMapper;
import com.example.finance.mapper.CategoryMapper;
import com.example.finance.mapper.TransactionMapper;
import com.example.finance.service.BudgetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 预算服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

  // category.type=1 为支出分类
  private static final int CATEGORY_TYPE_EXPENSE = 1;
  // transaction.type=2 为支出
  private static final int TRANSACTION_TYPE_EXPENSE = 2;

  private final BudgetMapper budgetMapper;
  private final CategoryMapper categoryMapper;
  private final TransactionMapper transactionMapper;

  /**
   * 查询用户预算列表
   */
  @Override
  public List<BudgetDTO> list(Long userId, String year, String month) {
    if (year == null || month == null) {
      LocalDateTime now = LocalDateTime.now();
      year = String.valueOf(now.getYear());
      month = String.valueOf(now.getMonthValue());
    }
    // 构造 month 格式: yyyy-MM
    String monthStr = year + "-" + (month.length() == 1 ? "0" + month : month);

    List<Budget> budgets = budgetMapper.selectList(
        new LambdaQueryWrapper<Budget>()
            .eq(Budget::getUserId, userId)
            .eq(Budget::getMonth, monthStr)
    );

    // 批量加载分类名称，避免 N+1 查询
    Set<Long> categoryIds = budgets.stream().map(Budget::getCategoryId).collect(Collectors.toSet());
    Map<Long, String> categoryNameMap = categoryIds.isEmpty()
        ? Map.of()
        : categoryMapper.selectByIds(categoryIds).stream()
            .collect(Collectors.toMap(Category::getId, Category::getName));

    return budgets.stream().map(b -> {
      BudgetDTO dto = new BudgetDTO();
      dto.setId(b.getId());
      dto.setCategoryId(b.getCategoryId());
      dto.setMonth(b.getMonth());
      dto.setAmount(b.getAmount());
      dto.setCreateTime(b.getCreateTime());
      dto.setUpdateTime(b.getUpdateTime());
      dto.setCategoryName(categoryNameMap.getOrDefault(b.getCategoryId(), ""));
      return dto;
    }).toList();
  }

  /**
   * 保存预算（INSERT ON DUPLICATE KEY UPDATE · @Transactional 保证并发安全）
   */
  @Override
  @Transactional
  public BudgetDTO save(Long userId, BudgetRequest request) {
    // 校验分类存在
    Category category = categoryMapper.selectById(request.getCategoryId());
    if (category == null) {
      throw new BusinessException(4001, "分类不存在");
    }
    // 预算仅针对支出分类（category.type=1 为支出）
    if (category.getType() != CATEGORY_TYPE_EXPENSE) {
      throw new BusinessException(4001, "预算仅可设置在支出分类上");
    }

    // R-05-issue-6: 已修复 - 捕获DuplicateKeyException并发兜底
    // 查询是否已存在该月该分类的预算
    Budget existing = budgetMapper.selectOne(
        new LambdaQueryWrapper<Budget>()
            .eq(Budget::getUserId, userId)
            .eq(Budget::getCategoryId, request.getCategoryId())
            .eq(Budget::getMonth, request.getMonth())
    );

    Budget budget;
    if (existing != null) {
      // 更新
      budget = existing;
      budget.setAmount(request.getAmount());
      budget.setUpdateTime(LocalDateTime.now());
      budgetMapper.updateById(budget);
    } else {
      // 新增
      budget = new Budget();
      budget.setUserId(userId);
      budget.setCategoryId(request.getCategoryId());
      budget.setMonth(request.getMonth());
      budget.setAmount(request.getAmount());
      budget.setCreateTime(LocalDateTime.now());
      budget.setUpdateTime(LocalDateTime.now());
      try {
        budgetMapper.insert(budget);
      } catch (DuplicateKeyException e) {
        // 并发插入时唯一索引拦截，重新查询已存在的记录
        log.warn("预算并发插入冲突，重新查询: userId={}, categoryId={}, month={}", userId, request.getCategoryId(), request.getMonth());
        budget = budgetMapper.selectOne(
            new LambdaQueryWrapper<Budget>()
                .eq(Budget::getUserId, userId)
                .eq(Budget::getCategoryId, request.getCategoryId())
                .eq(Budget::getMonth, request.getMonth())
        );
        if (budget != null) {
          budget.setAmount(request.getAmount());
          budget.setUpdateTime(LocalDateTime.now());
          budgetMapper.updateById(budget);
        }
      }
    }

    BudgetDTO dto = new BudgetDTO();
    dto.setId(budget.getId());
    dto.setCategoryId(budget.getCategoryId());
    dto.setCategoryName(category.getName());
    dto.setMonth(budget.getMonth());
    dto.setAmount(budget.getAmount());
    dto.setCreateTime(budget.getCreateTime());
    dto.setUpdateTime(budget.getUpdateTime());
    return dto;
  }

  /**
   * 获取预算进度
   */
  @Override
  public List<BudgetProgressDTO> getProgress(Long userId, String year, String month) {
    if (year == null || month == null) {
      LocalDateTime now = LocalDateTime.now();
      year = String.valueOf(now.getYear());
      month = String.valueOf(now.getMonthValue());
    }
    List<BudgetDTO> budgets = list(userId, year, month);
    int yearInt = Integer.parseInt(year);
    int monthInt = Integer.parseInt(month);

    // R-05-issue-2: 已修复 - selectCategorySummary提到循环外消除N+1查询
    var summaryList = transactionMapper.selectCategorySummary(userId, yearInt, monthInt, TRANSACTION_TYPE_EXPENSE);
    java.util.Map<Long, java.math.BigDecimal> spentMap = summaryList.stream()
        .collect(java.util.stream.Collectors.toMap(
            com.example.finance.entity.dto.CategorySummaryDTO::getCategoryId,
            com.example.finance.entity.dto.CategorySummaryDTO::getTotalAmount,
            (a, b) -> a
        ));

    List<BudgetProgressDTO> result = new ArrayList<>();
    for (BudgetDTO budget : budgets) {
      BudgetProgressDTO dto = new BudgetProgressDTO();
      dto.setCategoryId(budget.getCategoryId());
      dto.setCategoryName(budget.getCategoryName());
      dto.setBudgetAmount(budget.getAmount());

      BigDecimal spent = spentMap.getOrDefault(budget.getCategoryId(), BigDecimal.ZERO);
      dto.setSpentAmount(spent);

      // 计算百分比
      if (budget.getAmount().compareTo(BigDecimal.ZERO) > 0) {
        dto.setPercentage(spent.divide(budget.getAmount(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
      } else {
        dto.setPercentage(BigDecimal.ZERO);
      }

      // 是否超支
      dto.setOverspent(spent.compareTo(budget.getAmount()) > 0);

      result.add(dto);
    }
    return result;
  }

  /**
   * 获取预算预警（仅返回超支项）
   */
  @Override
  public List<BudgetProgressDTO> getAlert(Long userId, String year, String month) {
    List<BudgetProgressDTO> all = getProgress(userId, year, month);
    return all.stream()
        .filter(BudgetProgressDTO::isOverspent)
        .toList();
  }
}
