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
 * 分类服务实现（PRD P0-3 收支分类查询）
 *
 * 分类为种子数据（13 条：支出 8 + 收入 5），由 sql/01-init.sql 初始化
 * 所有登录用户共享，不支持用户自定义增改删
 * 被 5 个前端页面/组件复用（CategoryPage / TransactionListPage / BudgetPage / RecurringBillPage / AnalyticsPage）
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

  /** → CategoryMapper：分类数据访问（只读查询） */
  private final CategoryMapper categoryMapper;

  /**
   * 查询所有分类（种子数据，全量返回，无分页无筛选）
   *
   * @return 分类列表（按 id 升序，含 id/name/type）
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
