package com.example.finance.entity.dto; // 声明该类属于 entity.dto 包（数据传输对象层，UserController 登录/注册成功 → 前端 LoginPage.vue 接收的 JSON 响应体）

import lombok.AllArgsConstructor;                         // Lombok: 自动生成全参构造器
import lombok.Data;                                       // Lombok: 自动生成 getter/setter/toString/equals/hashCode
import lombok.NoArgsConstructor;                          // Lombok: 自动生成无参构造器（Jackson 反序列化需要）

/**
 * 登录/注册响应 DTO（UserController → 前端 LoginPage.vue）
 *
 * <p>前端收到后处理流程: token → localStorage('token') → userStore.setUser(userId, username, role)。</p>
 * <p>含 @NoArgsConstructor 满足 Jackson 反序列化，@AllArgsConstructor 方便 Service 层一行构造。</p>
 *
 * <p>调用链路: UserServiceImpl.login()/register() → new LoginResponse(token, userId, username, role) → Result.success() → 前端。</p>
 */
@Data                                    // Lombok: 自动生成 getter/setter/toString/equals/hashCode
@NoArgsConstructor                       // 无参构造器（Jackson 反序列化 JSON → 对象时需要）
@AllArgsConstructor                      // 全参构造器（Service 层快捷构造: new LoginResponse(token, id, name, role)）
public class LoginResponse {

  /** JWT token（Bearer 认证，7 天有效期，含 userId + role） */
  private String token;

  /** 用户主键 ID（注册/登录成功后返回） */
  private Long userId;

  /** 用户名（显示在 AppLayout 顶栏右侧） */
  private String username;

  /** 角色：0=普通用户, 1=管理员（满足评分标准≥2类角色要求） */
  private Integer role;
}
