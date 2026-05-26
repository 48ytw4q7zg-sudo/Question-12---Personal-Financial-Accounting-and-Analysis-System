package com.example.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;       // MP 主键策略（IdType.AUTO=数据库自增）
import com.baomidou.mybatisplus.annotation.TableField;    // MP 字段映射（驼峰↔下划线转换）
import com.baomidou.mybatisplus.annotation.TableId;       // MP 主键标识
import com.baomidou.mybatisplus.annotation.TableName;      // MP 表名映射
import lombok.Data;                                       // Lombok 自动生成 getter/setter/toString/equals/hashCode

import java.math.BigDecimal;                              // 金额字段精度类型（DECIMAL(12,2) 映射）
import java.time.LocalDateTime;                           // JDK 21 时间类型（对齐 DATETIME 数据库列）

/**
 * 交易记录实体（对应数据库 transaction 表，PRD P0-4 收支记录 + P1-5 转账）
 *
 * <p>普通收支：transfer_id = NULL，流水列表正常展示。</p>
 * <p>转账记录：transfer_id = UUID，成对出现（一出一进），仅允许修改备注（禁止修改金额/账户/分类）。</p>
 *
 * <p>关联文件：</p>
 * <ul>
 *   <li>被调用方: TransactionController.java / TransactionServiceImpl.java / TransferServiceImpl.java</li>
 *   <li>关联 DTO: TransactionRequest.java / TransactionDTO.java / TransferRequest.java / TransferDTO.java</li>
 *   <li>数据库 DDL: sql/01-init.sql CREATE TABLE transaction</li>
 *   <li>XML 映射: TransactionMapper.xml（批量聚合查询 accountExpenseBatch / accountIncomeBatch）</li>
 * </ul>
 */
@Data                                 // Lombok: 自动生成 getter/setter/toString/equals/hashCode
@TableName("transaction")             // 映射数据库 transaction 表
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
