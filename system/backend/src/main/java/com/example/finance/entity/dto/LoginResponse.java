package com.example.finance.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录/注册响应（返回给前端 LoginPage.vue）
 *
 * 前端收到后：token → localStorage，userId + username → userStore.setUser()
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

  /** JWT token（Bearer 认证，7 天有效期，含 userId + role） */
  private String token;

  /** 用户主键 ID（注册/登录成功后返回） */
  private Long userId;

  /** 用户名（显示在 AppLayout 顶栏右侧） */
  private String username;
}
