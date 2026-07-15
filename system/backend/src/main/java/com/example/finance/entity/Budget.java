package com.example.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;       // MP 主键策略（IdType.AUTO=数据库自增）
import com.baomidou.mybatisplus.annotation.TableField;    // MP 字段映射（驼峰↔下划线转换）
import com.baomidou.mybatisplus.annotation.TableId;       // MP 主键标识
import com.baomidou.mybatisplus.annotation.TableName;      // MP 表名映射
import lombok.Data;                                       // Lombok 自动生成 getter/setter/toString/equals/hashCode

import java.math.BigDecimal;                              // 金额字段精度类型（DECIMAL(12,2) 映射）
import java.time.LocalDateTime;                           // JDK 21 时间类型（对齐 DATETIME 数据库列）

/**
 * 预算实体（对应数据库 budget 表，PRD P1-3 预算管理）
 *
 * <p>月预算按分类设置，同一用户+同一月+同一分类唯一约束（INSERT ON DUPLICATE KEY UPDATE）。</p>
 * <p>仅支出分类可设置预算（收入分类不参与预算）。</p>
 *
 * <p>关联文件：</p>
 * <ul>
 *   <li>被调用方: BudgetController.java / BudgetServiceImpl.java / BudgetScheduler.java</li>
 *   <li>关联 DTO: BudgetRequest.java / BudgetDTO.java / BudgetProgressDTO.java / BudgetAlertDTO.java</li>
 *   <li>数据库 DDL: sql/01-init.sql CREATE TABLE budget（含唯一索引 uk_user_category_month）</li>
 * </ul>
 */
@Data                                 // Lombok: 自动生成 getter/setter/toString/equals/hashCode
@TableName("budget")                  // 映射数据库 budget 表
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
