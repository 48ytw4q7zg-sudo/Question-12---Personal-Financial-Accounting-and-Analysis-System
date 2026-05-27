package com.example.finance.service;

import com.example.finance.entity.dto.CategoryDTO;

import java.util.List;

/**
 * 分类服务接口（PRD P0-3 分类查询 · 种子数据，所有用户共享）
 */
public interface CategoryService {

  /**
   * 查询所有收支分类（13 条种子数据：支出 8 条 + 收入 5 条）
   *
   * @return 分类列表（按 id 升序）
   */
  List<CategoryDTO> list();
}
