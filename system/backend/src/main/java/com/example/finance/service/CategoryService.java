package com.example.finance.service;

import com.example.finance.entity.dto.CategoryDTO;

import java.util.List;

/**
 * 分类服务接口
 */
public interface CategoryService {

  /**
   * 查询所有分类
   */
  List<CategoryDTO> list();
}
