package com.example.finance.service;

import com.example.finance.entity.dto.LoginResponse;
import com.example.finance.entity.dto.UserLoginRequest;

/**
 * 用户服务接口（PRD P0-1 用户注册/登录/JWT + 修改密码）
 */
public interface UserService {

  /**
   * 用户注册（用户名已存在则抛异常 1001）
   *
   * @param request 注册请求（含用户名、密码）
   * @return 登录响应（含 JWT token、userId、username、role）
   * @throws com.example.finance.common.BusinessException 1001 用户名已存在
   */
  LoginResponse register(UserLoginRequest request);

  /**
   * 用户登录（校验用户名 + BCrypt 密码，签发 JWT token）
   *
   * @param request 登录请求（含用户名、密码）
   * @return 登录响应（含 JWT token、userId、username、role）
   * @throws com.example.finance.common.BusinessException 1002 用户名或密码错误
   */
  LoginResponse login(UserLoginRequest request);

  /**
   * 修改密码（需验证旧密码，新旧密码不可相同）
   *
   * @param userId      当前用户 ID（JWT 解码获取）
   * @param oldPassword 旧密码（BCrypt matches 校验）
   * @param newPassword 新密码（BCrypt 加密后更新数据库）
   * @throws com.example.finance.common.BusinessException 1005 旧密码错误（对齐 ErrorCode.OLD_PASSWORD_ERROR）
   * @throws com.example.finance.common.BusinessException 1006 新旧密码不能相同（对齐 ErrorCode.SAME_PASSWORD）
   */
  void changePassword(Long userId, String oldPassword, String newPassword);
}
