package com.example.finance.entity.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 周期性账单数据传输对象（前端 RecurringBillPage.vue 列表 / 弹窗展示）
 *
 * accountName/categoryName 由 Service 层批量加载填充（避免 N+1）
 * 活跃账单（status=1）关联账户被禁用时标记为异常
 */
@Data
public class RecurringBillDTO {

  /** 账单主键 ID */
  private Long id;

  /** 账单名称（1-30 字符，如「月房租」「月工资」） */
  private String name;

  /** 关联账户 ID（FK → account.id） */
  private Long accountId;

  /** 关联账户名称（批量加载填充，前端列表展示用） */
  private String accountName;

  /** 关联分类 ID（FK → category.id） */
  private Long categoryId;

  /** 关联分类名称（批量加载填充） */
  private String categoryName;

  /** 金额（DECIMAL(12,2)） */
  private BigDecimal amount;

  /** 类型：1=收入 2=支出（对齐 TransactionType 枚举 · 前端 tag 颜色区分） */
  private Integer type;

  /** 周期：daily/weekly/monthly/yearly */
  private String period;

  /** 下次到期日（yyyy-MM-dd 格式字符串） */
  private String nextDueDate;

  /** 状态：1=活跃 0=停用（停用后不可恢复） */
  private Integer status;

  /** 关联账户是否已禁用（true=账户已禁用, 账单标记为异常 · PRD P1-4 业务规则③） */
  private Boolean accountDisabled;

  /** 创建时间 */
  private LocalDateTime createTime;

  /** 最后更新时间 */
  private LocalDateTime updateTime;
}
