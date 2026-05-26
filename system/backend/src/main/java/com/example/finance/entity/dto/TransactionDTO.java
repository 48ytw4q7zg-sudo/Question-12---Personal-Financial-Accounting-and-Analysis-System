package com.example.finance.entity.dto; // 声明该类属于 entity.dto 包（数据传输对象层，TransactionController 列表查询/详情 → 前端 TransactionListPage.vue / TransferPage.vue / ImportPage.vue JSON 响应）

import lombok.Data; // Lombok: 自动生成 getter/setter/toString/equals/hashCode（减少样板代码）

import java.math.BigDecimal; // Java 精确十进制类型（amount 金额字段，禁止 float/double 防精度丢失）
import java.time.LocalDateTime; // JDK 8+ 日期时间类（线程安全不可变，映射 transaction 表 DATETIME 类型字段 create_time / update_time）

/**
 * 交易记录数据传输对象（TransactionController 列表查询/详情 → 前端 Table 行渲染）
 *
 * <p>数据库来源：transaction 表（id / account_id / category_id / type / amount / note / transfer_id / create_time / update_time）
 * + account 表 JOIN（accountName）+ category 表 JOIN（categoryName）。</p>
 *
 * <p>JOIN 策略：</p>
 * <ul>
 *   <li>列表查询时 accountName/categoryName 由 XML JOIN 填充（TransactionMapper.xml selectTransactionPage），避免 N+1 逐条查库</li>
 *   <li>单条查询时由 TransactionServiceImpl.toDTO() 中单独查库填充（AccountMapper.selectById + CategoryMapper.selectById）</li>
 * </ul>
 *
 * <p>time 字段为 String 类型（非 LocalDateTime）：直接透传数据库格式化的字符串 "yyyy-MM-dd HH:mm:ss"，
 * 避免 LocalDateTime → String 转换的不一致问题（列表查询用 DATE_FORMAT(time, '%Y-%m-%d %H:%i:%s')）。</p>
 *
 * <p>跨文件引用：</p>
 * <ul>
 *   <li>TransactionController.list() / getById() → TransactionServiceImpl → TransactionMapper → List&lt;TransactionDTO&gt;</li>
 *   <li>前端 TransactionListPage.vue（收支流水列表 el-table）消费此 DTO</li>
 *   <li>前端 TransferPage.vue（转账记录列表）消费此 DTO</li>
 *   <li>前端 ImportPage.vue（导入数据预览）消费此 DTO</li>
 *   <li>前端 DashboardPage.vue（最近交易）消费此 DTO</li>
 * </ul>
 */
@Data // Lombok: 自动生成 getter/setter/toString/equals/hashCode
public class TransactionDTO {

  /** 交易记录主键 ID（对应 transaction 表 id 列，BIGINT AUTO_INCREMENT 自增主键，@TableId(IdType.AUTO)） */
  private Long id;

  /** 关联账户 ID（对应 transaction 表 account_id 列，BIGINT NOT NULL，外键 FK → account.id） */
  private Long accountId;

  /** 关联账户名称（由 SQL JOIN account 表填充 a.name，不是 transaction 表字段，前端 el-table 列显示用） */
  private String accountName;

  /** 关联分类 ID（对应 transaction 表 category_id 列，BIGINT NOT NULL，外键 FK → category.id） */
  private Long categoryId;

  /** 关联分类名称（由 SQL JOIN category 表填充 c.name，不是 transaction 表字段，前端 el-table 列显示用） */
  private String categoryName;

  /** 交易类型：1=收入 2=支出（对应 transaction 表 type 列，TINYINT NOT NULL，前端按类型显示不同颜色标签：绿色收入/红色支出） */
  private Integer type;

  /** 交易金额（对应 transaction 表 amount 列，DECIMAL(12,2) NOT NULL，精度保留 2 位小数，必须 > 0） */
  private BigDecimal amount;

  /** 备注（对应 transaction 表 note 列，VARCHAR(200) DEFAULT NULL，可选填，前端 el-table 列显示） */
  private String note;

  /** 交易时间（对应 transaction 表 time 列，DATETIME NOT NULL，格式为 "yyyy-MM-dd HH:mm:ss" 字符串，列表查询用 DATE_FORMAT SQL 函数格式化） */
  private String time;

  /** 转账关联 UUID（对应 transaction 表 transfer_id 列，VARCHAR(36) DEFAULT NULL，NULL=普通收支记录，非 NULL=转账关联记录，前端流水列表标记「转入/转出」标签） */
  private String transferId;

  /** 创建时间（对应 transaction 表 create_time 列，DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP） */
  private LocalDateTime createTime;

  /** 最后更新时间（对应 transaction 表 update_time 列，DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP，每次修改自动更新） */
  private LocalDateTime updateTime;
}
