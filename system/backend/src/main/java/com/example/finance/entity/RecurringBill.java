package com.example.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 周期性账单实体
 */
@Data
@TableName("recurring_bill")
public class RecurringBill {

  @TableId(type = IdType.AUTO)
  private Long id;

  @TableField("user_id")
  private Long userId;

  @TableField("account_id")
  private Long accountId;

  @TableField("category_id")
  private Long categoryId;

  @TableField("name")
  private String name;

  @TableField("amount")
  private BigDecimal amount;

  /**
   * 类型：1=支出 2=收入
   */
  @TableField("type")
  private Integer type;

  /**
   * 周期：daily=每日 weekly=每周 monthly=每月 yearly=每年
   */
  @TableField("period")
  private String period;

  @TableField("next_due_date")
  private LocalDate nextDueDate;

  /**
   * 状态：1=启用 0=停用
   */
  @TableField("status")
  private Integer status;

  @TableField("create_time")
  private LocalDateTime createTime;

  @TableField("update_time")
  private LocalDateTime updateTime;
}
