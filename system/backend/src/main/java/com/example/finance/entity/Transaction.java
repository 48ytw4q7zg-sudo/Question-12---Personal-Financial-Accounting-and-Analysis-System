package com.example.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易记录实体
 */
@Data
@TableName("transaction")
public class Transaction {

  @TableId(type = IdType.AUTO)
  private Long id;

  @TableField("user_id")
  private Long userId;

  @TableField("account_id")
  private Long accountId;

  @TableField("category_id")
  private Long categoryId;

  /**
   * 类型：1=收入 2=支出
   */
  @TableField("type")
  private Integer type;

  @TableField("amount")
  private BigDecimal amount;

  @TableField("note")
  private String note;

  @TableField("time")
  private LocalDateTime time;

  /**
   * 转账关联ID（UUID；NULL=普通收支记录，非NULL=转账关联记录）
   */
  @TableField("transfer_id")
  private String transferId;

  @TableField("create_time")
  private LocalDateTime createTime;

  @TableField("update_time")
  private LocalDateTime updateTime;
}
