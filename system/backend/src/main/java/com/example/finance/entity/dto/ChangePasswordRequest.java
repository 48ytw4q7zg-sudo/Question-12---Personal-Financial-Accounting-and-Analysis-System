package com.example.finance.entity.dto; // 声明该类属于 entity.dto 包（数据传输对象层，接收前端 UserSettingsPage.vue 修改密码表单 → UserController.changePassword 入参）

import jakarta.validation.constraints.NotBlank;           // Jakarta Bean Validation: 字符串非空校验
import jakarta.validation.constraints.Size;               // Jakarta Bean Validation: 字符串长度范围校验
import lombok.Data;                                       // Lombok 自动生成 getter/setter/toString/equals/hashCode

/**
 * 修改密码请求体（前端 UserSettingsPage.vue → UserController.java POST /api/user/change-password）
 *
 * <p>需 JWT 登录态（LoginInterceptor.java 从 token 解码提取 userId），用户名不在请求体中而在 token 中。</p>
 * <p>校验规则：旧密码 6-20 字符 + 新密码 6-20 字符 + 新旧密码不可相同（Service 层 BCrypt matches 校验）。</p>
 *
 * <p>调用链路: UserSettingsPage.vue → api/user.js changePassword() → POST /api/user/change-password → UserController.changePassword() → UserServiceImpl。</p>
 */
@Data                                         // Lombok: 自动生成 getter/setter/toString/equals/hashCode
public class ChangePasswordRequest {

  /** 旧密码（用于验证身份，BCrypt matches 校验） */
  @NotBlank(message = "旧密码不能为空")
  @Size(min = 6, max = 20, message = "密码长度6-20位")
  private String oldPassword;

  /** 新密码（6-20 字符，BCrypt 加密后更新数据库） */
  @NotBlank(message = "新密码不能为空")
  @Size(min = 6, max = 20, message = "密码长度6-20位")
  private String newPassword;
}
