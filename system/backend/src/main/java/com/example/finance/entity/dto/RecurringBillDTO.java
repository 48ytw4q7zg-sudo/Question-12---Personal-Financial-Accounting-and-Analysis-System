// 数据传输对象包（DTO 层，用于接收前端请求和返回响应）
package com.example.finance.entity.dto;

// Lombok 自动生成 getter/setter/toString/equals/hashCode
import lombok.Data;

// Java 高精度金额类型（禁止 float/double 防精度丢失）
import java.math.BigDecimal;
// Java 8 日期时间类型（CLAUDE.md §二·二 强制：时间字段一律用 LocalDateTime）
import java.time.LocalDateTime;

/**
 * 周期性账单数据传输对象 DTO（前端 RecurringBillPage.vue 列表 / 弹窗展示）
 *
 * accountName/categoryName 由 Service 层批量加载填充（避免 N+1）
 * 活跃账单（status=1）关联账户被禁用时标记为异常
 *
 * 对应数据库表: recurring_bill (id/name/account_id/category_id/amount/type/period/next_due_date/status/create_time/update_time)
 * 调用方: controller/RecurringBillController.java → service/impl/RecurringBillServiceImpl.java
 */
// Lombok: 自动生成 getter/setter/toString/equals/hashCode（Lombok 1.18.46）
@Data
// 周期账单信息 DTO 类（Service 层查询后组装返回，Controller 用 Result<List<RecurringBillDTO>> 包装）
public class RecurringBillDTO {

  /** 账单主键 ID（对应 recurring_bill.id 自增主键） */
  private Long id;

  /** 账单名称（1-30 字符，如「月房租」「月工资」，对应 recurring_bill.name VARCHAR(30)） */
  private String name;

  /** 关联账户 ID（FK → account.id，对应 recurring_bill.account_id 字段） */
  private Long accountId;

  /** 关联账户名称（批量加载填充，来自 account 表的 name 字段，前端列表展示用） */
  private String accountName;

  /** 关联分类 ID（FK → category.id，对应 recurring_bill.category_id 字段） */
  private Long categoryId;

  /** 关联分类名称（批量加载填充，来自 category 表的 name 字段） */
  private String categoryName;

  /** 金额（对应 recurring_bill.amount DECIMAL(12,2)） */
  private BigDecimal amount;

  /** 类型：1=收入 2=支出（对应 recurring_bill.type TINYINT(1)，对齐 TransactionType 枚举 · 前端 el-tag 颜色区分） */
  private Integer type;

  /** 周期（daily=每日/weekly=每周/monthly=每月/yearly=每年，对应 recurring_bill.period VARCHAR(10)） */
  private String period;

  /** 下次到期日（yyyy-MM-dd 格式，对应 recurring_bill.next_due_date DATE，@Scheduled 定时任务判断依据） */
  private String nextDueDate;

  /** 状态：1=活跃 0=停用（对应 recurring_bill.status TINYINT(1)，停用后不可恢复） */
  private Integer status;

  /** 关联账户是否已禁用（true=账户已禁用，账单标记为异常 · PRD P1-4 业务规则③ · 由 Service 层 JOIN account 表填充） */
  private Boolean accountDisabled;

  /** 创建时间（对应 recurring_bill.create_time DATETIME） */
  private LocalDateTime createTime;

  /** 最后更新时间（对应 recurring_bill.update_time DATETIME，每次编辑自动更新） */
  private LocalDateTime updateTime;
}
