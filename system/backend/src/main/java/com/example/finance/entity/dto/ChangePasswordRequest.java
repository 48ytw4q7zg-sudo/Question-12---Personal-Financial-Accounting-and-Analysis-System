package com.example.finance.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求
 */
@Data
public class ChangePasswordRequest {

  @NotBlank(message = "旧密码不能为空")
  @Size(min = 6, max = 20, message = "密码长度6-20位")
  private String oldPassword;

  @NotBlank(message = "新密码不能为空")
  @Size(min = 6, max = 20, message = "密码长度6-20位")
  private String newPassword;
}
