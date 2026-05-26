package com.example.finance.entity.dto; // 声明该类属于 entity.dto 包（数据传输对象层，接收前端 LoginPage.vue 表单 JSON 请求体 → UserController 入参）

import jakarta.validation.constraints.NotBlank;           // Jakarta Bean Validation: 字符串非空校验（含 trim）
import jakarta.validation.constraints.Pattern;            // Jakarta Bean Validation: 正则表达式校验
import jakarta.validation.constraints.Size;               // Jakarta Bean Validation: 字符串长度范围校验[min,max]
import lombok.Data;                                       // Lombok 自动生成 getter/setter/toString/equals/hashCode

/**
 * 用户登录/注册请求体（前端 LoginPage.vue → UserController.java POST /api/user/login 或 /register）
 *
 * <p>校验规则：用户名 3-20 字符（仅限字母/数字/下划线），密码 6-20 字符（任意字符）。</p>
 * <p>调用链路: LoginPage.vue → api/user.js loginUser()/registerUser() → POST /api/user/* → UserController → UserServiceImpl。</p>
 */
@Data                             // Lombok: 自动生成 getter/setter/toString/equals/hashCode
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
