package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.common.BusinessException;
import com.example.finance.entity.Account;
import com.example.finance.entity.Category;
import com.example.finance.entity.RecurringBill;
import com.example.finance.entity.dto.RecurringBillDTO;
import com.example.finance.entity.dto.RecurringBillRequest;
import com.example.finance.mapper.AccountMapper;
import com.example.finance.mapper.CategoryMapper;
import com.example.finance.mapper.RecurringBillMapper;
import com.example.finance.mapper.TransactionMapper;
import com.example.finance.common.EntityValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringBillServiceImplTest {

  @Mock
  private RecurringBillMapper recurringBillMapper;
  @Mock
  private TransactionMapper transactionMapper;
  @Mock
  private AccountMapper accountMapper;
  @Mock
  private CategoryMapper categoryMapper;
  @Mock
  private EntityValidator entityValidator;

  @InjectMocks
  private RecurringBillServiceImpl service;

  private Account testAccount;
  private Category testCategory;
  private RecurringBill testBill;

  @BeforeEach
  void setUp() {
    testAccount = new Account();
    testAccount.setId(1L);
    testAccount.setUserId(1L);
    testAccount.setName("现金");
    testAccount.setStatus(1);

    testCategory = new Category();
    testCategory.setId(1L);
    testCategory.setName("餐饮");

    testBill = new RecurringBill();
    testBill.setId(1L);
    testBill.setUserId(1L);
    testBill.setAccountId(1L);
    testBill.setCategoryId(1L);
    testBill.setName("月房租");
    testBill.setAmount(new BigDecimal("2500.00"));
    testBill.setType(2);
    testBill.setPeriod("monthly");
    testBill.setNextDueDate(LocalDate.of(2026, 6, 1));
    testBill.setStatus(1);
  }

  @Test
  @DisplayName("创建周期账单成功")
  void create_success() {
    when(entityValidator.validateAccount(1L, 1L)).thenReturn(testAccount);  // mock EntityValidator 校验账户
    when(entityValidator.validateCategory(1L)).thenReturn(testCategory);  // mock EntityValidator 校验分类
    when(recurringBillMapper.insert(any(RecurringBill.class))).thenReturn(1);

    RecurringBillRequest req = new RecurringBillRequest();
    req.setAccountId(1L);
    req.setCategoryId(1L);
    req.setName("月房租");
    req.setAmount(new BigDecimal("2500.00"));
    req.setType(2);
    req.setPeriod("monthly");
    req.setNextDueDate("2026-06-01");

    RecurringBillDTO dto = service.create(1L, req);
    assertNotNull(dto);
    assertEquals("月房租", dto.getName());
    verify(recurringBillMapper).insert(any(RecurringBill.class));
  }

  @Test
  @DisplayName("创建失败 - 账户不存在")
  void create_accountNotFound() {
    when(entityValidator.validateAccount(1L, 999L)).thenThrow(new BusinessException(3004, "账户不存在或已禁用"));  // mock EntityValidator 账户不存在

    RecurringBillRequest req = new RecurringBillRequest();
    req.setAccountId(999L);
    req.setCategoryId(1L);
    req.setName("invalid");
    req.setAmount(BigDecimal.ONE);
    req.setType(2);
    req.setPeriod("monthly");
    req.setNextDueDate("2026-06-01");

    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.create(1L, req));
    // ErrorCode 已改为 ACCOUNT_NOT_FOUND_OR_DISABLED(3004)，因为 validateAccount 现在走 EntityValidator
    assertEquals(3004, ex.getCode());
  }

  @Test
  @DisplayName("停用周期账单成功")
  void deactivate_success() {
    when(recurringBillMapper.selectById(1L)).thenReturn(testBill);
    when(recurringBillMapper.updateById(any(RecurringBill.class))).thenReturn(1);

    assertDoesNotThrow(() -> service.deactivate(1L, 1L));
    assertEquals(0, testBill.getStatus());
  }

  @Test
  @DisplayName("停用失败 - 已停用")
  void deactivate_alreadyInactive() {
    testBill.setStatus(0);
    when(recurringBillMapper.selectById(1L)).thenReturn(testBill);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.deactivate(1L, 1L));
    assertEquals(5005, ex.getCode());
  }

  @Test
  @DisplayName("生成交易记录失败 - 账单已停用")
  void generate_inactiveBill() {
    testBill.setStatus(0);
    when(recurringBillMapper.selectById(1L)).thenReturn(testBill);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.generate(1L, 1L));
    assertEquals(5005, ex.getCode());
  }

  @Test
  @DisplayName("列表查询空数据")
  void list_empty() {
    when(recurringBillMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(Collections.emptyList());

    var result = service.list(1L);
    assertTrue(result.isEmpty());
  }
}
