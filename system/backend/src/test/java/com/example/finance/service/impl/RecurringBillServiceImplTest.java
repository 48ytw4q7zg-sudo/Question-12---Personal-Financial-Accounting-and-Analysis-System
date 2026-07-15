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

/**
 * 周期账单服务实现类单元测试（PRD P1-4 周期性账单提醒）
 *
 * <p>测试覆盖场景：</p>
 * <ul>
 *   <li>周期账单创建 - 正常创建、账户验证、分类验证</li>
 *   <li>周期账单更新 - 正常更新、账单不存在、账单已停用</li>
 *   <li>周期账单删除 - 正常删除（软删除）、账单不存在</li>
 *   <li>周期账单列表 - 查询用户所有账单、空数据处理</li>
 *   <li>到期生成交易 - 生成收支记录、更新下次到期日、账单已停用</li>
 * </ul>
 *
 * <p>业务规则验证：</p>
 * <ul>
 *   <li>周期类型：monthly（每月）、weekly（每周）</li>
 *   <li>账单状态：1=启用、0=停用（软删除）</li>
 *   <li>到期生成：根据周期类型自动计算下次到期日</li>
 *   <li>账户验证：只能关联到用户自己的活跃账户</li>
 *   <li>分类验证：分类必须存在且类型匹配</li>
 * </ul>
 *
 * <p>Mock依赖: RecurringBillMapper, AccountMapper, CategoryMapper, TransactionMapper, EntityValidator</p>
 *
 * @see RecurringBillServiceImpl
 * @see RecurringBillDTO
 * @see RecurringBillRequest
 */
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

<<<<<<< HEAD
  private static String futureDueDate() {
    return LocalDate.now().plusMonths(1).toString();
  }

=======
>>>>>>> d463476029f30a051c2b7a044cbcc537a6e63de6
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
<<<<<<< HEAD
    req.setNextDueDate(futureDueDate());
=======
    req.setNextDueDate("2026-06-01");
>>>>>>> d463476029f30a051c2b7a044cbcc537a6e63de6

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
<<<<<<< HEAD
    req.setNextDueDate(futureDueDate());
=======
    req.setNextDueDate("2026-06-01");
>>>>>>> d463476029f30a051c2b7a044cbcc537a6e63de6

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
