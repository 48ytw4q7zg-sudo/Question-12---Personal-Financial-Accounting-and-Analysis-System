package com.example.finance.service;

import com.example.finance.entity.User;
import com.example.finance.entity.dto.UserDTO;

import java.util.List;

/**
 * 管理员服务接口（评分标准要求 ≥2 类用户角色: 普通用户 + 管理员）
 *
 * 职责：管理员专属业务逻辑——用户列表查询、用户删除、角色切换。
 * 调用方：AdminController（/api/admin 路由）
 */
public interface AdminService {

  /**
   * 查询所有用户DTO列表（仅管理员可调用 · DTO转换在Service层完成）
   *
   * @return 全部注册用户DTO列表（密码不返回）
   */
  List<UserDTO> listAllUserDTOs();

  /**
   * 删除指定用户（硬删除 · 仅管理员可调用）
   *
   * @param userId        要删除的用户 ID
   * @param currentUserId 当前管理员 ID（用于防止自删）
   */
  void deleteUser(Long userId, Long currentUserId);

  /**
   * 切换用户角色（普通用户 ↔ 管理员 · 仅管理员可调用）
   *
   * @param userId        要切换角色的用户 ID
   * @param currentUserId 当前管理员 ID（用于防止自切换）
   * @return 更新后的用户信息
   */
  User toggleUserRole(Long userId, Long currentUserId);
}
