package com.example.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预算预警实体（对应 budget_alert 表 · P2-2）
 *
 * <p>由 BudgetScheduler 每日凌晨 2:00 定时写入，记录各分类预算的预警状态。</p>
 * <p>预警级别: NORMAL(正常) / DAILY_WARN(日预警) / MONTHLY_WARN(月预警) / OVERSPENT(已超支)。</p>
 */
@Data
@TableName("budget_alert")
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
