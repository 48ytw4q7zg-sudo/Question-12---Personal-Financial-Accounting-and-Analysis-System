package com.example.finance.entity.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 周期性账单数据传输对象
 */
@Data
public class RecurringBillDTO {

  private Long id;
  private String name;
  private Long accountId;
  private String accountName;
  private Long categoryId;
  private String categoryName;
  private BigDecimal amount;
  private Integer type;
  private String period;
  private String nextDueDate;
  private Integer status;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
