package com.example.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.finance.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分类 Mapper — 映射 category 表（MyBatis-Plus BaseMapper，仅只读查询）
 *
 * <p>继承 BaseMapper&lt;Category&gt; 提供 selectList/selectByIds 等只读方法。</p>
 * <p>分类为种子数据（13 条：支出 8 + 收入 5），由 sql/01-init.sql 初始化，不做用户自定义增改删。</p>
 * <p>查询走 CategoryServiceImpl 中的 LambdaQueryWrapper.orderByAsc(Category::getId)，缓存策略使用 @Cacheable。</p>
 *
 * <p>调用方:</p>
 * <ul>
 *   <li>CategoryServiceImpl.list() — selectList(全量分类 · 按 id 升序 · @Cacheable 缓存)</li>
 *   <li>BudgetServiceImpl.list() — selectByIds(批量加载分类名称 · 消除 N+1)</li>
 *   <li>BudgetServiceImpl.save() — selectById(校验分类存在 + 支出分类限制)</li>
 *   <li>BudgetServiceImpl.getProgress() — selectByIds(批量加载分类名称)</li>
 *   <li>TransactionServiceImpl.create/update() — EntityValidator.validateCategory → selectById(分类存在校验)</li>
 *   <li>TransactionServiceImpl.transfer() — selectOne(查询"其他"支出分类 ID)</li>
 *   <li>TransactionServiceImpl.importCsv() — selectList(全量分类缓存到内存 Map)</li>
 *   <li>RecurringBillServiceImpl.list() — selectByIds(批量加载分类名称)</li>
 *   <li>RecurringBillServiceImpl.create/update() — EntityValidator.validateCategory → selectById(分类存在校验)</li>
 *   <li>BudgetAlertServiceImpl.getAlerts() — selectByIds(批量加载分类名称)</li>
 * </ul>
 *
 * <p>category 表索引: idx_category_type(type) — 支出/收入分类的 type 字段二元索引</p>
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
