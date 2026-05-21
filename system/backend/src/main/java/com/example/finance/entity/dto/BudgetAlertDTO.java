package com.example.finance.entity.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 预算预警 DTO（P2-2）
 *
 * <p>对应 budget_alert 表，由 BudgetScheduler 每日写入，前端通过 GET /api/budget/alert 查询。</p>
 */
@Data
public class BudgetAlertDTO {

  /** 预警记录ID */
  private Long id;

  /** 关联分类ID */
  private Long categoryId;

  /** 分类名称（由 CategoryMapper 填充） */
  private String categoryName;

  /** 预算月份（YYYY-MM） */
  private String month;

  /** 预警级别: NORMAL / DAILY_WARN / MONTHLY_WARN / OVERSPENT */
  private String alertLevel;

  /** 预算金额 */
  private BigDecimal budgetAmount;

  /** 已消耗金额 */
  private BigDecimal spentAmount;

  /** 消耗百分比 */
  private BigDecimal percentage;
}
