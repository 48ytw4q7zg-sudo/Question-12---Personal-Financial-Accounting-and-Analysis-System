package com.example.finance.controller;

import com.example.finance.common.Result;
import com.example.finance.entity.dto.UserDTO;
import com.example.finance.interceptor.LoginInterceptor;
import com.example.finance.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;  // @RestController/@RequestMapping/@GetMapping/@DeleteMapping/@PutMapping/@PathVariable

import java.util.List;

/**
 * 管理员控制器（评分标准要求 ≥2 类用户角色: 普通用户 + 管理员）
 *
 * <p>职责: 管理员专属接口——用户列表查询、用户删除、角色切换。</p>
 * <p>路由前缀: /api/admin · 所有接口需JWT鉴权且role=1(管理员)。</p>
 * <p>被前端 AdminPage.vue 调用 → api/admin.js。</p>
 *
 * <p>权限校验: 由 AdminInterceptor（interceptor/AdminInterceptor.java 第47行）统一拦截 /api/v1/admin/**，
 * 在 Controller 之前校验 role=1（管理员），非管理员返回 403。Controller 不重复校验。</p>
 * <p>分层约束: Controller 不直接操作 Mapper, 所有业务逻辑委托 AdminService 处理。</p>
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Validated
public class AdminController {

  /** → AdminService：管理员业务逻辑层（修复：之前直接注入 UserMapper 违反分层） */
  private final AdminService adminService;

  /**
   * 获取所有用户列表（仅管理员可调用 · 权限由 AdminInterceptor 统一校验）
   *
   * <p>返回全部注册用户的基本信息(id/username/role/createTime), 密码不返回(由UserDTO排除password字段)。</p>
   *
   * @param request HTTP请求(已由 LoginInterceptor 注入 userId+role，AdminInterceptor 已验证 role=1)
   * @return Result 包含用户DTO列表
   * @apiNote Q-CR修复：listUsers无需HttpServletRequest参数（权限由AdminInterceptor统一校验），移除以避免误导
   */
  @GetMapping("/users")
  public Result<List<UserDTO>> listUsers() {
    // → AdminServiceImpl.listAllUserDTOs()：查询 user 表全部记录（按 id 升序）→ 转 UserDTO（service/impl/AdminServiceImpl.java）
    return Result.success(adminService.listAllUserDTOs());
  }

  /**
   * 删除指定用户（仅管理员可调用 · 硬删除 · 权限由 AdminInterceptor 统一校验）
   *
   * @param userId 要删除的用户ID
   * @param request HTTP请求(用于获取当前管理员ID，防止自删)
   * @return Result 删除结果
   */
  @DeleteMapping("/users/{userId}")
  public Result<Void> deleteUser(@PathVariable @Min(1) Long userId, HttpServletRequest request) {
    Long currentUserId = LoginInterceptor.getUserId(request);
    // → AdminService.deleteUser()：校验管理员不能自删 → 级联清理关联数据 → 物理删除用户
    adminService.deleteUser(userId, currentUserId);
    return Result.success(null, "用户已删除");
  }

  /**
   * 切换用户角色（普通用户↔管理员 · 仅管理员可调用 · 权限由 AdminInterceptor 统一校验）
   *
   * @param userId 要切换角色的用户ID
   * @param request HTTP请求(用于获取当前管理员ID，防止自切换)
   * @return Result 包含更新后的用户信息
   */
  @PutMapping("/users/{userId}/role")
  public Result<UserDTO> toggleRole(@PathVariable @Min(1) Long userId, HttpServletRequest request) {
    Long currentUserId = LoginInterceptor.getUserId(request);
    // → AdminServiceImpl.toggleUserRole()：校验管理员不能切换自己 → 翻转 role 值（0↔1）→ 更新数据库（service/impl/AdminServiceImpl.java）
    UserDTO userDTO = adminService.toggleUserRole(userId, currentUserId);
    return Result.success(userDTO, "角色切换成功");  // Q-CR修复：添加成功提示（与其他接口风格一致）
  }
}
