package com.example.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.finance.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper（MyBatis-Plus BaseMapper，内置 CRUD）
 *
 * 条件查询走 UserServiceImpl 中的 LambdaQueryWrapper（如按 username 查重）
 * 无需自定义 SQL 方法
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
