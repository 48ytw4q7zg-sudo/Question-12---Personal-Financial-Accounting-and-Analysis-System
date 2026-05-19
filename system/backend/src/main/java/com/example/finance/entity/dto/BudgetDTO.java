package com.example.finance.entity.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预算数据传输对象（前端 BudgetPage.vue 列表展示）
 *
 * 预算按用户+分类+月份设置，仅支出分类可参与预算
 * categoryName 由 Service 层批量 JOIN category 表填充（避免 N+1）
 */
@Data
public class BudgetDTO {

  /** 预算主键 ID */
  private Long id;

  /** 关联分类 ID（FK → category.id，仅支出分类 type=1） */
  private Long categoryId;

  /** 关联分类名称（批量加载填充，前端进度条展示用） */
  private String categoryName;

  /** 预算月份（yyyy-MM 格式，如 2026-05） */
  private String month;

  /** 预算金额（DECIMAL(12,2)） */
  private BigDecimal amount;

  /** 创建时间 */
  private LocalDateTime createTime;

  /** 最后更新时间（覆盖保存时更新） */
  private LocalDateTime updateTime;
}
