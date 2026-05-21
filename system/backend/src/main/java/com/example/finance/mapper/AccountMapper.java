package com.example.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.finance.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 账户 Mapper（MyBatis-Plus BaseMapper，内置 CRUD）
 *
 * 简单 CRUD 起 BaseMapper 内置方法（selectById/insert/updateById/deleteById）
 * 条件查询走 AccountServiceImpl 中的 LambdaQueryWrapper
 * selectByIdForUpdate 用于转账场景的悲观锁（防并发透支）
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {

  /** 悲观锁查询：SELECT ... FOR UPDATE（转账余额检查时使用，防 TOCTOU 并发透支） */
  @Select("SELECT * FROM account WHERE id = #{id} FOR UPDATE")
  Account selectByIdForUpdate(Long id);
}