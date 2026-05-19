package com.example.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.finance.entity.Account;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账户 Mapper（MyBatis-Plus BaseMapper，内置 CRUD）
 *
 * 简单 CRUD 走 BaseMapper 内置方法（selectById/insert/updateById/deleteById）
 * 条件查询走 AccountServiceImpl 中的 LambdaQueryWrapper
 * 无需自定义 SQL 方法
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {
}
