package com.example.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;       // MP主键策略
import com.baomidou.mybatisplus.annotation.TableField;    // MP字段映射
import com.baomidou.mybatisplus.annotation.TableId;       // MP主键标识
import com.baomidou.mybatisplus.annotation.TableLogic;     // MP逻辑删除（需字段级声明，MybatisPlusConfig.java 未全局配置）
import com.baomidou.mybatisplus.annotation.TableName;      // MP表名映射
import lombok.Data;                                       // Lombok自动生成getter/setter/toString/equals/hashCode

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 周期性账单实体（对应数据库 recurring_bill 表，PRD P1-4 周期性账单提醒）
 *
 * <p>用户设置周期性收支模板（月房租/月工资等），手动一键生成交易记录。</p>
 * <p>停用后不可恢复（软删除 status=0）。到期日到达后由 @Scheduled 定时任务检测并通知。</p>
 *
 * <p>关联文件：</p>
 * <ul>
 *   <li>被调用方: RecurringBillController.java / RecurringBillServiceImpl.java / BillScheduler.java</li>
 *   <li>关联 DTO: RecurringBillRequest.java / RecurringBillDTO.java</li>
 *   <li>数据库 DDL: sql/01-init.sql CREATE TABLE recurring_bill</li>
 * </ul>
 */
@Data                                            // Lombok: 自动生成 getter/setter/toString/equals/hashCode
@TableName("recurring_bill")                      // 映射数据库 recurring_bill 表
public class RecurringBill {

  /** 账单主键 ID（BIGINT AUTO_INCREMENT） */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 所属用户 ID（FK → user.id，数据隔离） */
  @TableField("user_id")
  private Long userId;

  /** 关联账户 ID（FK → account.id，一键生成时写入该账户） */
  @TableField("account_id")
  private Long accountId;

  /** 关联分类 ID（FK → category.id） */
  @TableField("category_id")
  private Long categoryId;

  /** 账单名称（1-30 字符，如「月房租」「月工资」） */
  @TableField("name")
  private String name;

  /** 金额（DECIMAL(12,2)，必须 > 0） */
  @TableField("amount")
  private BigDecimal amount;

  /**
   * 类型：1=收入 2=支出（与 Transaction.type 语义一致，generate()直接透传）
   */
  @TableField("type")
  private Integer type;

  /**
   * 周期：daily=每日 weekly=每周 monthly=每月 yearly=每年
   */
  @TableField("period")
  private String period;

  /** 下次到期日（DATE，用于 @Scheduled 判断是否到期） */
  @TableField("next_due_date")
  private LocalDate nextDueDate;

  /**
   * 状态：1=启用 0=停用（停用后不可恢复）
   * @TableLogic 注解：MyBatis-Plus 自动在 SELECT 时追加 status=1 条件
   * 注意：DELETE/停用操作未使用 deleteById()（由 RecurringBillServiceImpl.java 手动 updateById 设置 status）
   *       因 deactivate() 需先检查关联账户状态，不依赖 MP 的自动逻辑删除
   */
  @TableLogic(value = "1", delval = "0")                // MP逻辑删除: value=启用, delval=停用
  @TableField("status")
  private Integer status;

  /** 创建时间 */
  @TableField("create_time")
  private LocalDateTime createTime;

  /** 最后更新时间 */
  @TableField("update_time")
  private LocalDateTime updateTime;
}
