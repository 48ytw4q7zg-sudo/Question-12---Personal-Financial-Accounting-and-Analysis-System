package com.example.finance.entity.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 分类汇总数据传输对象（前端 DashboardPage.vue 饼图 + AnalyticsPage.vue 分类饼图数据源）
 *
 * 按 category_id 分组聚合：SUM(amount) + COUNT(*)
 * 排除转账记录（transfer_id IS NULL），可按 type 筛选（1=收入 2=支出 null=全部）
 * 无消费记录的分类金额显示为 0（业务兜底）
 */
@Data
public class CategorySummaryDTO {

  /** 分类 ID */
  private Long categoryId;

  /** 分类名称（由 SQL JOIN category 表填充） */
  private String categoryName;

  /** 分类类型（由 SQL JOIN 带回，1=支出 2=收入，前端饼图颜色区分用） */
  private Integer type;

  /** 金额合计（SUM(amount)，DECIMAL(12,2)） */
  private BigDecimal totalAmount;

  /** 交易笔数（COUNT(*)） */
  private Long transactionCount;
}
