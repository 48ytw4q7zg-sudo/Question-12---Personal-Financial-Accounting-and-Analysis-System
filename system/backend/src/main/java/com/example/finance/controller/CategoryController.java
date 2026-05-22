package com.example.finance.controller;

import com.example.finance.common.Result;
import com.example.finance.entity.dto.CategoryDTO;
import com.example.finance.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类控制器（PRD P0-3 收支分类查询 + P0-6 分类浏览）
 *
 * 职责：提供收支分类的查询接口（分类为种子数据，不做用户自定义增改删）
 * 路由前缀：/api/category
 * 依赖：→ CategoryService（业务逻辑层）→ CategoryMapper（数据访问层）
 *
 * 接口清单：
 *   GET /api/category — 查询所有分类（8 支出 + 5 收入 = 13 条种子数据）
 *
 * 被前端调用：→ api/category.js 的 getCategoryList()
 * 被 CategoryPage.vue、TransactionListPage.vue（筛选）、BudgetPage.vue、
 *    RecurringBillPage.vue、AnalyticsPage.vue 等多处调用
 */
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
@Validated
public class CategoryController {

  /** → CategoryService：查询分类列表的业务逻辑 */
  private final CategoryService categoryService;

  /**
   * 查询所有分类接口
   *
   * 流程：直接查 category 表全部种子数据（无分页、无筛选）
   *     → 按 type 分组（1=支出 8 条，2=收入 5 条）
   *
   * @return Result<List<CategoryDTO>> 分类列表（含 id、name、type）
   *
   * 被前端 5 个页面调用：
   *   - CategoryPage.vue：分类浏览（el-tabs 分收入/支出两组展示）
   *   - TransactionListPage.vue：筛选栏 + 新增/编辑表单的分类下拉
   *   - BudgetPage.vue：新增/编辑预算时的分类下拉
   *   - RecurringBillPage.vue：创建周期账单时的分类下拉
   *   - AnalyticsPage.vue：按分类筛选统计数据
   */
  @GetMapping
  public Result<List<CategoryDTO>> list() {
    // → CategoryService.list()：查询 category 表全部记录（种子数据，不过滤用户）
    List<CategoryDTO> list = categoryService.list();
    return Result.success(list);
  }
}
