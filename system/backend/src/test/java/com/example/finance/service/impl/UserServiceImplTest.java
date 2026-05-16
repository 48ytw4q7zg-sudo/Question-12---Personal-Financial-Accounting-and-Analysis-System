package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.common.BusinessException;
import com.example.finance.entity.User;
import com.example.finance.entity.dto.LoginResponse;
import com.example.finance.entity.dto.UserLoginRequest;
import com.example.finance.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @Mock
  private UserMapper userMapper;

  @InjectMocks
  private UserServiceImpl userService;

  private UserLoginRequest validRequest;

  @BeforeEach
  void setUp() {
    validRequest = new UserLoginRequest();
    validRequest.setUsername("testuser");
    validRequest.setPassword("123456");
  }

  @Test
  @DisplayName("注册成功 - 新用户名")
  void register_success() {
    when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
    when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
      User user = invocation.getArgument(0);
      user.setId(1L);
      return 1;
    });

    LoginResponse response = userService.register(validRequest);

    assertNotNull(response);
    assertNotNull(response.getToken());
    assertEquals("testuser", response.getUsername());
    verify(userMapper).insert(any(User.class));
  }

  @Test
  @DisplayName("注册失败 - 用户名已存在")
  void register_duplicateUsername() {
    User existing = new User();
    existing.setId(1L);
    existing.setUsername("testuser");
    when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> userService.register(validRequest));
    assertEquals(1001, ex.getCode());
    assertEquals("用户名已存在", ex.getMessage());
  }

  @Test
  @DisplayName("登录成功 - 正确密码")
  void login_success() {
    User user = new User();
    user.setId(1L);
    user.setUsername("testuser");
    user.setPassword(new BCryptPasswordEncoder().encode("123456"));
    when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

    LoginResponse response = userService.login(validRequest);

    assertNotNull(response);
    assertNotNull(response.getToken());
    assertEquals(1L, response.getUserId());
  }

  @Test
  @DisplayName("登录失败 - 用户不存在")
  void login_userNotFound() {
    when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> userService.login(validRequest));
    assertEquals(1002, ex.getCode());
    assertEquals("用户名或密码错误", ex.getMessage());
  }

  @Test
  @DisplayName("登录失败 - 密码错误")
  void login_wrongPassword() {
    User user = new User();
    user.setId(1L);
    user.setUsername("testuser");
    user.setPassword(new BCryptPasswordEncoder().encode("correctpass"));
    when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> userService.login(validRequest));
    assertEquals(1002, ex.getCode());
  }

  @Test
  @DisplayName("修改密码成功")
  void changePassword_success() {
    User user = new User();
    user.setId(1L);
    user.setUsername("testuser");
    user.setPassword(new BCryptPasswordEncoder().encode("123456"));
    when(userMapper.selectById(1L)).thenReturn(user);

    userService.changePassword(1L, "123456", "newpass123");

    verify(userMapper).updateById(any(User.class));
  }

  @Test
  @DisplayName("修改密码失败 - 旧密码错误")
  void changePassword_wrongOldPassword() {
    User user = new User();
    user.setId(1L);
    user.setPassword(new BCryptPasswordEncoder().encode("123456"));
    when(userMapper.selectById(1L)).thenReturn(user);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> userService.changePassword(1L, "wrongpass", "newpass123"));
    assertEquals(1002, ex.getCode());
  }

  @Test
  @DisplayName("修改密码失败 - 新旧密码相同")
  void changePassword_samePassword() {
    User user = new User();
    user.setId(1L);
    user.setPassword(new BCryptPasswordEncoder().encode("123456"));
    when(userMapper.selectById(1L)).thenReturn(user);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> userService.changePassword(1L, "123456", "123456"));
    assertEquals(1003, ex.getCode());
    assertEquals("新密码不能与旧密码相同", ex.getMessage());
  }

  @Test
  @DisplayName("修改密码失败 - 用户不存在")
  void changePassword_userNotFound() {
    when(userMapper.selectById(999L)).thenReturn(null);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> userService.changePassword(999L, "123456", "newpass"));
    assertEquals(1003, ex.getCode());
  }
}
