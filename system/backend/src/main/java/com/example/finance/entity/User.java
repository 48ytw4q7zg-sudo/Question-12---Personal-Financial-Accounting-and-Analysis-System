package com.example.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体（对应 user 表，PRD P0-1 登录/JWT）
 *
 * 单一用户角色系统，所有数据按 user_id 隔离
 * 密码通过 @JsonIgnore 禁止序列化到响应中（登录响应走 LoginResponse DTO）
 */
@Data
@TableName("user")
public class User {

  /** 用户主键 ID（BIGINT AUTO_INCREMENT） */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 用户名（3-20 字符，字母/数字/下划线，唯一索引） */
  @TableField("username")
  private String username;

  /** BCrypt 哈希密码（@JsonIgnore 禁止序列化到 JSON 响应） */
  @TableField("password")
  @JsonIgnore
  private String password;

  /** 角色：0=普通用户（教学简化，单一角色预留扩展） */
  @TableField("role")
  private Integer role;

  /** 创建时间 */
  @TableField("create_time")
  private LocalDateTime createTime;

  /** 最后更新时间 */
  @TableField("update_time")
  private LocalDateTime updateTime;
}
