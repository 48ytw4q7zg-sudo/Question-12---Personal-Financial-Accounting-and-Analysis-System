package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.common.BusinessException;
import com.example.finance.entity.User;
import com.example.finance.entity.dto.LoginResponse;
import com.example.finance.entity.dto.UserLoginRequest;
import com.example.finance.mapper.UserMapper;
import com.example.finance.service.UserService;
import com.example.finance.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserMapper userMapper;
  private final BCryptPasswordEncoder bCryptEncoder = new BCryptPasswordEncoder();

  /**
   * 注册（用户名已存在则抛异常）
   */
  @Override
  public LoginResponse register(UserLoginRequest request) {
    // 检查用户名是否已存在
    User existing = userMapper.selectOne(
        new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
    );
    if (existing != null) {
      throw new BusinessException(1001, "用户名已存在");
    }

    // 创建用户
    User user = new User();
    user.setUsername(request.getUsername());
    user.setPassword(bCryptEncoder.encode(request.getPassword()));
    user.setCreateTime(LocalDateTime.now());
    user.setUpdateTime(LocalDateTime.now());
    userMapper.insert(user);

    String token = JwtUtils.generateToken(user.getId());
    return new LoginResponse(token, user.getId(), user.getUsername());
  }

  /**
   * 登录（校验密码，用户不存在则抛异常）
   */
  @Override
  public LoginResponse login(UserLoginRequest request) {
    User user = userMapper.selectOne(
        new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
    );
    if (user == null || !bCryptEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new BusinessException(1002, "用户名或密码错误");
    }

    String token = JwtUtils.generateToken(user.getId());
    return new LoginResponse(token, user.getId(), user.getUsername());
  }

  /**
   * 修改密码
   */
  @Override
  public void changePassword(Long userId, String oldPassword, String newPassword) {
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BusinessException(1003, "用户不存在");
    }

    // 校验旧密码
    if (!bCryptEncoder.matches(oldPassword, user.getPassword())) {
      throw new BusinessException(1002, "旧密码错误");
    }

    // 校验新密码不能与旧密码相同
    if (bCryptEncoder.matches(newPassword, user.getPassword())) {
      throw new BusinessException(1003, "新密码不能与旧密码相同");
    }

    // 更新密码
    user.setPassword(bCryptEncoder.encode(newPassword));
    user.setUpdateTime(LocalDateTime.now());
    userMapper.updateById(user);
  }
}
