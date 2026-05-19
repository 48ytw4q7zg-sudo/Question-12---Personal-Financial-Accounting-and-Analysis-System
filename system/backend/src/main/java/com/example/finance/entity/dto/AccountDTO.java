package com.example.finance.entity.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户数据传输对象（Controller → 前端响应）
 *
 * 数据来源：account 表查询结果 → AccountServiceImpl.toDTO() 转换
 * 被前端 AccountPage.vue / TransferPage.vue / ImportPage.vue 消费
 */
@Data
public class AccountDTO {

  /** 账户主键 ID（BIGINT AUTO_INCREMENT） */
  private Long id;

  /** 账户名称（1-20 字符，如同名可重复） */
  private String name;

  /** 账户类型：1=现金 2=银行卡 3=支付宝 4=微信（TINYINT） */
  private Integer type;

  /** 初始余额（DECIMAL(12,2)，精度保留 2 位） */
  private BigDecimal initialBalance;

  /** 币种代码：CNY/USD/EUR/JPY/GBP/HKD，默认 CNY */
  private String currency;

  /** 状态：1=启用（status=1） 0=停用（软删除后不可恢复） */
  private Integer status;

  /** 创建时间（yyyy-MM-dd HH:mm:ss） */
  private LocalDateTime createTime;

  /** 最后更新时间（每次修改自动更新） */
  private LocalDateTime updateTime;
}
