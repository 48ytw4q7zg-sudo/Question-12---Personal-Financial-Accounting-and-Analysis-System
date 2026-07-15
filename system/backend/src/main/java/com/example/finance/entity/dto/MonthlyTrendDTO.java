// 数据传输对象包（DTO 层，用于接收前端请求和返回响应）
package com.example.finance.entity.dto;

// Lombok 自动生成 getter/setter/toString/equals/hashCode
import lombok.Data;

// Java 高精度金额类型（禁止 float/double 防精度丢失）
import java.math.BigDecimal;

/**
 * 月度趋势数据传输对象 DTO（前端 DashboardPage.vue 收支折线图 + AnalyticsPage.vue 趋势图）
 *
 * 按 DATE_FORMAT(time, '%Y-%m') 分组聚合，默认最近 12 个月
 * 排除转账记录（transfer_id IS NULL）避免虚增
 * ECharts X 轴 = month 数组，Y 轴 = totalIncome/totalExpense 两条折线
 *
 * 数据来源: SQL GROUP BY DATE_FORMAT(time, '%Y-%m') 聚合 transaction 表
 * 调用方: controller/StatisticsController.java → service/impl/StatisticsServiceImpl.java
 */
// Lombok: 自动生成 getter/setter/toString/equals/hashCode（Lombok 1.18.46）
@Data
// 月度趋势 DTO 类（Controller 用 Result<List<MonthlyTrendDTO>> 返回前端 ECharts 折线图数据源）
public class MonthlyTrendDTO {

  /** 月份标签（yyyy-MM 格式，如 2026-01，ECharts X 轴坐标，SQL DATE_FORMAT(time, '%Y-%m') 聚合结果） */
  private String month;

  /** 该月总收入（type=1 且 transfer_id IS NULL 的 amount 合计，ECharts 收入折线 Y 值） */
  private BigDecimal totalIncome;

  /** 该月总支出（type=2 且 transfer_id IS NULL 的 amount 合计，ECharts 支出折线 Y 值） */
  private BigDecimal totalExpense;
}
