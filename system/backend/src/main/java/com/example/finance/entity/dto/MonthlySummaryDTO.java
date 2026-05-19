package com.example.finance.entity.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 月度/年度汇总数据传输对象（前端 DashboardPage.vue 统计卡片 / AnalyticsPage.vue 汇总）
 *
 * balance = totalIncome - totalExpense
 * 年度汇总时 month 字段为 null
 * 所有金额排除 transfer_id 非空的转账记录，避免虚增
 */
@Data
public class MonthlySummaryDTO {

  /** 年份（如 2026） */
  private Integer year;

  /** 月份（1-12，年度汇总时为 null） */
  private Integer month;

  /** 总收入（该期间内 type=1 且 transfer_id IS NULL 的 amount 合计） */
  private BigDecimal totalIncome;

  /** 总支出（该期间内 type=2 且 transfer_id IS NULL 的 amount 合计） */
  private BigDecimal totalExpense;

  /** 结余 = totalIncome - totalExpense（正数绿色/负数红色） */
  private BigDecimal balance;
}
