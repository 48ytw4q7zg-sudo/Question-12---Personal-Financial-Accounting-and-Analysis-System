package com.example.finance.entity.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 分类汇总数据传输对象
 */
@Data
public class CategorySummaryDTO {

  private Long categoryId;
  private String categoryName;
  private Integer type;
  private BigDecimal totalAmount;
  private Long transactionCount;
}
