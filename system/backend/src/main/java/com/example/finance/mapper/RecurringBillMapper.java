package com.example.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.finance.entity.RecurringBill;
import org.apache.ibatis.annotations.Mapper;

/**
 * 周期性账单 Mapper（MyBatis-Plus BaseMapper，内置 CRUD）
 *
 * 条件查询走 RecurringBillServiceImpl 中的 LambdaQueryWrapper（按 userId + status 筛选）
 * 更新走 BaseMapper.updateById()（status 软删除 / 编辑字段 / 下次到期日）
 * 无需自定义 SQL 方法
 */
@Mapper
public interface RecurringBillMapper extends BaseMapper<RecurringBill> {
}
