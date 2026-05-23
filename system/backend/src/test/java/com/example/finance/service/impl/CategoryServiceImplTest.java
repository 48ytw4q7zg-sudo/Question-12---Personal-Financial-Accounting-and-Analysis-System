package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.entity.Category;
import com.example.finance.entity.dto.CategoryDTO;
import com.example.finance.mapper.CategoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 分类服务实现类单元测试
 *
 * <p>测试覆盖场景：</p>
 * <ul>
 *   <li>查询分类列表 - 正常查询、空数据、收入类型映射、支出类型映射、收入支出混合</li>
 * </ul>
 *
 * <p>业务特点：</p>
 * <ul>
 *   <li>分类为种子数据（13条：支出8 + 收入5），由数据库初始化脚本预置</li>
 *   <li>所有登录用户共享，不支持用户自定义增改删</li>
 *   <li>type 取值：1=收入，2=支出</li>
 * </ul>
 *
 * @see CategoryServiceImpl
 * @see Category
 * @see CategoryDTO
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

  @Mock
  private CategoryMapper categoryMapper;

  @InjectMocks
  private CategoryServiceImpl categoryService;

  @Test
  @DisplayName("查询分类列表成功")
  void list_success() {
    Category c1 = new Category();
    c1.setId(1L);
    c1.setName("餐饮");
    c1.setType(1);
    Category c2 = new Category();
    c2.setId(9L);
    c2.setName("工资");
    c2.setType(2);

    when(categoryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(c1, c2));

    List<CategoryDTO> result = categoryService.list();
    assertEquals(2, result.size());
    assertEquals("餐饮", result.get(0).getName());
    assertEquals("工资", result.get(1).getName());
  }

  @Test
  @DisplayName("查询分类列表 - 空数据")
  void list_empty() {
    when(categoryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

    List<CategoryDTO> result = categoryService.list();
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("查询分类列表 - 收入类型正确映射")
  void list_incomeTypeMapping() {
    Category c = new Category();
    c.setId(3L);
    c.setName("工资");
    c.setType(1);

    when(categoryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(c));

    List<CategoryDTO> result = categoryService.list();
    assertEquals(1, result.size());
    assertEquals(1, result.get(0).getType());
    assertEquals("工资", result.get(0).getName());
    assertEquals(3L, result.get(0).getId());
  }

  @Test
  @DisplayName("查询分类列表 - 支出类型正确映射")
  void list_expenseTypeMapping() {
    Category c = new Category();
    c.setId(5L);
    c.setName("餐饮");
    c.setType(2);

    when(categoryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(c));

    List<CategoryDTO> result = categoryService.list();
    assertEquals(1, result.size());
    assertEquals(2, result.get(0).getType());
  }

  @Test
  @DisplayName("查询分类列表 - 收入支出混合并正确区分")
  void list_mixedIncomeExpense() {
    Category income = new Category();
    income.setId(1L);
    income.setName("工资");
    income.setType(1);
    Category expense = new Category();
    expense.setId(2L);
    expense.setName("餐饮");
    expense.setType(2);

    when(categoryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(income, expense));

    List<CategoryDTO> result = categoryService.list();
    assertEquals(2, result.size());
    assertTrue(result.stream().anyMatch(d -> d.getType() == 1));
    assertTrue(result.stream().anyMatch(d -> d.getType() == 2));
  }
}
