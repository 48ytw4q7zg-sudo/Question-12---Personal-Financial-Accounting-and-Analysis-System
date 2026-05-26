package com.example.finance.entity.dto; // 声明该类属于 entity.dto 包（数据传输对象层，接收前端 AccountPage.vue 创建/编辑账户弹窗 JSON → AccountController 入参）

import jakarta.validation.constraints.DecimalMin; // Jakarta Bean Validation: 数值最小值校验（BigDecimal 精度安全，校验初始余额不能为负数）
import jakarta.validation.constraints.Max; // Jakarta Bean Validation: 整数最大值校验（限制账户类型枚举范围上限）
import jakarta.validation.constraints.Min; // Jakarta Bean Validation: 整数最小值校验（限制账户类型枚举范围下限）
import jakarta.validation.constraints.NotBlank; // Jakarta Bean Validation: 字符串非空校验（含 trim，账户名必填）
import jakarta.validation.constraints.NotNull; // Jakarta Bean Validation: 对象非 null 校验（账户类型/初始余额必填，区别于 @NotBlank 仅限字符串）
import jakarta.validation.constraints.Pattern; // Jakarta Bean Validation: 正则表达式校验（币种只能是 7 种 ISO 4217 代码之一）
import jakarta.validation.constraints.Size; // Jakarta Bean Validation: 字符串长度范围校验[min,max]（账户名 1-20 字符）
import lombok.Data; // Lombok: 自动生成 getter/setter/toString/equals/hashCode（减少样板代码）

import java.math.BigDecimal; // Java 精确十进制类型（用于金额字段，禁止 float/double 防精度丢失，对齐 DATABASE_DESIGN.md §3 #6 字段约定）

/**
 * 账户创建/更新请求体（前端 AccountPage.vue 弹窗 → AccountController.create()/update() → POST/PUT /api/account）
 *
 * <p>数据库对应：account 表（name / type / initial_balance / currency）。</p>
 * <p>校验规则：名称 1-20 字符（@NotBlank + @Size），类型 1-4（@Min + @Max），初始余额 >= 0（@DecimalMin）。</p>
 * <p>币种默认 CNY，可选 USD/EUR/JPY/GBP/HKD/KRW（@Pattern 枚举校验）。</p>
 *
 * <p>跨文件引用：被 AccountController.create() / AccountController.update() 方法使用，
 * 经 AccountServiceImpl → AccountMapper 写入 account 表。</p>
 */
@Data // Lombok: 自动生成 getter/setter/toString/equals/hashCode
public class AccountRequest {

  /** 账户名称（对应 account 表 name 列，VARCHAR(20) NOT NULL，@NotBlank 禁止空字符串 + @Size 限制 1-20 字符） */
  @NotBlank(message = "账户名不能为空") // 校验：字符串不能为 null 且 trim 后不能为空字符串
  @Size(min = 1, max = 20, message = "账户名长度须在1-20之间") // 校验：字符数在 [1, 20] 范围内
  private String name;

  /** 账户类型：1=现金 2=银行卡 3=支付宝 4=微信（对应 account 表 type 列，TINYINT NOT NULL，@NotNull + @Min + @Max 枚举范围校验） */
  @NotNull(message = "账户类型不能为空") // 校验：Integer 不能为 null
  @Min(value = 1, message = "账户类型须在1-4之间") // 校验：最小值为 1（1=现金）
  @Max(value = 4, message = "账户类型须在1-4之间") // 校验：最大值为 4（4=微信）
  private Integer type;

  /** 初始余额（对应 account 表 initial_balance 列，DECIMAL(12,2) NOT NULL DEFAULT 0.00，@NotNull + @DecimalMin("0.00") 防负数） */
  @NotNull(message = "初始余额不能为空") // 校验：BigDecimal 不能为 null
  @DecimalMin(value = "0.00", message = "初始余额不能为负数") // 校验：金额 >= 0.00（BigDecimal 精确比较，防精度丢失）
  private BigDecimal initialBalance;

  /**
   * 币种代码（对应 account 表 currency 列，VARCHAR(3) DEFAULT 'CNY'，@Pattern 限制 7 种 ISO 4217 币种）
   *
   * <p>支持：CNY(人民币) / USD(美元) / EUR(欧元) / JPY(日元) / GBP(英镑) / HKD(港币) / KRW(韩元)。</p>
   * <p>默认 CNY，前端下拉选择器仅展示这 7 项。</p>
   */
  @Pattern(regexp = "^(CNY|USD|EUR|JPY|GBP|HKD|KRW)$", message = "币种只能是CNY/USD/EUR/JPY/GBP/HKD/KRW") // 校验：严格匹配 7 种币种代码之一
  private String currency;
}
