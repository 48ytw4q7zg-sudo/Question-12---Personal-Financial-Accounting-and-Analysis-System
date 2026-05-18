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
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  /**
   * 注册
   */
  @PostMapping("/register")
  public Result<LoginResponse> register(@Valid @RequestBody UserLoginRequest request) {
    LoginResponse response = userService.register(request);
    return Result.success(response, "注册成功");
  }

  /**
   * 登录
   */
  @PostMapping("/login")
  public Result<LoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
    LoginResponse response = userService.login(request);
    return Result.success(response, "登录成功");
  }

  /**
   * 修改密码（需登录）
   */
  @PostMapping("/change-password")
  public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
      HttpServletRequest httpRequest) {
    Long userId = LoginInterceptor.getUserId(httpRequest);
    userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
    return Result.success(null, "密码修改成功");
  }
}
