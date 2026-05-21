package com.example.finance.entity.dto;

import com.example.finance.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户数据传输对象（管理员接口返回，避免直接暴露 User entity）
 *
 * 用途: AdminController.listUsers() / toggleRole() 返回，仅暴露 id/username/role/createTime
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

  /** 用户主键 ID */
  private Long id;

  /** 用户名 */
  private String username;

  /** 角色：0=普通用户, 1=管理员 */
  private Integer role;

  /** 创建时间 */
  private LocalDateTime createTime;

  /**
   * 从 User entity 转换为 UserDTO
   * @param user 用户实体
   * @return UserDTO
   */
  public static UserDTO fromUser(User user) {
    UserDTO dto = new UserDTO();
    dto.setId(user.getId());
    dto.setUsername(user.getUsername());
    dto.setRole(user.getRole());
    dto.setCreateTime(user.getCreateTime());
    return dto;
  }
}
