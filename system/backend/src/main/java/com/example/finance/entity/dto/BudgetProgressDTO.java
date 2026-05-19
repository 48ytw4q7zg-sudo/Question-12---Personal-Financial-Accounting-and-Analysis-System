package com.example.finance.entity.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 预算进度数据传输对象（前端 BudgetPage.vue 进度条数据源）
 *
 * percentage = spentAmount / budgetAmount × 100%
 * overspent = spentAmount > budgetAmount（前端进度条变红色）
 * 进度条颜色：<80% 绿色 / 80-100% 橙色 / >100% 红色
 */
@Data
public class BudgetProgressDTO {

  /** 分类 ID */
  private Long categoryId;

  /** 分类名称（批量加载填充） */
  private String categoryName;

  /** 预算金额（用户设置的月度预算上限） */
  private BigDecimal budgetAmount;

  /** 已支出金额（该分类当月实际支出合计） */
  private BigDecimal spentAmount;

  /** 消耗百分比（spentAmount ÷ budgetAmount × 100，保留精度） */
  private BigDecimal percentage;

  /** 是否超支（true = 已支出 > 预算金额） */
  private boolean overspent;
}
