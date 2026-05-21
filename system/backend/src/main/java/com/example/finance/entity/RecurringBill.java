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
 * 周期性账单实体（对应 recurring_bill 表，PRD P1-4 周期性账单提醒）
 *
 * 用户设置周期性收支模板（月房租/月工资等），手动一键生成交易记录
 * 停用后不可恢复（软删除 status=0）
 */
@Data
@TableName("recurring_bill")
public class RecurringBill {

  /** 账单主键 ID（BIGINT AUTO_INCREMENT） */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 所属用户 ID（FK → user.id，数据隔离） */
  @TableField("user_id")
  private Long userId;

  /** 关联账户 ID（FK → account.id，一键生成时写入该账户） */
  @TableField("account_id")
  private Long accountId;

  /** 关联分类 ID（FK → category.id） */
  @TableField("category_id")
  private Long categoryId;

  /** 账单名称（1-30 字符，如「月房租」「月工资」） */
  @TableField("name")
  private String name;

  /** 金额（DECIMAL(12,2)，必须 > 0） */
  @TableField("amount")
  private BigDecimal amount;

  /**
   * 类型：1=收入 2=支出（与 Transaction.type 语义一致，generate()直接透传）
   */
  @TableField("type")
  private Integer type;

  /**
   * 周期：daily=每日 weekly=每周 monthly=每月 yearly=每年
   */
  @TableField("period")
  private String period;

  /** 下次到期日（DATE，用于 @Scheduled 判断是否到期） */
  @TableField("next_due_date")
  private LocalDate nextDueDate;

  /**
   * 状态：1=启用 0=停用（停用后不可恢复）
   */
  @TableField("status")
  private Integer status;

  /** 创建时间 */
  @TableField("create_time")
  private LocalDateTime createTime;

  /** 最后更新时间 */
  @TableField("update_time")
  private LocalDateTime updateTime;
}
