package com.example.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.finance.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分类 Mapper（MyBatis-Plus BaseMapper，内置 CRUD · 只读使用）
 *
 * 分类为种子数据，不做用户自定义增改删
 * 查询走 CategoryServiceImpl 中的 LambdaQueryWrapper.orderByAsc(Category::getId)
 * 无需自定义 SQL 方法
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
