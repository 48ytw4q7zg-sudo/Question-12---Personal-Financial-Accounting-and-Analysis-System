package com.example.finance.entity.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 月度趋势数据传输对象
 */
@Data
public class MonthlyTrendDTO {

  private String month;
  private BigDecimal totalIncome;
  private BigDecimal totalExpense;
}
