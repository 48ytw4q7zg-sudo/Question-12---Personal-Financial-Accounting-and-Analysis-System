package com.example.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;       // MP 主键策略（IdType.AUTO=数据库自增）
import com.baomidou.mybatisplus.annotation.TableField;    // MP 字段映射（驼峰↔下划线转换）
import com.baomidou.mybatisplus.annotation.TableId;       // MP 主键标识
import com.baomidou.mybatisplus.annotation.TableName;      // MP 表名映射
import com.fasterxml.jackson.annotation.JsonIgnore;        // Jackson 序列化时忽略该字段（禁止密码泄漏到 JSON 响应）
import lombok.Data;                                       // Lombok 自动生成 getter/setter/toString/equals/hashCode

import java.time.LocalDateTime;                           // JDK 21 时间类型（对齐 DATETIME 数据库列）

/**
 * 用户实体（对应数据库 user 表，PRD P0-1 登录/JWT）
 *
 * <p>双角色系统：0=普通用户, 1=管理员。所有业务数据按 user_id 隔离，管理员通过 AdminController 管理用户。</p>
 * <p>密码通过 @JsonIgnore 禁止序列化到响应中（登录响应走 LoginResponse DTO，不直接返回此实体）。</p>
 *
 * <p>关联文件：</p>
 * <ul>
 *   <li>被调用方: UserController.java / UserServiceImpl.java / AdminController.java</li>
 *   <li>关联 DTO: UserLoginRequest.java / LoginResponse.java / UserDTO.java</li>
 *   <li>数据库 DDL: sql/01-init.sql CREATE TABLE user</li>
 * </ul>
 */
@Data                               // Lombok: 自动生成 getter/setter/toString/equals/hashCode
@TableName("user")                  // 映射数据库 user 表
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

  /** 角色：0=普通用户, 1=管理员（对应 AdminController 管理员功能） */
  @TableField("role")
  private Integer role;

  /** 创建时间 */
  @TableField("create_time")
  private LocalDateTime createTime;

  /** 最后更新时间 */
  @TableField("update_time")
  private LocalDateTime updateTime;
}
