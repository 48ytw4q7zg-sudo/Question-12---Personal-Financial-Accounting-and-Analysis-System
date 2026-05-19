package com.example.finance.entity.dto;

import lombok.Data;

/**
 * 分类数据传输对象（前端 CategoryPage.vue / TransactionListPage / BudgetPage / RecurringBillPage 分类下拉）
 *
 * 种子数据（13 条：支出 8 + 收入 5），所有用户共享，不支持增改删
 * 被 5 个前端页面/组件复用
 */
@Data
public class CategoryDTO {

  /** 分类主键 ID（种子数据预置，1-13） */
  private Long id;

  /** 分类名称（1-10 字符，如：餐饮/交通/工资/奖金） */
  private String name;

  /** 分类类型：1=支出 2=收入 */
  private Integer type;
}
