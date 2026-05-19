package com.example.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户实体（对应 account 表，PRD P0-2 账户管理）
 *
 * 多账户管理（现金/银行卡/支付宝/微信），软删除通过 status=0 实现
 * 用户只能操作自己的账户（通过 userId 隔离）
 */
@Data
@TableName("account")
public class Account {

  /** 账户主键 ID（BIGINT AUTO_INCREMENT） */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 所属用户 ID（FK → user.id，数据隔离） */
  @TableField("user_id")
  private Long userId;

  /** 账户名称（1-20 字符，同名允许，如两个「现金」账户） */
  @TableField("name")
  private String name;

  /**
   * 账户类型：1=现金 2=银行卡 3=支付宝 4=微信
   */
  @TableField("type")
  private Integer type;

  /** 初始余额（DECIMAL(12,2)，精度保留 2 位） */
  @TableField("initial_balance")
  private BigDecimal initialBalance;

  /** 币种代码：CNY/USD/EUR/JPY/GBP/HKD，默认 CNY */
  @TableField("currency")
  private String currency;

  /**
   * 状态：1=正常 0=已删除（软删除，不可恢复）
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
