package com.example.finance.service;

import com.example.finance.entity.dto.BudgetAlertDTO;

import java.util.List;

/**
 * 预算预警服务接口（P2-2）
 *
 * <p>查询 BudgetScheduler 持久化到 budget_alert 表的预警记录。</p>
 */
public interface BudgetAlertService {

  /**
   * 查询当前用户本月的预警记录列表
   *
   * @param userId 当前用户ID（从JWT token解析）
   * @param year   年份，空时默认当前年
   * @param month  月份，空时默认当前月
   * @return 预警记录列表（含分类名称、预警级别、消耗数据）
   */
  List<BudgetAlertDTO> getAlerts(Long userId, String year, String month);
}
