package com.example.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.finance.entity.BudgetAlert;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预算预警 Mapper（对应 budget_alert 表 · P2-2）
 *
 * <p>由 BudgetScheduler 定时写入预警记录，BudgetController 查询预警列表。</p>
 */
@Mapper
public interface BudgetAlertMapper extends BaseMapper<BudgetAlert> {
}
