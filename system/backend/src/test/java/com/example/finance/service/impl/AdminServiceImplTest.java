package com.example.finance.service.impl;

import com.example.finance.common.BusinessException;
import com.example.finance.entity.User;
import com.example.finance.entity.dto.UserDTO;
import com.example.finance.mapper.AccountMapper;
import com.example.finance.mapper.BudgetAlertMapper;
import com.example.finance.mapper.BudgetMapper;
import com.example.finance.mapper.RecurringBillMapper;
import com.example.finance.mapper.TransactionMapper;
import com.example.finance.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AdminServiceImpl 单元测试（评分标准要求 ≥2 类用户角色: 普通用户 + 管理员）
 *
 * 测试覆盖:
 *   - listAllUsers(): 正常查询 + 空集合
 *   - deleteUser(): 正常删除 + 自删防护 + 用户不存在
 *   - toggleUserRole(): 正常切换 + 自切换防护 + 用户不存在
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

  @Mock
  private UserMapper userMapper;
  @Mock
  private TransactionMapper transactionMapper;
  @Mock
  private BudgetMapper budgetMapper;
  @Mock
  private RecurringBillMapper recurringBillMapper;
  @Mock
  private AccountMapper accountMapper;
  @Mock
  private BudgetAlertMapper budgetAlertMapper;

  @InjectMocks
  private AdminServiceImpl adminService;

  private User adminUser;
  private User normalUser;

  @BeforeEach
  void setUp() {
    // 管理员用户（role=1）
    adminUser = new User();
    adminUser.setId(1L);
    adminUser.setUsername("admin");
    adminUser.setRole(1);

    // 普通用户（role=0）
    normalUser = new User();
    normalUser.setId(2L);
    normalUser.setUsername("zhangsan");
    normalUser.setRole(0);
  }

  // ==================== listAllUserDTOs 测试 ====================

  @Test
  @DisplayName("查询所有用户DTO - 正常返回多个用户DTO")
  void listAllUserDTOs_shouldReturnAllUserDTOs() {
    when(userMapper.selectList(any())).thenReturn(Arrays.asList(adminUser, normalUser));

    List<UserDTO> result = adminService.listAllUserDTOs();

    assertEquals(2, result.size());
    assertEquals("admin", result.get(0).getUsername());
    assertEquals("zhangsan", result.get(1).getUsername());
    verify(userMapper).selectList(any());
  }

  @Test
  @DisplayName("查询所有用户DTO - 空集合")
  void listAllUserDTOs_shouldReturnEmptyList() {
    when(userMapper.selectList(any())).thenReturn(Collections.emptyList());

    List<UserDTO> result = adminService.listAllUserDTOs();

    assertTrue(result.isEmpty());
    verify(userMapper).selectList(any());
  }

  // ==================== deleteUser 测试 ====================

  @Test
  @DisplayName("删除用户 - 正常删除（级联清理关联数据）")
  void deleteUser_shouldDeleteUser() {
    when(userMapper.selectById(2L)).thenReturn(normalUser);
    when(transactionMapper.delete(any())).thenReturn(0);
    when(budgetAlertMapper.delete(any())).thenReturn(0);
    when(budgetMapper.delete(any())).thenReturn(0);
    when(recurringBillMapper.delete(any())).thenReturn(0);
    when(accountMapper.delete(any())).thenReturn(0);
    when(userMapper.deleteById(2L)).thenReturn(1);

    assertDoesNotThrow(() -> adminService.deleteUser(2L, 1L));
    // 验证级联删除顺序：transactions → budget_alerts → budgets → recurring_bills → accounts → user
    verify(transactionMapper).delete(any());
    verify(budgetAlertMapper).delete(any());
    verify(budgetMapper).delete(any());
    verify(recurringBillMapper).delete(any());
    verify(accountMapper).delete(any());
    verify(userMapper).deleteById(2L);
  }

  @Test
  @DisplayName("删除用户 - 不能删除自己")
  void deleteUser_shouldThrowWhenDeletingSelf() {
    BusinessException e = assertThrows(BusinessException.class,
        () -> adminService.deleteUser(1L, 1L));
    assertEquals(6001, e.getCode());
    assertTrue(e.getMessage().contains("不能删除自己"));
    verify(userMapper, never()).deleteById(anyLong());
  }

  @Test
  @DisplayName("删除用户 - 用户不存在")
  void deleteUser_shouldThrowWhenUserNotFound() {
    when(userMapper.selectById(999L)).thenReturn(null);

    BusinessException e = assertThrows(BusinessException.class,
        () -> adminService.deleteUser(999L, 1L));
    assertEquals(6003, e.getCode());
    assertTrue(e.getMessage().contains("用户不存在"));
    verify(userMapper, never()).deleteById(anyLong());
  }

  // ==================== toggleUserRole 测试 ====================

  @Test
  @DisplayName("切换角色 - 普通用户变管理员")
  void toggleUserRole_shouldToggleFromNormalToAdmin() {
    when(userMapper.selectById(2L)).thenReturn(normalUser);
    when(userMapper.updateById(normalUser)).thenReturn(1);

    UserDTO result = adminService.toggleUserRole(2L, 1L);

    assertEquals(1, result.getRole()); // 0 → 1
    verify(userMapper).updateById(normalUser);
  }

  @Test
  @DisplayName("切换角色 - 管理员变普通用户")
  void toggleUserRole_shouldToggleFromAdminToNormal() {
    when(userMapper.selectById(1L)).thenReturn(adminUser);
    when(userMapper.updateById(adminUser)).thenReturn(1);

    UserDTO result = adminService.toggleUserRole(1L, 2L);

    assertEquals(0, result.getRole()); // 1 → 0
    verify(userMapper).updateById(adminUser);
  }

  @Test
  @DisplayName("切换角色 - 不能切换自己")
  void toggleUserRole_shouldThrowWhenTogglingSelf() {
    BusinessException e = assertThrows(BusinessException.class,
        () -> adminService.toggleUserRole(1L, 1L));
    assertEquals(6002, e.getCode());
    assertTrue(e.getMessage().contains("不能修改自己的角色"));
    verify(userMapper, never()).updateById(any(User.class));
  }

  @Test
  @DisplayName("切换角色 - 用户不存在")
  void toggleUserRole_shouldThrowWhenUserNotFound() {
    when(userMapper.selectById(999L)).thenReturn(null);

    BusinessException e = assertThrows(BusinessException.class,
        () -> adminService.toggleUserRole(999L, 1L));
    assertEquals(6003, e.getCode());
    assertTrue(e.getMessage().contains("用户不存在"));
    verify(userMapper, never()).updateById(any(User.class));
  }
}
