package com.example.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;       // MP 主键策略（IdType.AUTO=数据库自增）
import com.baomidou.mybatisplus.annotation.TableField;    // MP 字段映射（驼峰↔下划线转换）
import com.baomidou.mybatisplus.annotation.TableId;       // MP 主键标识
import com.baomidou.mybatisplus.annotation.TableName;      // MP 表名映射
import lombok.Data;                                       // Lombok 自动生成 getter/setter/toString/equals/hashCode

import java.math.BigDecimal;                              // 金额字段精度类型（DECIMAL(12,2) 映射）
import java.time.LocalDateTime;                           // JDK 21 时间类型（对齐 DATETIME 数据库列）

/**
 * 预算预警实体（对应数据库 budget_alert 表，PRD P2-2 预算预警）
 *
 * <p>由 BudgetScheduler 每日凌晨 2:00 定时写入，记录各分类预算的预警状态。</p>
 * <p>预警级别枚举: NORMAL(正常) / DAILY_WARN(日预警) / MONTHLY_WARN(月预警) / OVERSPENT(已超支)。</p>
 *
 * <p>关联文件：</p>
 * <ul>
 *   <li>被调用方: BudgetController.java / BudgetServiceImpl.java / BudgetScheduler.java</li>
 *   <li>关联 DTO: BudgetAlertDTO.java</li>
 *   <li>数据库 DDL: sql/01-init.sql CREATE TABLE budget_alert</li>
 * </ul>
 */
@Data                                   // Lombok: 自动生成 getter/setter/toString/equals/hashCode
@TableName("budget_alert")              // 映射数据库 budget_alert 表
public class BudgetAlert {

  /** 预警记录主键 ID（BIGINT AUTO_INCREMENT） */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 所属用户ID */
  @TableField("user_id")
  private Long userId;

  /** 关联分类ID */
  @TableField("category_id")
  private Long categoryId;

  /** 预算月份（YYYY-MM） */
  @TableField("month")
  private String month;

  /** 预警级别: NORMAL / DAILY_WARN / MONTHLY_WARN / OVERSPENT */
  @TableField("alert_level")
  private String alertLevel;

  /** 预算金额 */
  @TableField("budget_amount")
  private BigDecimal budgetAmount;

  /** 已消耗金额 */
  @TableField("spent_amount")
  private BigDecimal spentAmount;

  /** 消耗百分比（0-100） */
  @TableField("percentage")
  private BigDecimal percentage;

  /** 预警生成时间 */
  @TableField("create_time")
  private LocalDateTime createTime;

  /** 更新时间 */
  @TableField("update_time")
  private LocalDateTime updateTime;
}
