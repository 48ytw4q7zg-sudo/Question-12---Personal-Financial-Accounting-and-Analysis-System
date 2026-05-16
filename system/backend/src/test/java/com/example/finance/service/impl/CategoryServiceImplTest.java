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
 * CategoryServiceImpl 单元测试
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
}
