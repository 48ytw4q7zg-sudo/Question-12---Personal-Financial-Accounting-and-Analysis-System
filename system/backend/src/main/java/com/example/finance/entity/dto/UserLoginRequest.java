package com.example.finance.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户登录/注册请求体（前端 LoginPage.vue → POST /api/user/login 或 /register）
 *
 * 校验规则：用户名 3-20 字符（字母/数字/下划线），密码 6-20 字符
 */
@Data
public class UserLoginRequest {

  /** 用户名（3-20 字符，字母/数字/下划线，@NotBlank + @Size + @Pattern 校验） */
  @NotBlank(message = "用户名不能为空")
  @Size(min = 3, max = 20, message = "用户名长度须在3-20之间")
  @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
  private String username;

  /** 密码（6-20 字符，@NotBlank + @Size 校验，BCrypt 加密后存储） */
  @NotBlank(message = "密码不能为空")
  @Size(min = 6, max = 20, message = "密码长度须在6-20之间")
  private String password;
}
