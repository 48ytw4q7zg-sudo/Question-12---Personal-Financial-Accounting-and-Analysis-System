package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.common.BusinessException;
import com.example.finance.common.EntityValidator;
import com.example.finance.entity.Account;
import com.example.finance.entity.Category;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.dto.TransactionDTO;
import com.example.finance.entity.dto.TransactionRequest;
import com.example.finance.entity.dto.TransferDTO;
import com.example.finance.entity.dto.TransferRequest;
import com.example.finance.mapper.AccountMapper;
import com.example.finance.mapper.CategoryMapper;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 交易服务实现类单元测试
 *
 * <p>测试覆盖场景：</p>
 * <ul>
 *   <li>创建收支记录 - 正常流程、账户校验、分类校验</li>
 *   <li>更新收支记录 - 正常更新、记录不存在、转账记录限制</li>
 *   <li>转账功能 - 自我转账、余额不足</li>
 * </ul>
 *
 * <p>使用 Mockito 模拟依赖：TransactionMapper、AccountMapper、CategoryMapper、EntityValidator</p>
 *
 * @see TransactionServiceImpl
 * @see TransactionRequest
 * @see TransferRequest
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

  @Mock
  private TransactionMapper transactionMapper;
  @Mock
  private AccountMapper accountMapper;
  @Mock
  private CategoryMapper categoryMapper;
  @Mock
  private EntityValidator entityValidator;

  @InjectMocks
  private TransactionServiceImpl transactionService;

  private Account testAccount;
  private Category testCategory;

  @BeforeEach
  void setUp() {
    testAccount = new Account();
    testAccount.setId(1L);
    testAccount.setUserId(1L);
    testAccount.setName("现金");
    testAccount.setInitialBalance(new BigDecimal("5000.00"));
    testAccount.setStatus(1);

    testCategory = new Category();
    testCategory.setId(1L);
    testCategory.setName("餐饮");
    testCategory.setType(1);

    // EntityValidator mock 在各测试方法中单独配置，避免 UnnecessaryStubbing
  }

  @Test
  @DisplayName("创建收支记录成功")
  void create_success() {
    when(entityValidator.validateAccount(1L, 1L)).thenReturn(testAccount);
    when(entityValidator.validateCategory(1L)).thenReturn(testCategory);
    when(transactionMapper.insert(any(Transaction.class))).thenReturn(1);

    TransactionRequest request = buildRequest(1L, 1L, 2, new BigDecimal("50.00"), "午餐", LocalDateTime.now());
    TransactionDTO dto = transactionService.create(1L, request);

    assertNotNull(dto);
    assertEquals(new BigDecimal("50.00"), dto.getAmount());
    verify(transactionMapper).insert(any(Transaction.class));
  }

  @Test
  @DisplayName("创建失败 - 账户不存在或已禁用")
  void create_accountInvalid() {
    when(entityValidator.validateAccount(1L, 999L)).thenThrow(new BusinessException(3004, "账户不存在或已禁用"));

    TransactionRequest request = buildRequest(999L, 1L, 2, new BigDecimal("50.00"), "test", LocalDateTime.now());
    BusinessException ex = assertThrows(BusinessException.class,
        () -> transactionService.create(1L, request));
    assertEquals(3004, ex.getCode());
  }

  @Test
  @DisplayName("创建失败 - 分类不存在")
  void create_categoryNotFound() {
    when(entityValidator.validateAccount(1L, 1L)).thenReturn(testAccount);
    when(entityValidator.validateCategory(999L)).thenThrow(new BusinessException(3005, "分类不存在"));

    TransactionRequest request = buildRequest(1L, 999L, 2, new BigDecimal("50.00"), "test", LocalDateTime.now());
    BusinessException ex = assertThrows(BusinessException.class,
        () -> transactionService.create(1L, request));
    assertEquals(3005, ex.getCode());
  }

  @Test
  @DisplayName("更新成功")
  void update_success() {
    Transaction existing = buildTransaction(1L, 1L, 1L, 2, new BigDecimal("50.00"), "old", null);
    when(transactionMapper.selectById(1L)).thenReturn(existing);
    when(entityValidator.validateAccount(1L, 1L)).thenReturn(testAccount);
    when(entityValidator.validateCategory(1L)).thenReturn(testCategory);
    when(transactionMapper.updateById(any(Transaction.class))).thenReturn(1);

    TransactionRequest request = buildRequest(1L, 1L, 2, new BigDecimal("80.00"), "updated", LocalDateTime.now());
    TransactionDTO dto = transactionService.update(1L, 1L, request);

    assertEquals(new BigDecimal("80.00"), dto.getAmount());
    assertEquals("updated", dto.getNote());
  }

  @Test
  @DisplayName("更新失败 - 记录不存在")
  void update_notFound() {
    when(transactionMapper.selectById(999L)).thenReturn(null);

    TransactionRequest request = buildRequest(1L, 1L, 2, new BigDecimal("50.00"), "test", LocalDateTime.now());
    BusinessException ex = assertThrows(BusinessException.class,
        () -> transactionService.update(1L, 999L, request));
    assertEquals(3011, ex.getCode());
  }

  @Test
  @DisplayName("更新转账记录 - 禁止修改金额")
  void update_transfer_cannotChangeAmount() {
    Transaction transferRecord = buildTransaction(1L, 1L, 13L, 2, new BigDecimal("200.00"), "转账", "uuid-123");
    when(transactionMapper.selectById(1L)).thenReturn(transferRecord);

    TransactionRequest request = buildRequest(1L, 13L, 2, new BigDecimal("300.00"), "转账", LocalDateTime.now());
    BusinessException ex = assertThrows(BusinessException.class,
        () -> transactionService.update(1L, 1L, request));
    assertEquals(3006, ex.getCode());
  }

  @Test
  @DisplayName("转账失败 - 不能转给自己")
  void transfer_sameAccount() {
    when(accountMapper.selectByIdForUpdate(1L)).thenReturn(testAccount);
    when(accountMapper.selectByIdForUpdate(1L)).thenReturn(testAccount);

    TransferRequest request = new TransferRequest();
    request.setFromAccountId(1L);
    request.setToAccountId(1L);
    request.setAmount(new BigDecimal("100.00"));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> transactionService.transfer(1L, request));
    assertEquals(3008, ex.getCode());
  }

  @Test
  @DisplayName("转账失败 - 余额不足")
  void transfer_insufficientBalance() {
    Account fromAccount = testAccount;
    Account toAccount = new Account();
    toAccount.setId(2L);
    toAccount.setUserId(1L);
    toAccount.setName("银行卡");
    toAccount.setInitialBalance(new BigDecimal("1000.00"));
    toAccount.setStatus(1);

    when(accountMapper.selectByIdForUpdate(1L)).thenReturn(fromAccount);
    when(accountMapper.selectByIdForUpdate(2L)).thenReturn(toAccount);
    // 账户当前余额 = 5000 + 0 - 0 = 5000，要转 10000 不够
    when(transactionMapper.selectAccountIncome(eq(1L), eq(1L))).thenReturn(BigDecimal.ZERO);
    when(transactionMapper.selectAccountExpense(eq(1L), eq(1L))).thenReturn(BigDecimal.ZERO);

    TransferRequest request = new TransferRequest();
    request.setFromAccountId(1L);
    request.setToAccountId(2L);
    request.setAmount(new BigDecimal("10000.00"));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> transactionService.transfer(1L, request));
    assertEquals(3009, ex.getCode());
  }

  private TransactionRequest buildRequest(Long accountId, Long categoryId, int type, BigDecimal amount, String note, LocalDateTime time) {
    TransactionRequest req = new TransactionRequest();
    req.setAccountId(accountId);
    req.setCategoryId(categoryId);
    req.setType(type);
    req.setAmount(amount);
    req.setNote(note);
    req.setTime(time);
    return req;
  }

  private Transaction buildTransaction(Long id, Long accountId, Long categoryId, int type, BigDecimal amount, String note, String transferId) {
    Transaction t = new Transaction();
    t.setId(id);
    t.setUserId(1L);
    t.setAccountId(accountId);
    t.setCategoryId(categoryId);
    t.setType(type);
    t.setAmount(amount);
    t.setNote(note);
    t.setTime(LocalDateTime.now());
    t.setTransferId(transferId);
    return t;
  }
}
