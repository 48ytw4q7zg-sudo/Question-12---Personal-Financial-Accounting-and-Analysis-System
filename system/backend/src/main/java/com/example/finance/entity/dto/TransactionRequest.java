package com.example.finance.entity.dto; // 声明该类属于 entity.dto 包（数据传输对象层，接收前端 TransactionListPage.vue「记一笔」弹窗 JSON → TransactionController 入参）

import com.fasterxml.jackson.annotation.JsonFormat; // Jackson 日期格式化注解：控制 LocalDateTime 序列化/反序列化的字符串格式（前端"yyyy-MM-dd HH:mm:ss" ↔ 后端 LocalDateTime）
import jakarta.validation.constraints.DecimalMin; // Jakarta Bean Validation: 数值最小值校验（BigDecimal 精度安全，交易金额必须 > 0）
import jakarta.validation.constraints.Max; // Jakarta Bean Validation: 整数最大值校验（交易类型：1=收入 2=支出，上限为 2）
import jakarta.validation.constraints.Min; // Jakarta Bean Validation: 整数最小值校验（交易类型下限为 1 + 账户/分类 ID 必须为正整数 > 0）
import jakarta.validation.constraints.NotNull; // Jakarta Bean Validation: 对象非 null 校验（账户/分类/金额/类型/时间均为必填字段）
import jakarta.validation.constraints.Size; // Jakarta Bean Validation: 字符串长度范围校验（备注 ≤ 200 字符）
import lombok.Data; // Lombok: 自动生成 getter/setter/toString/equals/hashCode（减少样板代码）

import java.math.BigDecimal; // Java 精确十进制类型（amount 金额字段，禁止 float/double 防精度丢失，对齐 DATABASE_DESIGN.md §3 #6）
import java.time.LocalDateTime; // JDK 8+ 日期时间类（线程安全不可变，映射 transaction 表 DATETIME 类型字段 time）

/**
 * 交易记录创建/更新请求体（前端 TransactionListPage.vue「记一笔」弹窗 → TransactionController.create()/update() → POST/PUT /api/transaction）
 *
 * <p>数据库对应：transaction 表（account_id / category_id / type / amount / note / time）。</p>
 *
 * <p>校验规则（@Valid 触发 Bean Validation）：</p>
 * <ul>
 *   <li>accountId：@NotNull + @Min(1) — 关联账户必须存在且 status=1</li>
 *   <li>categoryId：@NotNull + @Min(1) — 关联分类必须存在于 category 种子数据中</li>
 *   <li>type：@NotNull + @Min(1) + @Max(2) — 1=收入(TransactionType.INCOME) 2=支出(TransactionType.EXPENSE)</li>
 *   <li>amount：@NotNull + @DecimalMin("0.01") — 金额必须 > 0，DECIMAL(12,2) 精度</li>
 *   <li>note：@Size(max=200) — 备注可选，最多 200 字符</li>
 *   <li>time：@NotNull + @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss") — 交易时间必填</li>
 * </ul>
 *
 * <p>特殊约束（Service 层附加校验）：修改时如果记录是转账生成的（transferId 非 null），仅允许修改 note 备注字段，
 * accountId/categoryId/type/amount/time 字段不可修改（转账记录金额由系统生成，防止破坏借贷平衡）。</p>
 *
 * <p>跨文件引用：被 TransactionController.create() / TransactionController.update() 方法使用，
 * 经 TransactionServiceImpl → TransactionMapper 写入 transaction 表。</p>
 */
@Data // Lombok: 自动生成 getter/setter/toString/equals/hashCode
public class TransactionRequest {

  /** 关联账户 ID（对应 transaction 表 account_id 列，BIGINT NOT NULL，外键 FK → account.id，@NotNull + @Min(1) 拦截 0 和负数传入数据库） */
  @NotNull(message = "账户不能为空") // 校验：Long 不能为 null
  @Min(value = 1, message = "账户ID必须为正整数") // 校验：最小值为 1（account.id 自增主键从 1 开始）
  private Long accountId;

  /** 关联分类 ID（对应 transaction 表 category_id 列，BIGINT NOT NULL，外键 FK → category.id，@NotNull + @Min(1) 拦截 0 和负数传入数据库） */
  @NotNull(message = "分类不能为空") // 校验：Long 不能为 null
  @Min(value = 1, message = "分类ID必须为正整数") // 校验：最小值为 1（category.id 种子数据从 1 开始）
  private Long categoryId;

  // R-05-issue-2: 已修复 - type 注释改为与 Entity 一致"1=收入 2=支出"，消除 Request/Entity/DATABASE_DESIGN 三处语义不一致
  /**
   * 交易类型：1=收入（TransactionType.INCOME），2=支出（TransactionType.EXPENSE）（对应 transaction 表 type 列，TINYINT NOT NULL，@NotNull + @Min(1) + @Max(2) 枚举值校验）
   */
  @NotNull(message = "类型不能为空") // 校验：Integer 不能为 null
  @Min(value = 1, message = "类型须为1(收入)或2(支出)") // 校验：最小值为 1（1=收入）
  @Max(value = 2, message = "类型须为1(收入)或2(支出)") // 校验：最大值为 2（2=支出）
  private Integer type;

  /** 交易金额（对应 transaction 表 amount 列，DECIMAL(12,2) NOT NULL，@NotNull + @DecimalMin("0.01") 必须 > 0 且用 BigDecimal 防浮点精度丢失） */
  @NotNull(message = "金额不能为空") // 校验：BigDecimal 不能为 null
  @DecimalMin(value = "0.01", message = "金额必须大于0") // 校验：金额 >= 0.01（BigDecimal 精确比较，禁止 0 元交易）
  private BigDecimal amount;

  /** 备注（对应 transaction 表 note 列，VARCHAR(200) DEFAULT NULL，@Size(max=200) 限制 200 字符以内，可选填） */
  @Size(max = 200, message = "备注长度不能超过200") // 校验：字符串长度 <= 200（对应数据库 VARCHAR(200)）
  private String note;

  /** 交易时间（对应 transaction 表 time 列，DATETIME NOT NULL，@NotNull + @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss") 控制前端传参格式） */
  @NotNull(message = "交易时间不能为空") // 校验：LocalDateTime 不能为 null
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") // Jackson 注解：反序列化时按此格式解析前端传来的日期字符串，序列化时也按此格式输出
  private LocalDateTime time;
}
