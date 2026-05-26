// 数据传输对象包（DTO 层，用于接收前端请求和返回响应）
package com.example.finance.entity.dto;

// Lombok 自动生成 getter/setter/toString/equals/hashCode
import lombok.Data;

/**
 * 转账结果传输对象 DTO（前端 TransferPage.vue 转账成功后的响应）
 *
 * 转账生成两条关联记录：转出账户 type=2（支出）+ 转入账户 type=1（收入）
 * transferId 为 UUID，两条记录共享同一 transferId 标识关联关系
 * 转账记录禁止修改金额（仅可修改备注）
 *
 * 调用方: controller/TransactionController.java → service/impl/TransactionServiceImpl.java
 */
// Lombok: 自动生成 getter/setter/toString/equals/hashCode（Lombok 1.18.46）
@Data
// 转账结果 DTO 类（Controller 通过 Result<TransferDTO> 返回前端）
public class TransferDTO {

  /** 转账关联 UUID（两条记录共享，流水列表通过此字段标记转出/转入） */
  private String transferId;

  /** 转出记录（type=2 支出，category=13 其他，note 含 from→to 标记） */
  private TransactionDTO outRecord;

  /** 转入记录（type=1 收入，category=13 其他，note 含 from→to 标记） */
  private TransactionDTO inRecord;
}
