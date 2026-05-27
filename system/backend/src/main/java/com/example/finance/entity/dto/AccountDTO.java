package com.example.finance.entity.dto; // 声明该类属于 entity.dto 包（数据传输对象层，AccountController 列表查询 → 前端 AccountPage.vue / TransferPage.vue / ImportPage.vue JSON 响应）

import lombok.Data; // Lombok: 自动生成 getter/setter/toString/equals/hashCode（减少样板代码）

import java.math.BigDecimal; // Java 精确十进制类型（金额字段 initialBalance，禁止 float/double 防精度丢失）
import java.time.LocalDateTime; // JDK 8+ 日期时间类（线程安全不可变，映射数据库 account 表 DATETIME 类型字段 create_time / update_time）

/**
 * 账户数据传输对象（Controller → 前端响应，account 表全字段查询结果 → DTO 转换）
 *
 * <p>数据库来源：account 表全部列（id / name / type / initial_balance / currency / status / create_time / update_time）。</p>
 * <p>数据转换：AccountServiceImpl.toDTO() 方法做 Entity→DTO 字段拷贝，不含 user_id（userId 由 JWT 从 token 中提取，不暴露给前端）。</p>
 *
 * <p>跨文件引用：</p>
 * <ul>
 *   <li>AccountController.list() → AccountServiceImpl.listByUserId() → 返回 List&lt;AccountDTO&gt;</li>
 *   <li>前端 AccountPage.vue 账户列表渲染此 DTO</li>
 *   <li>前端 TransferPage.vue 转出/转入账户下拉选择器消费此 DTO</li>
 *   <li>前端 ImportPage.vue 导入数据选择目标账户消费此 DTO</li>
 * </ul>
 */
@Data // Lombok: 自动生成 getter/setter/toString/equals/hashCode
public class AccountDTO {

  /** 账户主键 ID（对应 account 表 id 列，BIGINT AUTO_INCREMENT 自增主键，@TableId(IdType.AUTO)） */
  private Long id;

  /** 账户名称（对应 account 表 name 列，VARCHAR(20) NOT NULL，1-20 字符，如同名可重复） */
  private String name;

  /** 账户类型：1=现金 2=银行卡 3=支付宝 4=微信（对应 account 表 type 列，TINYINT NOT NULL，枚举值由前端 AccountPage.vue 映射为中文标签） */
  private Integer type;

  /** 初始余额（对应 account 表 initial_balance 列，DECIMAL(12,2) NOT NULL DEFAULT 0.00，精度保留 2 位小数） */
  private BigDecimal initialBalance;

  /** 币种代码（对应 account 表 currency 列，VARCHAR(3) DEFAULT 'CNY'，ISO 4217 三位字母码：CNY/USD/EUR/JPY/GBP/HKD/KRW） */
  private String currency;

  /** 账户状态：1=启用（status=1），0=停用（软删除后不可恢复）（对应 account 表 status 列，TINYINT NOT NULL DEFAULT 1） */
  private Integer status;

  /** 创建时间（对应 account 表 create_time 列，DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP，Jackson 序列化为 ISO 8601 格式） */
  private LocalDateTime createTime;

  /** 最后更新时间（对应 account 表 update_time 列，DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP，每次修改自动更新） */
  private LocalDateTime updateTime;
}
