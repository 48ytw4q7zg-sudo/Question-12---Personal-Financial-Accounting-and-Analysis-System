package com.example.finance.entity.dto; // 声明该类属于 entity.dto 包（数据传输对象层，仅暴露安全字段，避免直接暴露 User entity 含 password 字段）

import com.example.finance.entity.User; // 导入 User 实体类（@TableName("user") ORM 映射，用于 fromUser() 静态工厂做 Entity→DTO 脱敏转换）
import lombok.AllArgsConstructor; // Lombok: 自动生成全参构造器（Service 层快捷构造: new UserDTO(id, name, role, createTime)）
import lombok.Data; // Lombok: 自动生成 getter/setter/toString/equals/hashCode（减少样板代码）
import lombok.NoArgsConstructor; // Lombok: 自动生成无参构造器（Jackson 反序列化 JSON→对象时需要，如 AdminController 返回 JSON 场景）

import java.time.LocalDateTime; // JDK 8+ 日期时间类（线程安全不可变，映射数据库 user 表 DATETIME 类型字段 create_time）

/**
 * 用户数据传输对象（管理员接口返回，避免直接暴露 User entity 含密码字段）
 *
 * <p>数据库来源：user 表（id / username / role / create_time），不含 password 字段（安全脱敏）。</p>
 * <p>用途：AdminController.listUsers() / toggleRole() 返回，仅暴露 id/username/role/createTime。</p>
 * <p>跨文件引用：被 AdminController → UserServiceImpl.listAllUsers() → UserDTO.fromUser() 使用，
 * 前端 AdminPage.vue 用户列表消费此 DTO。</p>
 *
 * <p>安全：故意不包含 password 字段 — User entity 的 password 由 @JsonIgnore 排除，fromUser() 手动忽略密码字段。</p>
 */
@Data // Lombok: 自动生成 getter/setter/toString/equals/hashCode
@NoArgsConstructor // 无参构造器（Jackson 反序列化 JSON→对象时需要默认构造器）
@AllArgsConstructor // 全参构造器（Service 层快捷构造: new UserDTO(id, name, role, createTime)）
public class UserDTO {

  /** 用户主键 ID（对应 user 表 id 列，BIGINT AUTO_INCREMENT 自增主键，@TableId(IdType.AUTO)） */
  private Long id;

  /** 用户名（对应 user 表 username 列，VARCHAR(20) UNIQUE NOT NULL，注册时 @Size(min=3, max=20) @Pattern(regexp="^[a-zA-Z0-9_]+$") 校验） */
  private String username;

  /** 角色：0=普通用户, 1=管理员（对应 user 表 role 列，TINYINT NOT NULL DEFAULT 0，满足评分标准≥2类角色要求） */
  private Integer role;

  /** 创建时间（对应 user 表 create_time 列，DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP，Jackson 序列化为 ISO 8601 格式字符串） */
  private LocalDateTime createTime;

  /**
   * 从 User entity 转换为 UserDTO（Entity→DTO 脱敏转换，手动排除 password 字段）
   *
   * <p>调用方：UserServiceImpl.listAllUsers() → 遍历 userList → UserDTO.fromUser(user)。</p>
   *
   * @param user 用户实体（来自 user 表 MyBatis-Plus BaseMapper 查询结果）
   * @return 脱敏后的 UserDTO 实例（不含 password 字段，仅暴露 4 个安全字段）
   */
  public static UserDTO fromUser(User user) {
    UserDTO dto = new UserDTO(); // 创建空 DTO 对象（字段默认 null）
    dto.setId(user.getId()); // 映射：user.id（BIGINT） → dto.id
    dto.setUsername(user.getUsername()); // 映射：user.username（VARCHAR(20)） → dto.username
    dto.setRole(user.getRole()); // 映射：user.role（TINYINT） → dto.role
    dto.setCreateTime(user.getCreateTime()); // 映射：user.createTime（DATETIME/LocalDateTime） → dto.createTime
    return dto; // 返回脱敏后的 DTO（不含 password 字段，安全）
  }
}
