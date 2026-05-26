// 数据传输对象包（DTO 层，用于接收前端请求和返回响应）
package com.example.finance.entity.dto;

// Lombok 自动生成 getter/setter/toString/equals/hashCode
import lombok.Data;

// Java 高精度金额类型（禁止 float/double 防精度丢失）
import java.math.BigDecimal;

/**
 * 预算预警 DTO（P2-2 预算预警功能）
 *
 * 对应 budget_alert 表（id/category_id/month/alert_level/budget_amount/spent_amount/percentage）
 * 由 BudgetScheduler 定时任务每日自动写入，前端通过 GET /api/budget/alert 查询当前用户预警列表
 * 预警级别枚举：NORMAL=正常 / DAILY_WARN=日均超支 / MONTHLY_WARN=月度告警 / OVERSPENT=已超支
 *
 * 调用方: controller/BudgetController.java → service/impl/BudgetServiceImpl.java
 */
// Lombok: 自动生成 getter/setter/toString/equals/hashCode（Lombok 1.18.46）
@Data
// 预算预警 DTO 类（定时任务写入 budget_alert 表后，Controller 查询返回前端）
public class BudgetAlertDTO {

  /** 预警记录主键 ID（对应 budget_alert.id 自增主键） */
  private Long id;

  /** 关联分类 ID（FK → category.id，预警针对的具体分类） */
  private Long categoryId;

  /** 分类名称（由 CategoryMapper 批量填充，对应 category.name 字段） */
  private String categoryName;

  /** 预算月份（YYYY-MM 格式，如 2026-05，对应 budget_alert.month VARCHAR(7)） */
  private String month;

  /** 预警级别（NORMAL=正常 / DAILY_WARN=日均超支 / MONTHLY_WARN=月度告警 / OVERSPENT=已超支） */
  private String alertLevel;

  /** 预算金额（该分类当月预算上限，对应 budget_alert.budget_amount DECIMAL(12,2)） */
  private BigDecimal budgetAmount;

  /** 已消耗金额（该分类当月实际支出合计，对应 budget_alert.spent_amount DECIMAL(12,2)） */
  private BigDecimal spentAmount;

  /** 消耗百分比（spentAmount ÷ budgetAmount × 100，对应 budget_alert.percentage DECIMAL(5,2)） */
  private BigDecimal percentage;
}
