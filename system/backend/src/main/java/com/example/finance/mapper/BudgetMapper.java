package com.example.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.finance.entity.Budget;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预算 Mapper（MyBatis-Plus BaseMapper，内置 CRUD）
 *
 * 条件查询走 BudgetServiceImpl 中的 LambdaQueryWrapper（按 userId + month 筛选）
 * INSERT 走 BaseMapper.insert()（BudgetServiceImpl.save() 捕获 DuplicateKeyException 并发兜底）
 * 无需自定义 SQL 方法
 */
@Mapper
public interface BudgetMapper extends BaseMapper<Budget> {
}
