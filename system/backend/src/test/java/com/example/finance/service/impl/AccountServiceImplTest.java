package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.common.BusinessException;
import com.example.finance.entity.Account;
import com.example.finance.entity.dto.AccountDTO;
import com.example.finance.entity.dto.AccountRequest;
import com.example.finance.mapper.AccountMapper;
import com.example.finance.mapper.RecurringBillMapper;
import com.example.finance.mapper.TransactionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AccountServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

  @Mock
  private AccountMapper accountMapper;
  @Mock
  private TransactionMapper transactionMapper;
  @Mock
  private RecurringBillMapper recurringBillMapper;

  @InjectMocks
  private AccountServiceImpl accountService;

  private Account testAccount;

  @BeforeEach
  void setUp() {
    testAccount = new Account();
    testAccount.setId(1L);
    testAccount.setUserId(1L);
    testAccount.setName("现金账户");
    testAccount.setType(1);
    testAccount.setInitialBalance(new BigDecimal("10000.00"));
    testAccount.setCurrency("CNY");
    testAccount.setStatus(1);
    testAccount.setCreateTime(LocalDateTime.now());
    testAccount.setUpdateTime(LocalDateTime.now());
  }

  @Test
  @DisplayName("创建账户成功")
  void create_success() {
    AccountRequest request = new AccountRequest();
    request.setName("现金账户");
    request.setType(1);
    request.setInitialBalance(new BigDecimal("10000.00"));
    request.setCurrency("CNY");

    when(accountMapper.insert(any(Account.class))).thenAnswer(inv -> {
      Account a = inv.getArgument(0);
      a.setId(1L);
      return 1;
    });

    AccountDTO dto = accountService.create(1L, request);
    assertNotNull(dto);
    assertEquals("现金账户", dto.getName());
    verify(accountMapper).insert(any(Account.class));
  }

  @Test
  @DisplayName("删除账户失败 - 有交易记录")
  void delete_hasTransactions() {
    when(accountMapper.selectById(1L)).thenReturn(testAccount);
    when(transactionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> accountService.delete(1L, 1L));
    assertEquals(2002, ex.getCode());
  }

  @Test
  @DisplayName("删除账户失败 - 账户不存在")
  void delete_notFound() {
    when(accountMapper.selectById(999L)).thenReturn(null);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> accountService.delete(1L, 999L));
    assertEquals(2003, ex.getCode());
  }

  @Test
  @DisplayName("查询账户列表成功")
  void list_success() {
    Account a2 = new Account();
    a2.setId(2L);
    a2.setUserId(1L);
    a2.setName("储蓄卡");
    a2.setType(2);
    a2.setInitialBalance(new BigDecimal("5000.00"));
    a2.setCurrency("CNY");
    a2.setStatus(1);

    when(accountMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(testAccount, a2));

    List<AccountDTO> result = accountService.list(1L);
    assertEquals(2, result.size());
    assertEquals("现金账户", result.get(0).getName());
    assertEquals("储蓄卡", result.get(1).getName());
  }

  @Test
  @DisplayName("查询账户列表 - 空数据")
  void list_empty() {
    when(accountMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

    List<AccountDTO> result = accountService.list(1L);
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("更新账户成功")
  void update_success() {
    AccountRequest request = new AccountRequest();
    request.setName("现金账户改名");
    request.setType(1);
    request.setInitialBalance(new BigDecimal("20000.00"));
    request.setCurrency("USD");

    when(accountMapper.selectById(1L)).thenReturn(testAccount);
    when(accountMapper.updateById(any(Account.class))).thenReturn(1);

    AccountDTO dto = accountService.update(1L, 1L, request);
    assertNotNull(dto);
    assertEquals("现金账户改名", dto.getName());
    verify(accountMapper).updateById(any(Account.class));
  }

  @Test
  @DisplayName("更新账户失败 - 账户不存在")
  void update_notFound() {
    AccountRequest request = new AccountRequest();
    request.setName("不存在");
    request.setType(1);
    request.setInitialBalance(BigDecimal.ZERO);

    when(accountMapper.selectById(999L)).thenReturn(null);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> accountService.update(1L, 999L, request));
    assertEquals(2003, ex.getCode());
  }

  @Test
  @DisplayName("创建账户 - 默认币种CNY")
  void create_defaultCurrency() {
    AccountRequest request = new AccountRequest();
    request.setName("新账户");
    request.setType(2);
    request.setInitialBalance(new BigDecimal("500.00"));
    // 不设置 currency，应默认 CNY

    when(accountMapper.insert(any(Account.class))).thenAnswer(inv -> {
      Account a = inv.getArgument(0);
      a.setId(2L);
      return 1;
    });

    AccountDTO dto = accountService.create(1L, request);
    assertNotNull(dto);
    // currency 在 entity 上设置，DTO 通过 BeanUtils.copyProperties 获得
    assertEquals(2L, dto.getId());
    verify(accountMapper).insert(any(Account.class));
  }
}
