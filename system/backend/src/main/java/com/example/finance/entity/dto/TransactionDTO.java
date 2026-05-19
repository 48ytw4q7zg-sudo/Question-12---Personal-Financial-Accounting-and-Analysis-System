package com.example.finance.entity.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易记录数据传输对象（列表查询/详情返回）
 *
 * 列表查询时 accountName/categoryName 由 XML JOIN 填充（避免 N+1）
 * 单条查询时由 toDTO() 中单独查库填充
 * 被前端 TransactionListPage.vue / TransferPage.vue / ImportPage.vue 消费
 */
@Data
public class TransactionDTO {

  /** 交易记录主键 ID */
  private Long id;

  /** 关联账户 ID（FK → account.id） */
  private Long accountId;

  /** 关联账户名称（JOIN 填充，前端展示用） */
  private String accountName;

  /** 关联分类 ID（FK → category.id） */
  private Long categoryId;

  /** 关联分类名称（JOIN 填充，前端展示用） */
  private String categoryName;

  /** 交易类型：1=收入 2=支出 */
  private Integer type;

  /** 交易金额（DECIMAL(12,2)，必须 > 0） */
  private BigDecimal amount;

  /** 备注（≤200 字符，可选） */
  private String note;

  /** 交易时间（yyyy-MM-dd HH:mm:ss 格式字符串） */
  private String time;

  /** 转账关联 UUID：NULL=普通收支记录，非 NULL=转账关联记录（流水列表标记转出/转入） */
  private String transferId;

  /** 创建时间 */
  private LocalDateTime createTime;

  /** 最后更新时间 */
  private LocalDateTime updateTime;
}
