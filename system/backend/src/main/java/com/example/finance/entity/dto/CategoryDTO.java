package com.example.finance.entity.dto; // 声明该类属于 entity.dto 包（数据传输对象层，CategoryController.list() → 前端分类下拉选择器 JSON 响应）

import lombok.Data; // Lombok: 自动生成 getter/setter/toString/equals/hashCode（减少样板代码）

/**
 * 分类数据传输对象（CategoryController.list() → 前端 CategoryPage.vue / TransactionListPage / BudgetPage / RecurringBillPage 分类下拉选择器）
 *
 * <p>数据库来源：category 表（种子数据，13 条预设：支出 8 类 + 收入 5 类，所有用户共享）。</p>
 * <p>特性：种子数据预置，不支持前端增改删（只读接口，仅 GET /api/category/list）。</p>
 *
 * <p>跨文件引用：</p>
 * <ul>
 *   <li>CategoryController.list() → CategoryServiceImpl.listAll() → 返回 List&lt;CategoryDTO&gt;</li>
 *   <li>前端 CategoryPage.vue 分类列表页 消费此 DTO</li>
 *   <li>前端 TransactionListPage.vue「记一笔」弹窗 分类下拉 消费此 DTO</li>
 *   <li>前端 BudgetPage.vue 预算设置 分类下拉 消费此 DTO</li>
 *   <li>前端 RecurringBillPage.vue 账单设置 分类下拉 消费此 DTO</li>
 *   <li>前端 TransferPage.vue 转账分类筛选 消费此 DTO</li>
 * </ul>
 *
 * <p>共被 5 个前端页面/组件复用，是最广泛被引用的 DTO 之一。</p>
 */
@Data // Lombok: 自动生成 getter/setter/toString/equals/hashCode
public class CategoryDTO {

  /** 分类主键 ID（对应 category 表 id 列，BIGINT AUTO_INCREMENT，种子数据预置 id 值 1-13） */
  private Long id;

  /** 分类名称（对应 category 表 name 列，VARCHAR(10) NOT NULL UNIQUE，如：餐饮/交通/购物/娱乐/居住/通讯/医疗/教育/工资/奖金/兼职/理财/其他） */
  private String name;

  /** 分类类型：1=支出 2=收入（对应 category 表 type 列，TINYINT NOT NULL，前端按类型分组展示 — el-select 中用 optgroup 区分「支出分类」和「收入分类」） */
  private Integer type;
}
