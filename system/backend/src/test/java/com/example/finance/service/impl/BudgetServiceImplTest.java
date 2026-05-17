package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.common.BusinessException;
import com.example.finance.entity.Budget;
import com.example.finance.entity.Category;
import com.example.finance.entity.dto.BudgetDTO;
import com.example.finance.entity.dto.BudgetRequest;
import com.example.finance.mapper.BudgetMapper;
import com.example.finance.mapper.CategoryMapper;
import com.example.finance.mapper.TransactionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceImplTest {

  @Mock
  private BudgetMapper budgetMapper;
  @Mock
  private CategoryMapper categoryMapper;
  @Mock
  private TransactionMapper transactionMapper;

  @InjectMocks
  private BudgetServiceImpl budgetService;

  private Category expenseCategory;
  private Category incomeCategory;

  @BeforeEach
  void setUp() {
    expenseCategory = new Category();
    expenseCategory.setId(1L);
    expenseCategory.setName("餐饮");
    expenseCategory.setType(1);

    incomeCategory = new Category();
    incomeCategory.setId(9L);
    incomeCategory.setName("工资");
    incomeCategory.setType(2);
  }

  @Test
  @DisplayName("保存预算成功 - 新建")
  void save_newBudget() {
    when(categoryMapper.selectById(1L)).thenReturn(expenseCategory);
    when(budgetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
    when(budgetMapper.insert(any(Budget.class))).thenReturn(1);

    BudgetRequest request = new BudgetRequest();
    request.setCategoryId(1L);
    request.setMonth("2026-05");
    request.setAmount(new BigDecimal("2000.00"));

    BudgetDTO dto = budgetService.save(1L, request);
    assertNotNull(dto);
    assertEquals(new BigDecimal("2000.00"), dto.getAmount());
    assertEquals("餐饮", dto.getCategoryName());
    verify(budgetMapper).insert(any(Budget.class));
  }

  @Test
  @DisplayName("保存预算失败 - 仅支出分类可设预算")
  void save_incomeCategoryNotAllowed() {
    when(categoryMapper.selectById(9L)).thenReturn(incomeCategory);

    BudgetRequest request = new BudgetRequest();
    request.setCategoryId(9L);
    request.setMonth("2026-05");
    request.setAmount(new BigDecimal("5000.00"));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> budgetService.save(1L, request));
    assertEquals(4001, ex.getCode());
  }

  @Test
  @DisplayName("保存预算失败 - 分类不存在")
  void save_categoryNotFound() {
    when(categoryMapper.selectById(999L)).thenReturn(null);

    BudgetRequest request = new BudgetRequest();
    request.setCategoryId(999L);
    request.setMonth("2026-05");
    request.setAmount(new BigDecimal("1000.00"));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> budgetService.save(1L, request));
    assertEquals(4001, ex.getCode());
  }

  @Test
  @DisplayName("预算列表查询成功")
  void list_success() {
    Budget budget = new Budget();
    budget.setId(1L);
    budget.setUserId(1L);
    budget.setCategoryId(1L);
    budget.setMonth("2026-05");
    budget.setAmount(new BigDecimal("2000.00"));

    when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(budget));
    when(categoryMapper.selectByIds(anyCollection())).thenReturn(List.of(expenseCategory));

    List<BudgetDTO> result = budgetService.list(1L, "2026", "5");
    assertEquals(1, result.size());
    assertEquals(new BigDecimal("2000.00"), result.get(0).getAmount());
    assertEquals("餐饮", result.get(0).getCategoryName());
  }

  @Test
  @DisplayName("预算列表空数据")
  void list_empty() {
    when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

    List<BudgetDTO> result = budgetService.list(1L, "2026", "5");
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("预警只返回超支项")
  void alert_onlyOverspent() {
    Category cat2 = new Category();
    cat2.setId(2L);
    cat2.setName("交通");
    cat2.setType(1);

    Budget budget1 = new Budget();
    budget1.setId(1L);
    budget1.setUserId(1L);
    budget1.setCategoryId(1L);
    budget1.setMonth("2026-05");
    budget1.setAmount(new BigDecimal("2000.00"));

    Budget budget2 = new Budget();
    budget2.setId(2L);
    budget2.setUserId(1L);
    budget2.setCategoryId(2L);
    budget2.setMonth("2026-05");
    budget2.setAmount(new BigDecimal("500.00"));

    when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(budget1, budget2));
    when(categoryMapper.selectByIds(anyCollection())).thenReturn(List.of(expenseCategory, cat2));
    // 餐饮花了 500（未超），交通花了 600（超支）
    var summary1 = new com.example.finance.entity.dto.CategorySummaryDTO();
    summary1.setCategoryId(1L);
    summary1.setTotalAmount(new BigDecimal("500.00"));
    var summary2 = new com.example.finance.entity.dto.CategorySummaryDTO();
    summary2.setCategoryId(2L);
    summary2.setTotalAmount(new BigDecimal("600.00"));
    when(transactionMapper.selectCategorySummary(eq(1L), eq(2026), eq(5), eq(1)))
        .thenReturn(List.of(summary1, summary2));

    var alerts = budgetService.getAlert(1L, "2026", "5");
    assertEquals(1, alerts.size());
    assertEquals(2L, alerts.get(0).getCategoryId());
    assertTrue(alerts.get(0).isOverspent());
  }

  @Test
  @DisplayName("保存预算 - 更新已存在的预算")
  void save_updateExisting() {
    Budget existing = new Budget();
    existing.setId(1L);
    existing.setUserId(1L);
    existing.setCategoryId(1L);
    existing.setMonth("2026-05");
    existing.setAmount(new BigDecimal("1000.00"));

    when(categoryMapper.selectById(1L)).thenReturn(expenseCategory);
    when(budgetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
    when(budgetMapper.updateById(any(Budget.class))).thenReturn(1);

    BudgetRequest request = new BudgetRequest();
    request.setCategoryId(1L);
    request.setMonth("2026-05");
    request.setAmount(new BigDecimal("3000.00"));

    BudgetDTO dto = budgetService.save(1L, request);
    assertNotNull(dto);
    assertEquals(new BigDecimal("3000.00"), dto.getAmount());
    verify(budgetMapper).updateById(any(Budget.class));
    verify(budgetMapper, never()).insert(any(Budget.class));
  }

  @Test
  @DisplayName("预算进度 - 超支标记正确")
  void progress_overspentFlag() {
    Budget budget = new Budget();
    budget.setId(1L);
    budget.setUserId(1L);
    budget.setCategoryId(1L);
    budget.setMonth("2026-05");
    budget.setAmount(new BigDecimal("1000.00"));

    when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(budget));
    when(categoryMapper.selectByIds(anyCollection())).thenReturn(List.of(expenseCategory));
    var summary = new com.example.finance.entity.dto.CategorySummaryDTO();
    summary.setCategoryId(1L);
    summary.setTotalAmount(new BigDecimal("1200.00"));
    when(transactionMapper.selectCategorySummary(eq(1L), eq(2026), eq(5), eq(1)))
        .thenReturn(List.of(summary));

    var progress = budgetService.getProgress(1L, "2026", "5");
    assertEquals(1, progress.size());
    assertTrue(progress.get(0).isOverspent());
    assertEquals(new BigDecimal("1200.00"), progress.get(0).getSpentAmount());
  }
}
