package com.example.finance.entity.dto;

import lombok.Data;

/**
 * 转账数据传输对象
 */
@Data
public class TransferDTO {

  private String transferId;
  private TransactionDTO outRecord;
  private TransactionDTO inRecord;
}
