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
}
