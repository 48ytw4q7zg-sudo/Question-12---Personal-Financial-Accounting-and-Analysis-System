package com.example.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.finance.entity.Budget;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预算 Mapper — 映射 budget 表（MyBatis-Plus BaseMapper，内置 CRUD）
 *
 * <p>继承 BaseMapper&lt;Budget&gt; 提供标准数据访问方法，无需自定义 SQL。</p>
 * <p>条件查询走 BudgetServiceImpl 中的 LambdaQueryWrapper（按 userId + month 筛选）。</p>
 * <p>INSERT 走 BaseMapper.insert()（BudgetServiceImpl.save() 捕获 DuplicateKeyException 并发兜底）。</p>
 *
 * <p>调用方:</p>
 * <ul>
 *   <li>BudgetServiceImpl.list() — selectList(按 userId+month 查询)</li>
 *   <li>BudgetServiceImpl.save() — selectOne(查重) + insert(新增) / updateById(覆盖更新)</li>
 *   <li>BudgetServiceImpl.delete() — selectById(归属校验) + deleteById(物理删除)</li>
 *   <li>BudgetServiceImpl.getProgress() — selectList(查询该月所有预算)</li>
 *   <li>BudgetScheduler.checkBudgetAlerts() — selectList(按 month 查询所有用户预算 · scheduler/BudgetScheduler.java 第69行)</li>
 * </ul>
 *
 * <p>budget 表唯一约束: uk_budget_user_category_month(user_id, category_id, month) — 并发插入使用 DuplicateKeyException 兜底</p>
 */
@Mapper
public interface BudgetMapper extends BaseMapper<Budget> {
}
