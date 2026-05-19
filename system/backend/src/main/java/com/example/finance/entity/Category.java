package com.example.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分类实体（对应 category 表，PRD P0-3 收支分类）
 *
 * 种子数据（13 条：支出 8 + 收入 5），由 sql/01-init.sql 初始化
 * 所有用户共享同一套分类，不支持用户自定义增改删
 */
@Data
@TableName("category")
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
