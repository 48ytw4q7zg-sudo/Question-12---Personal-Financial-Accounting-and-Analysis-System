package com.example.finance.entity.dto;

import lombok.Data;

/**
 * 转账结果传输对象（前端 TransferPage.vue 转账成功后的响应）
 *
 * 转账生成两条关联记录：转出账户 type=2（支出）+ 转入账户 type=1（收入）
 * transferId 为 UUID，两条记录共享同一 transferId 标识关联关系
 * 转账记录禁止修改金额（仅可修改备注）
 */
@Data
public class TransferDTO {

  /** 转账关联 UUID（两条记录共享，流水列表通过此字段标记转出/转入） */
  private String transferId;

  /** 转出记录（type=2 支出，category=13 其他，note 含 from→to 标记） */
  private TransactionDTO outRecord;

  /** 转入记录（type=1 收入，category=13 其他，note 含 from→to 标记） */
  private TransactionDTO inRecord;
}
