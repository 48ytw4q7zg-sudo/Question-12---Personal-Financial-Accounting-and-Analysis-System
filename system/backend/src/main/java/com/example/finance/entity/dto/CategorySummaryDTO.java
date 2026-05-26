package com.example.finance.entity.dto; // 声明该类属于 entity.dto 包（数据传输对象层，StatisticsServiceImpl 聚合查询 → 前端 DashboardPage.vue / AnalyticsPage.vue 图表数据 JSON 响应）

import lombok.Data; // Lombok: 自动生成 getter/setter/toString/equals/hashCode（减少样板代码）

import java.math.BigDecimal; // Java 精确十进制类型（totalAmount 金额字段，禁止 float/double 防精度丢失）

/**
 * 分类汇总数据传输对象（StatisticsServiceImpl 按分类聚合 → 前端 DashboardPage.vue 饼图 + AnalyticsPage.vue 分类饼图数据源）
 *
 * <p>数据库来源：</p>
 * <ul>
 *   <li>transaction 表聚合：SUM(amount) AS totalAmount / COUNT(*) AS transactionCount / GROUP BY category_id</li>
 *   <li>category 表 JOIN：categoryName（SELECT c.name FROM category c WHERE c.id = t.category_id）</li>
 *   <li>category 表 JOIN：type（SELECT c.type FROM category c WHERE c.id = t.category_id）</li>
 * </ul>
 *
 * <p>查询逻辑：按 category_id 分组聚合 SUM(amount) + COUNT(*)，排除转账记录（transfer_id IS NULL 过滤），
 * 可按 type 筛选（1=收入 2=支出 null=全部），无消费记录的分类金额显示为 0。00（业务兜底）。</p>
 *
 * <p>跨文件引用：StatisticsServiceImpl.getCategorySummary() → TransactionMapper.selectCategorySummary() → List&lt;CategorySummaryDTO&gt;，
 * 前端 DashboardPage.vue（ECharts 饼图）和 AnalyticsPage.vue（ECharts 分类饼图）消费此 DTO。</p>
 */
@Data // Lombok: 自动生成 getter/setter/toString/equals/hashCode
public class CategorySummaryDTO {

  /** 分类 ID（对应 category 表 id 列，GROUP BY 聚合维度的键） */
  private Long categoryId;

  /** 分类名称（由 SQL JOIN category 表填充 c.name，如「餐饮」「交通」，前端饼图标签用） */
  private String categoryName;

  /** 分类类型（由 SQL JOIN category 表填充 c.type，1=支出 2=收入，前端饼图按类型区分颜色 — 支出红色系、收入绿色系） */
  private Integer type;

  /** 金额合计（SUM(amount)，DECIMAL(12,2)，该分类下所有交易记录（排除转账）的金额总和） */
  private BigDecimal totalAmount;

  /** 交易笔数（COUNT(*)，该分类下所有交易记录（排除转账）的记录数，前端可展示「共 N 笔」） */
  private Long transactionCount;
}
