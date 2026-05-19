package com.example.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易记录实体（对应 transaction 表，PRD P0-4 收支记录 + P1-5 转账）
 *
 * 普通收支：transfer_id = NULL，流水列表正常展示
 * 转账记录：transfer_id = UUID，成对出现（一出一进），仅允许修改备注
 */
@Data
@TableName("transaction")
public class Transaction {

  /** 记录主键 ID（BIGINT AUTO_INCREMENT） */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 所属用户 ID（FK → user.id，数据隔离） */
  @TableField("user_id")
  private Long userId;

  /** 关联账户 ID（FK → account.id） */
  @TableField("account_id")
  private Long accountId;

  /** 关联分类 ID（FK → category.id） */
  @TableField("category_id")
  private Long categoryId;

  /**
   * 类型：1=收入 2=支出
   */
  @TableField("type")
  private Integer type;

  /** 金额（DECIMAL(12,2)，必须 > 0） */
  @TableField("amount")
  private BigDecimal amount;

  /** 备注（≤200 字符，可选） */
  @TableField("note")
  private String note;

  /** 交易时间（DATETIME，yyyy-MM-dd HH:mm:ss） */
  @TableField("time")
  private LocalDateTime time;

  /**
   * 转账关联ID（UUID；NULL=普通收支记录，非NULL=转账关联记录）
   */
  @TableField("transfer_id")
  private String transferId;

  /** 创建时间 */
  @TableField("create_time")
  private LocalDateTime createTime;

  /** 最后更新时间 */
  @TableField("update_time")
  private LocalDateTime updateTime;
}
