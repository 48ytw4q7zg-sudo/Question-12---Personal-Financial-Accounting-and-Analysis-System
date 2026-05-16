package com.example.finance.entity.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户数据传输对象
 */
@Data
public class AccountDTO {

  private Long id;
  private String name;
  private Integer type;
  private BigDecimal initialBalance;
  private String currency;
  private Integer status;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
