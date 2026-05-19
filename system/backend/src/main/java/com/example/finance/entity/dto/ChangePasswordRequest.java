package com.example.finance.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求体（前端 UserSettingsPage.vue → POST /api/user/change-password）
 *
 * 需 JWT 登录态（LoginInterceptor 提取 userId），用户名从 token 解码
 * 校验规则：旧密码必填 + 新密码 6-20 字符 + 新旧密码不可相同（Service 层校验）
 */
@Data
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
