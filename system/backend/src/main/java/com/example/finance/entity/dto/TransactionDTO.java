package com.example.finance.entity.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易记录数据传输对象
 */
@Data
public class TransactionDTO {

  private Long id;
  private Long accountId;
  private String accountName;
  private Long categoryId;
  private String categoryName;
  private Integer type;
  private BigDecimal amount;
  private String note;
  private String time;
  private String transferId;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
