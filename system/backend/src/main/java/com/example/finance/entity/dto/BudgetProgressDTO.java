// 数据传输对象包（DTO 层，用于接收前端请求和返回响应）
package com.example.finance.entity.dto;

// Lombok 自动生成 getter/setter/toString/equals/hashCode
import lombok.Data;

// Java 高精度金额类型（禁止 float/double 防精度丢失）
import java.math.BigDecimal;

/**
 * 预算进度数据传输对象 DTO（前端 BudgetPage.vue 进度条数据源）
 *
 * percentage = spentAmount / budgetAmount × 100%
 * overspent = spentAmount > budgetAmount（前端进度条变红色）
 * 进度条颜色：<80% 绿色 / 80-100% 橙色 / >100% 红色
 *
 * 数据来源: Service 层实时计算（SQL SUM 该分类当月支出 / 该分类当月预算）
 * 调用方: controller/BudgetController.java → service/impl/BudgetServiceImpl.java
 */
// Lombok: 自动生成 getter/setter/toString/equals/hashCode（Lombok 1.18.46）
@Data
// 预算进度 DTO 类（Controller 用 Result<List<BudgetProgressDTO>> 返回前端进度条组件）
public class BudgetProgressDTO {

  /** 分类 ID（FK → category.id，来源 budget 表的 category_id 字段） */
  private Long categoryId;

  /** 分类名称（批量加载填充，来自 category 表的 name 字段） */
  private String categoryName;

  /** 预算金额（用户设置的月度预算上限，对应 budget.amount 字段 DECIMAL(12,2)） */
  private BigDecimal budgetAmount;

  /** 已支出金额（该分类当月实际支出合计，SQL SUM(transaction.amount) WHERE type=2） */
  private BigDecimal spentAmount;

  /** 消耗百分比（spentAmount ÷ budgetAmount × 100，保留精度，前端 progress 组件的 percentage 属性） */
  private BigDecimal percentage;

  /** 是否超支（true = 已支出 > 预算金额，前端进度条变红色 + el-tag 类型切换） */
  private boolean overspent;
}
