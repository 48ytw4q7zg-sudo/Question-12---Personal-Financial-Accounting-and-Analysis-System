package com.example.finance.entity.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 预算进度数据传输对象
 */
@Data
public class BudgetProgressDTO {

  private Long categoryId;
  private String categoryName;
  private BigDecimal budgetAmount;
  private BigDecimal spentAmount;
  private BigDecimal percentage;
  private boolean overspent;
}
