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
 *
 * <p>对应 PRD 功能: P1-3 预算管理(月预算按分类设置 + 超支标记)。</p>
 *
 * <p>关键业务规则:</p>
 * <ul>
 *   <li>预算仅适用于支出分类(category.type=1), 收入分类不可设置预算 (PRD P1-3 异常流程②)</li>
 *   <li>同一用户+同一分类+同一月份仅一条预算, 重复保存时覆盖写入(先查后改, 并发时DuplicateKeyException兜底)</li>
 *   <li>预算进度 = 本月该分类实际支出 / 预算金额 × 100%, 超支标记(overspent=true)当已用>预算</li>
 *   <li>getProgress() 使用批量查询消除N+1: 先查全部预算, 再一次性查各分类支出汇总, 最后内存聚合</li>
 *   <li>预警(getAlert)仅返回超支项(overspent=true), 由前端BudgetPage展示红色警告</li>
 * </ul>
 *
 * <p>并发安全: save() 使用 @Transactional + DuplicateKeyException 捕获并发插入冲突, 回退为更新。</p>
 *
 * <p>调用方: BudgetController (controller/BudgetController.java)</p>
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
   * 查询用户指定月份的预算列表
   *
   * <p>对应 PRD P1-3 GET /api/budget。</p>
   * <p>批量加载分类名称(避免N+1查询), 无预算的支出分类不返回。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param year 年份(如"2026"), 空时默认当前年
   * @param month 月份(如"5"), 空时默认当前月
   * @return 该月所有预算列表(含分类名称)
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
   * 保存预算（有则更新, 无则插入 · @Transactional 保证并发安全）
   *
   * <p>对应 PRD P1-3 POST /api/budget。</p>
   * <p>幂等策略: 先查询是否存在同用户+同分类+同月预算, 存在则updateById, 不存在则insert。</p>
   * <p>并发安全: insert时若被唯一索引uk_budget_user_category_month拦截(DuplicateKeyException),
   * 重新查询已存在记录并转为updateById, 确保并发场景下不报错。</p>
   * <p>校验: 分类必须存在且为支出分类(type=1)。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param request 预算请求参数(含categoryId/month/amount)
   * @return 保存后的预算DTO(含分类名称)
   * @throws BusinessException 4001=分类不存在 / 4001=预算仅可设置在支出分类上
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
   * 获取预算消耗进度（含已用金额/百分比/超支标记）
   *
   * <p>对应 PRD P1-3 GET /api/budget/progress。</p>
   * <p>性能优化(R-05-issue-2): 将selectCategorySummary提到循环外, 一次性查询所有分类的支出汇总,
   * 再在内存中聚合, 消除N+1查询。</p>
   * <p>计算公式: percentage = (spentAmount / budgetAmount) × 100%, overspent = spentAmount > budgetAmount。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param year 年份(如"2026"), 空时默认当前年
   * @param month 月份(如"5"), 空时默认当前月
   * @return 各分类预算进度列表(含预算金额/已用金额/百分比/是否超支)
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
   * 获取预算预警列表（仅返回超支项）
   *
   * <p>对应 PRD P2-2 GET /api/budget/alert。</p>
   * <p>复用getProgress()结果, 过滤overspent=true的项。</p>
   * <p>教学简化: 当前仅返回超支项, 日预警(日均150%)和月预警(月耗80%)由BudgetScheduler定时任务日志输出。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param year 年份, 空时默认当前年
   * @param month 月份, 空时默认当前月
   * @return 超支的分类预算列表(空集合表示无预警)
   */
  @Override
  public List<BudgetProgressDTO> getAlert(Long userId, String year, String month) {
    List<BudgetProgressDTO> all = getProgress(userId, year, month);
    return all.stream()
        .filter(BudgetProgressDTO::isOverspent)
        .toList();
  }
}
