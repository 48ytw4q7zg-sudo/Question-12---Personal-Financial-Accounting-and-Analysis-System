package com.example.finance.entity.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 账户余额数据传输对象
 */
@Data
public class AccountBalanceDTO {

  private Long accountId;
  private String accountName;
  private Integer accountType;
  private BigDecimal initialBalance;
  private BigDecimal totalIncome;
  private BigDecimal totalExpense;
  private BigDecimal currentBalance;
}
