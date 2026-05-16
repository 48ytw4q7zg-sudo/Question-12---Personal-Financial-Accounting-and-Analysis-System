package com.example.finance.entity.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 月度/年度汇总数据传输对象
 */
@Data
public class MonthlySummaryDTO {

  private Integer year;
  private Integer month;
  private BigDecimal totalIncome;
  private BigDecimal totalExpense;
  private BigDecimal balance;
}
