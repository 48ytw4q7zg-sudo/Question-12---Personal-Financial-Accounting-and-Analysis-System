package com.example.finance.controller;

import com.example.finance.common.Result;
import com.example.finance.entity.dto.ChangePasswordRequest;
import com.example.finance.entity.dto.LoginResponse;
import com.example.finance.entity.dto.UserLoginRequest;
import com.example.finance.interceptor.LoginInterceptor;
import com.example.finance.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制器（PRD P0-1 登录/JWT + P1-7 修改密码）
 *
 * 职责：接收用户认证相关的 HTTP 请求，参数校验后转发 UserService 处理
 * 路由前缀：/api/user
 * 依赖：→ UserService（业务逻辑层）→ UserMapper（数据访问层）
 *
 * 接口清单：
 *   POST /api/user/register       — 用户注册（公开，无需 JWT）
 *   POST /api/user/login          — 用户登录（公开，无需 JWT）
 *   POST /api/user/change-password — 修改密码（需 JWT 登录）
 *
 * 被前端调用：→ api/user.js 的 login() / register() / changePassword()
 * 被 LoginPage.vue、UserSettingsPage.vue 调用
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

  /** → UserService：处理注册/登录/改密的业务逻辑 */
  private final UserService userService;

  /**
   * 用户注册接口
   *
   * 流程：接收用户名+密码 → @Valid 校验（@NotBlank + @Size）
   *     → 调用 userService.register() → BCrypt 加密密码 → 生成 JWT token
   *     → 返回 LoginResponse（token + userId + username）
   *
   * @param request 注册请求体（username + password，由 @Valid 自动校验）
   * @return Result<LoginResponse> 包含 JWT token 的登录响应
   *
   * 被前端 LoginPage.vue 的 register 表单调用 → api/user.js 的 register()
   * 业务异常码：1001 = 用户名已存在
   */
  @PostMapping("/register")
  public Result<LoginResponse> register(@Valid @RequestBody UserLoginRequest request) {
    // → UserService.register()：BCrypt 加密密码 + 生成 JWT token
    LoginResponse response = userService.register(request);
    return Result.success(response, "注册成功");
  }

  /**
   * 用户登录接口
   *
   * 流程：接收用户名+密码 → @Valid 校验
   *     → 调用 userService.login() → 查找用户 → BCrypt 验证密码
   *     → 生成 JWT token → 返回 LoginResponse
   *
   * @param request 登录请求体（username + password）
   * @return Result<LoginResponse> 包含 JWT token 的登录响应
   *
   * 被前端 LoginPage.vue 的 login 表单调用 → api/user.js 的 login()
   * 业务异常码：1002 = 用户名不存在 / 1003 = 密码错误
   */
  @PostMapping("/login")
  public Result<LoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
    // → UserService.login()：查询用户 + BCrypt 密码校验 + 签发 JWT
    LoginResponse response = userService.login(request);
    return Result.success(response, "登录成功");
  }

  /**
   * 修改密码接口（需登录，JWT 保护）
   *
   * 流程：LoginInterceptor 从 JWT 提取 userId → @Valid 校验新旧密码
   *     → 调用 userService.changePassword() → BCrypt 验证旧密码 → 加密新密码 → 更新数据库
   *
   * @param request 修改密码请求体（oldPassword + newPassword，由 @Valid 自动校验）
   * @param httpRequest HTTP 请求（LoginInterceptor 已注入 userId 属性）
   * @return Result<Void> 成功无返回数据
   *
   * 被前端 UserSettingsPage.vue 调用 → api/user.js 的 changePassword()
   * 业务异常码：1003 = 旧密码错误
   */
  @PostMapping("/change-password")
  public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
      HttpServletRequest httpRequest) {
    // → LoginInterceptor.getUserId()：从 request 属性中提取 JWT 解析出的 userId
    Long userId = LoginInterceptor.getUserId(httpRequest);
    // → UserService.changePassword()：验证旧密码 + BCrypt 加密新密码 + 更新数据库
    userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
    return Result.success(null, "密码修改成功");
  }
}
