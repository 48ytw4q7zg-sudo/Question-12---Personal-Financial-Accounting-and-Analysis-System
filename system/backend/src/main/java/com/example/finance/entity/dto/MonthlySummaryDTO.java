// 数据传输对象包（DTO 层，用于接收前端请求和返回响应）
package com.example.finance.entity.dto;

// Lombok 自动生成 getter/setter/toString/equals/hashCode
import lombok.Data;

// Java 高精度金额类型（禁止 float/double 防精度丢失）
import java.math.BigDecimal;

/**
 * 月度/年度汇总数据传输对象 DTO（前端 DashboardPage.vue 统计卡片 + AnalyticsPage.vue 汇总表格）
 *
 * balance = totalIncome - totalExpense（由 Service 层计算，也可前端计算）
 * 年度汇总时 month 字段为 null
 * 所有金额排除 transfer_id IS NOT NULL 的转账记录，避免虚增（一笔转账两条记录，实际资金不变）
 *
 * 数据来源: SQL GROUP BY YEAR(time), MONTH(time) 聚合 transaction 表
 * 调用方: controller/StatisticsController.java → service/impl/StatisticsServiceImpl.java
 */
// Lombok: 自动生成 getter/setter/toString/equals/hashCode（Lombok 1.18.46）
@Data
// 月度汇总 DTO 类（Controller 用 Result<MonthlySummaryDTO> 或 Result<List<MonthlySummaryDTO>> 包装返回）
public class MonthlySummaryDTO {

  /** 年份（如 2026，对应 SQL YEAR(time) 聚合字段） */
  private Integer year;

  /** 月份（1-12，年度汇总时为 null 表示不按月分组，对应 SQL MONTH(time) 聚合字段） */
  private Integer month;

  /** 总收入（该期间内 type=1 且 transfer_id IS NULL 的 amount 合计，排除转账记录防虚增） */
  private BigDecimal totalIncome;

  /** 总支出（该期间内 type=2 且 transfer_id IS NULL 的 amount 合计，排除转账记录防虚增） */
  private BigDecimal totalExpense;

  /** 结余 = totalIncome - totalExpense（正数前端绿色 #67C23A / 负数前端红色 #F56C6C） */
  private BigDecimal balance;
}
