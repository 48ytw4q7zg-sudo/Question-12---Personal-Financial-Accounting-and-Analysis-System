package com.example.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预算实体（对应 budget 表，PRD P1-3 预算管理）
 *
 * 月预算按分类设置，同一用户+同一月+同一分类唯一约束
 * 仅支出分类可设置预算（收入分类不参与预算）
 */
@Data
@TableName("budget")
public class Budget {

  /** 预算主键 ID（BIGINT AUTO_INCREMENT） */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 所属用户 ID（FK → user.id，数据隔离） */
  @TableField("user_id")
  private Long userId;

  /** 关联分类 ID（FK → category.id，仅支出分类 type=1） */
  @TableField("category_id")
  private Long categoryId;

  /**
   * 预算月份，格式：yyyy-MM（如 2026-05）
   */
  @TableField("month")
  private String month;

  /** 预算金额（DECIMAL(12,2)，必须 > 0） */
  @TableField("amount")
  private BigDecimal amount;

  /** 创建时间 */
  @TableField("create_time")
  private LocalDateTime createTime;

  /** 最后更新时间（覆盖保存时更新） */
  @TableField("update_time")
  private LocalDateTime updateTime;
}
