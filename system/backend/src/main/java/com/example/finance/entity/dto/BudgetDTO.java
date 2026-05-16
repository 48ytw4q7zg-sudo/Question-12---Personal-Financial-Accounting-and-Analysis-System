package com.example.finance.entity.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预算数据传输对象
 */
@Data
public class BudgetDTO {

  private Long id;
  private Long categoryId;
  private String categoryName;
  private String month;
  private BigDecimal amount;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
