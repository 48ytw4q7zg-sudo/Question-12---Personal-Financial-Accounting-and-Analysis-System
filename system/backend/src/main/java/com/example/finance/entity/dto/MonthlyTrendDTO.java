package com.example.finance.entity.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 月度趋势数据传输对象（前端 DashboardPage.vue 折线图 + AnalyticsPage.vue 趋势图）
 *
 * 按 DATE_FORMAT(time, '%Y-%m') 分组聚合，最多 12 个月
 * 排除转账记录（transfer_id IS NULL）避免虚增
 */
@Data
public class MonthlyTrendDTO {

  /** 月份标签（yyyy-MM 格式，如 2026-01，ECharts X 轴坐标） */
  private String month;

  /** 该月总收入（type=1 且 transfer_id IS NULL 的 amount 合计） */
  private BigDecimal totalIncome;

  /** 该月总支出（type=2 且 transfer_id IS NULL 的 amount 合计） */
  private BigDecimal totalExpense;
}
