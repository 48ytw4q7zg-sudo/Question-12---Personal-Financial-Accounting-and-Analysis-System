package com.example.finance.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户登录/注册请求
 */
@Data
public class UserLoginRequest {

  @NotBlank(message = "用户名不能为空")
  @Size(min = 3, max = 20, message = "用户名长度须在3-20之间")
  private String username;

  @NotBlank(message = "密码不能为空")
  @Size(min = 6, max = 20, message = "密码长度须在6-20之间")
  private String password;
}
