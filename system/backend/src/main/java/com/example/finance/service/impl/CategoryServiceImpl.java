package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.entity.Category;
import com.example.finance.entity.dto.CategoryDTO;
import com.example.finance.mapper.CategoryMapper;
import com.example.finance.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分类服务实现
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

  private final CategoryMapper categoryMapper;

  /**
   * 查询所有分类
   */
  @Override
  public List<CategoryDTO> list() {
    List<Category> categories = categoryMapper.selectList(
        new LambdaQueryWrapper<Category>().orderByAsc(Category::getId)
    );
    return categories.stream().map(c -> {
      CategoryDTO dto = new CategoryDTO();
      dto.setId(c.getId());
      dto.setName(c.getName());
      dto.setType(c.getType());
      return dto;
    }).toList();
  }
}
