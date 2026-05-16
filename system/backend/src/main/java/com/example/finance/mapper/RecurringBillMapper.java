package com.example.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.finance.entity.RecurringBill;
import org.apache.ibatis.annotations.Mapper;

/**
 * 周期性账单 Mapper
 */
@Mapper
public interface RecurringBillMapper extends BaseMapper<RecurringBill> {
}
