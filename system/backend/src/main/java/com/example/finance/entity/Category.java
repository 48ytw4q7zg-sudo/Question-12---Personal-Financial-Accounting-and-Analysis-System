package com.example.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;       // MP 主键策略（IdType.AUTO=数据库自增）
import com.baomidou.mybatisplus.annotation.TableField;    // MP 字段映射（驼峰↔下划线转换）
import com.baomidou.mybatisplus.annotation.TableId;       // MP 主键标识
import com.baomidou.mybatisplus.annotation.TableName;      // MP 表名映射
import lombok.Data;                                       // Lombok 自动生成 getter/setter/toString/equals/hashCode

import java.time.LocalDateTime;                           // JDK 21 时间类型（对齐 DATETIME 数据库列）

/**
 * 分类实体（对应数据库 category 表，PRD P0-3 收支分类）
 *
 * <p>种子数据（13 条：支出 8 + 收入 5），由 sql/01-init.sql 初始化。</p>
 * <p>所有用户共享同一套分类，不支持用户自定义增改删。</p>
 *
 * <p>关联文件：</p>
 * <ul>
 *   <li>被调用方: CategoryController.java / CategoryServiceImpl.java</li>
 *   <li>关联 DTO: CategoryDTO.java / CategorySummaryDTO.java</li>
 *   <li>数据库 DDL: sql/01-init.sql INSERT INTO category</li>
 * </ul>
 */
@Data                               // Lombok: 自动生成 getter/setter/toString/equals/hashCode
@TableName("category")              // 映射数据库 category 表
public class Category {

  /** 分类主键 ID（BIGINT AUTO_INCREMENT，种子数据预置） */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 分类名称（1-10 字符，如：餐饮/交通/工资/奖金） */
  @TableField("name")
  private String name;

  /**
   * 类型：1=支出 2=收入
   */
  @TableField("type")
  private Integer type;

  /** 创建时间 */
  @TableField("create_time")
  private LocalDateTime createTime;

  /** 最后更新时间 */
  @TableField("update_time")
  private LocalDateTime updateTime;
}
