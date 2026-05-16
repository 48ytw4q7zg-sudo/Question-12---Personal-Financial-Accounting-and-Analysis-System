package com.example.finance.service;

import com.example.finance.entity.dto.LoginResponse;
import com.example.finance.entity.dto.UserLoginRequest;

/**
 * 用户服务接口
 */
public interface UserService {

  /**
   * 注册（用户名已存在则抛异常）
   */
  LoginResponse register(UserLoginRequest request);

  /**
   * 登录（校验密码）
   */
  LoginResponse login(UserLoginRequest request);

  /**
   * 修改密码
   */
  void changePassword(Long userId, String oldPassword, String newPassword);
}
