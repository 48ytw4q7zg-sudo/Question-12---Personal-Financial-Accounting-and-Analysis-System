package com.example.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户实体
 */
@Data
@TableName("account")
public class Account {

  @TableId(type = IdType.AUTO)
  private Long id;

  @TableField("user_id")
  private Long userId;

  @TableField("name")
  private String name;

  /**
   * 账户类型：1=现金 2=银行卡 3=支付宝 4=微信
   */
  @TableField("type")
  private Integer type;

  @TableField("initial_balance")
  private BigDecimal initialBalance;

  @TableField("currency")
  private String currency;

  /**
   * 状态：1=正常 0=已删除
   */
  @TableField("status")
  private Integer status;

  @TableField("create_time")
  private LocalDateTime createTime;

  @TableField("update_time")
  private LocalDateTime updateTime;
}
