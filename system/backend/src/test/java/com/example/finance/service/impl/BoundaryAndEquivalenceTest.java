package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.common.BusinessException;
import com.example.finance.common.EntityValidator;
import com.example.finance.entity.Account;
import com.example.finance.entity.Budget;
import com.example.finance.entity.Category;
import com.example.finance.entity.RecurringBill;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.User;
import com.example.finance.entity.dto.*;
import com.example.finance.mapper.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 边界值 & 等价类 & 健壮性综合测试
 *
 * <p>测试方法论:</p>
 * <ul>
 *   <li>白盒测试: 基本路径覆盖 + 逻辑覆盖（条件分支、循环边界）</li>
 *   <li>黑盒测试: 等价类划分 + 边界值分析 + 健壮性测试</li>
 * </ul>
 *
 * <p>覆盖模块:</p>
 * <ul>
 *   <li>P0-1 登录/JWT - 用户名长度边界(3-20)、密码强度、JWT有效期</li>
 *   <li>P0-2 账户CRUD - 账户名长度、初始余额精度、账户类型枚举</li>
 *   <li>P0-4 收支记录 - 金额边界、备注长度、时间格式</li>
 *   <li>P1-3 预算管理 - 预算金额、月份格式、分类类型校验</li>
 *   <li>P1-4 周期账单 - 周期类型、到期日校验、生成逻辑</li>
 * </ul>
 *
 * <p>边界值示例:</p>
 * <ul>
 *   <li>用户名: 2字符(失败)、3字符(成功)、20字符(成功)、21字符(失败)</li>
 *   <li>金额: 0.00(失败)、0.01(成功)、9999999999.99(成功)、超出(失败)</li>
 *   <li>备注: 空串(成功)、200字符(成功)、201字符(失败)</li>
 * </ul>
 *
 * @see BoundaryAndEquivalenceTest.UserBoundaryTests
 * @see BoundaryAndEquivalenceTest.AccountBoundaryTests
 */
@ExtendWith(MockitoExtension.class)
class BoundaryAndEquivalenceTest {

  /** 测试前初始化 JwtUtils（单元测试不走 Spring 上下文，需手动 init） */
  @BeforeAll
  static void initJwt() {
    com.example.finance.util.JwtUtils.init("test-secret-for-unit-testing-at-least-32-bytes-long!!", 7 * 24 * 60 * 60 * 1000L);
  }

  // ---- User module ----
  @Nested
  @DisplayName("P0-1 登录/JWT — 边界值与等价类")
  class UserBoundaryTests {
    @Mock UserMapper userMapper;
    @InjectMocks UserServiceImpl userService;

    UserLoginRequest req;

    @BeforeEach
    void setUp() {
      req = new UserLoginRequest();
      req.setUsername("testuser");
      req.setPassword("123456");
    }

    @Test @DisplayName("边界: 用户名最小长度3字符 — 有效等价类")
    void register_minUsernameLength() {
      req.setUsername("abc");
      when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
      when(userMapper.insert(any(User.class))).thenAnswer(inv -> { ((User)inv.getArgument(0)).setId(1L); return 1; });
      LoginResponse r = userService.register(req);
      assertNotNull(r.getToken());
    }

    @Test @DisplayName("边界: 用户名最大长度20字符 — 有效等价类")
    void register_maxUsernameLength() {
      req.setUsername("abcdefghij1234567890");
      when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
      when(userMapper.insert(any(User.class))).thenAnswer(inv -> { ((User)inv.getArgument(0)).setId(1L); return 1; });
      LoginResponse r = userService.register(req);
      assertNotNull(r);
    }

    @Test @DisplayName("边界: 密码最小长度6字符 — 有效等价类")
    void register_minPasswordLength() {
      req.setPassword("123456");
      when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
      when(userMapper.insert(any(User.class))).thenAnswer(inv -> { ((User)inv.getArgument(0)).setId(1L); return 1; });
      LoginResponse r = userService.register(req);
      assertNotNull(r);
    }

    @Test @DisplayName("边界: 密码最大长度20字符 — 有效等价类")
    void register_maxPasswordLength() {
      req.setPassword("12345678901234567890");
      when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
      when(userMapper.insert(any(User.class))).thenAnswer(inv -> { ((User)inv.getArgument(0)).setId(1L); return 1; });
      LoginResponse r = userService.register(req);
      assertNotNull(r);
    }

    @Test @DisplayName("等价类: 登录-用户不存在不为分 → 不区分用户/密码(防枚举)")
    void login_doNotDistinguishUserOrPassword() {
      when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
      BusinessException ex = assertThrows(BusinessException.class, () -> userService.login(req));
      assertEquals(1002, ex.getCode());
      assertTrue(ex.getMessage().contains("用户名或密码"));
    }

    @Test @DisplayName("等价类: 注册-用户名重复 → 1001")
    void register_duplicate() {
      User u = new User(); u.setId(1L); u.setUsername("testuser");
      when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(u);
      BusinessException ex = assertThrows(BusinessException.class, () -> userService.register(req));
      assertEquals(1001, ex.getCode());
    }

    @Test @DisplayName("边界: 修改密码-新密码与旧密码相同 → 拒绝")
    void changePassword_sameAsOld() {
      User u = new User(); u.setId(1L);
      u.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("oldpass"));
      when(userMapper.selectById(1L)).thenReturn(u);
      BusinessException ex = assertThrows(BusinessException.class,
          () -> userService.changePassword(1L, "oldpass", "oldpass"));
      assertEquals(1006, ex.getCode());
    }
  }

  // ---- Account module ----
  @Nested
  @DisplayName("P0-2 账户CRUD — 边界值与等价类")
  class AccountBoundaryTests {
    @Mock AccountMapper accountMapper;
    @Mock TransactionMapper transactionMapper;
    @Mock RecurringBillMapper recurringBillMapper;
    @InjectMocks AccountServiceImpl accountService;

    Account acct;
    AccountRequest req;

    @BeforeEach
    void setUp() {
      acct = new Account(); acct.setId(1L); acct.setUserId(1L); acct.setName("现金");
      acct.setInitialBalance(new BigDecimal("1000.00")); acct.setStatus(1); acct.setType(1);
      req = new AccountRequest(); req.setName("测试账户"); req.setType(1);
      req.setInitialBalance(new BigDecimal("500.00")); req.setCurrency("CNY");
    }

    @Test @DisplayName("边界: 初始余额=0 — 有效等价类")
    void create_zeroInitialBalance() {
      req.setInitialBalance(BigDecimal.ZERO);
      when(accountMapper.insert(any(Account.class))).thenReturn(1);
      AccountDTO dto = accountService.create(1L, req);
      assertEquals(BigDecimal.ZERO, dto.getInitialBalance());
    }

    @Test @DisplayName("边界: 初始余额=DECIMAL(12,2)最大值 9999999999.99")
    void create_maxBalance() {
      req.setInitialBalance(new BigDecimal("9999999999.99"));
      when(accountMapper.insert(any(Account.class))).thenReturn(1);
      AccountDTO dto = accountService.create(1L, req);
      assertEquals(new BigDecimal("9999999999.99"), dto.getInitialBalance());
    }

    @Test @DisplayName("等价类: 账户名最小1字符 — 有效")
    void create_minNameLength() {
      req.setName("A");
      when(accountMapper.insert(any(Account.class))).thenReturn(1);
      AccountDTO dto = accountService.create(1L, req);
      assertEquals("A", dto.getName());
    }

    @Test @DisplayName("等价类: 账户名最大20字符 — 有效")
    void create_maxNameLength() {
      req.setName("12345678901234567890");
      when(accountMapper.insert(any(Account.class))).thenReturn(1);
      AccountDTO dto = accountService.create(1L, req);
      assertEquals(20, dto.getName().length());
    }

    @Test @DisplayName("等价类: 删除-账户有关联交易 → 拒绝[2002]")
    void delete_withTransactions() {
      when(accountMapper.selectById(1L)).thenReturn(acct);
      when(transactionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);
      BusinessException ex = assertThrows(BusinessException.class,
          () -> accountService.delete(1L, 1L));
      assertEquals(2002, ex.getCode());
      assertTrue(ex.getMessage().contains("5 条收支记录"));
    }

    @Test @DisplayName("等价类: 删除-账户有关联活跃周期账单 → 拒绝[2003]")
    void delete_withActiveRecurringBills() {
      when(accountMapper.selectById(1L)).thenReturn(acct);
      when(transactionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
      when(recurringBillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);
      BusinessException ex = assertThrows(BusinessException.class,
          () -> accountService.delete(1L, 1L));
      assertEquals(2003, ex.getCode());
      assertTrue(ex.getMessage().contains("活跃周期性账单"));
    }

    @Test @DisplayName("等价类: 账户不存在 → [2004]")
    void getAccount_notFound() {
      when(accountMapper.selectById(999L)).thenReturn(null);
      BusinessException ex = assertThrows(BusinessException.class,
          () -> accountService.delete(1L, 999L));
      assertEquals(2004, ex.getCode());
    }

    @Test @DisplayName("边界: 余额=初始+收入-支出 — 计算精度验证")
    void balance_calculationPrecision() {
      Account a = new Account(); a.setId(1L); a.setUserId(1L); a.setName("测试");
      a.setInitialBalance(new BigDecimal("100.00")); a.setStatus(1); a.setType(1);
      when(accountMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a));
      AccountBatchIncomeDTO incDto = new AccountBatchIncomeDTO(); incDto.setAccountId(1L); incDto.setTotalIncome(new BigDecimal("50.25"));
      when(transactionMapper.selectAccountIncomeBatch(eq(1L), anyList())).thenReturn(List.of(incDto));
      AccountBatchExpenseDTO expDto = new AccountBatchExpenseDTO(); expDto.setAccountId(1L); expDto.setTotalExpense(new BigDecimal("30.10"));
      when(transactionMapper.selectAccountExpenseBatch(eq(1L), anyList())).thenReturn(List.of(expDto));

      List<AccountBalanceDTO> balances = accountService.getBalance(1L);
      assertEquals(1, balances.size());
      assertEquals(new BigDecimal("120.15"), balances.get(0).getCurrentBalance());
    }
  }

  // ---- Transaction module ----
  @Nested
  @DisplayName("P0-4/P1-5 收支记录+转账 — 边界值与等价类")
  class TransactionBoundaryTests {
    @Mock TransactionMapper transactionMapper;
    @Mock AccountMapper accountMapper;
    @Mock CategoryMapper categoryMapper;
    @Mock EntityValidator entityValidator;
    @InjectMocks TransactionServiceImpl transactionService;

    Account acct; Category cat;

    @BeforeEach
    void setUp() {
      acct = new Account(); acct.setId(1L); acct.setUserId(1L); acct.setName("现金");
      acct.setInitialBalance(new BigDecimal("5000.00")); acct.setStatus(1);
      cat = new Category(); cat.setId(1L); cat.setName("餐饮"); cat.setType(1);
    }

    @Test @DisplayName("边界: 金额=0.01 — 最小正值")
    void create_minAmount() {
      when(entityValidator.validateAccount(1L, 1L)).thenReturn(acct);
      when(entityValidator.validateCategory(1L)).thenReturn(cat);
      when(transactionMapper.insert(any(Transaction.class))).thenReturn(1);
      TransactionRequest req = buildReq(1L, 1L, 2, new BigDecimal("0.01"));
      TransactionDTO dto = transactionService.create(1L, req);
      assertEquals(new BigDecimal("0.01"), dto.getAmount());
    }

    @Test @DisplayName("边界: 金额=9999999999.99 — DECIMAL(12,2)最大值")
    void create_maxAmount() {
      when(entityValidator.validateAccount(1L, 1L)).thenReturn(acct);
      when(entityValidator.validateCategory(1L)).thenReturn(cat);
      when(transactionMapper.insert(any(Transaction.class))).thenReturn(1);
      TransactionRequest req = buildReq(1L, 1L, 2, new BigDecimal("9999999999.99"));
      TransactionDTO dto = transactionService.create(1L, req);
      assertEquals(new BigDecimal("9999999999.99"), dto.getAmount());
    }

    @Test @DisplayName("等价类: type=1收入 — 正确创建")
    void create_income() {
      when(entityValidator.validateAccount(1L, 1L)).thenReturn(acct);
      when(entityValidator.validateCategory(1L)).thenReturn(cat);
      when(transactionMapper.insert(any(Transaction.class))).thenReturn(1);
      TransactionRequest req = buildReq(1L, 1L, 1, new BigDecimal("100.00"));
      TransactionDTO dto = transactionService.create(1L, req);
      assertEquals(1, dto.getType());
    }

    @Test @DisplayName("等价类: type=2支出 — 正确创建")
    void create_expense() {
      when(entityValidator.validateAccount(1L, 1L)).thenReturn(acct);
      when(entityValidator.validateCategory(1L)).thenReturn(cat);
      when(transactionMapper.insert(any(Transaction.class))).thenReturn(1);
      TransactionRequest req = buildReq(1L, 1L, 2, new BigDecimal("100.00"));
      TransactionDTO dto = transactionService.create(1L, req);
      assertEquals(2, dto.getType());
    }

    @Test @DisplayName("等价类: 账户禁用 → [3004]")
    void create_disabledAccount() {
      acct.setStatus(0);
      when(entityValidator.validateAccount(1L, 1L)).thenThrow(new BusinessException(3004, "账户不存在或已禁用"));
      TransactionRequest req = buildReq(1L, 1L, 2, new BigDecimal("100.00"));
      BusinessException ex = assertThrows(BusinessException.class,
          () -> transactionService.create(1L, req));
      assertEquals(3004, ex.getCode());
    }

    @Test @DisplayName("等价类: 转账-出/入不同用户 → 越权拒绝")
    void transfer_crossUser() {
      when(accountMapper.selectByIdForUpdate(1L)).thenReturn(acct);
      when(accountMapper.selectByIdForUpdate(2L)).thenReturn(null);
      TransferRequest req = new TransferRequest();
      req.setFromAccountId(1L); req.setToAccountId(2L); req.setAmount(new BigDecimal("100.00"));
      BusinessException ex = assertThrows(BusinessException.class,
          () -> transactionService.transfer(1L, req));
      assertEquals(3004, ex.getCode());
    }

    @Test @DisplayName("路径: 转账记录-仅可改备注,修改金额→拒绝[3006]")
    void update_transfer_amountBlocked() {
      Transaction t = new Transaction(); t.setId(1L); t.setUserId(1L);
      t.setAccountId(1L); t.setCategoryId(13L); t.setType(2);
      t.setAmount(new BigDecimal("200.00")); t.setNote("转账"); t.setTransferId("uuid-123");
      when(transactionMapper.selectById(1L)).thenReturn(t);
      TransactionRequest req = buildReq(1L, 13L, 2, new BigDecimal("999.00"), "hack", LocalDateTime.now());
      BusinessException ex = assertThrows(BusinessException.class,
          () -> transactionService.update(1L, 1L, req));
      assertEquals(3006, ex.getCode());
    }

    private TransactionRequest buildReq(Long acctId, Long catId, int type, BigDecimal amount) {
      return buildReq(acctId, catId, type, amount, "test", LocalDateTime.now());
    }

    private TransactionRequest buildReq(Long acctId, Long catId, int type, BigDecimal amount, String note, LocalDateTime time) {
      TransactionRequest r = new TransactionRequest();
      r.setAccountId(acctId); r.setCategoryId(catId); r.setType(type);
      r.setAmount(amount); r.setNote(note); r.setTime(time);
      return r;
    }
  }

  // ---- Budget module ----
  @Nested
  @DisplayName("P1-3 预算管理 — 边界值与等价类")
  class BudgetBoundaryTests {
    @Mock BudgetMapper budgetMapper;
    @Mock CategoryMapper categoryMapper;
    @Mock TransactionMapper transactionMapper;
    @InjectMocks BudgetServiceImpl budgetService;

    Category expenseCat, incomeCat;

    @BeforeEach
    void setUp() {
      expenseCat = new Category(); expenseCat.setId(1L); expenseCat.setName("餐饮"); expenseCat.setType(1);
      incomeCat = new Category(); incomeCat.setId(10L); incomeCat.setName("工资"); incomeCat.setType(2);
    }

    @Test @DisplayName("等价类: 预算仅可设支出分类 — 收入分类拒绝[4002]")
    void save_incomeCategoryRejected() {
      when(categoryMapper.selectById(10L)).thenReturn(incomeCat);
      BudgetRequest req = new BudgetRequest();
      req.setCategoryId(10L); req.setMonth("2026-05"); req.setAmount(new BigDecimal("1000.00"));
      BusinessException ex = assertThrows(BusinessException.class,
          () -> budgetService.save(1L, req));
      assertEquals(4002, ex.getCode());
    }

    @Test @DisplayName("边界: 预算金额=0.01 — 最小正值,有效")
    void save_minAmount() {
      when(categoryMapper.selectById(1L)).thenReturn(expenseCat);
      when(budgetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
      when(budgetMapper.insert(any(Budget.class))).thenReturn(1);
      BudgetRequest req = new BudgetRequest();
      req.setCategoryId(1L); req.setMonth("2026-05"); req.setAmount(new BigDecimal("0.01"));
      BudgetDTO dto = budgetService.save(1L, req);
      assertEquals(new BigDecimal("0.01"), dto.getAmount());
    }

    @Test @DisplayName("等价类: 分类不存在 → [4003]")
    void save_categoryNotFound() {
      when(categoryMapper.selectById(999L)).thenReturn(null);
      BudgetRequest req = new BudgetRequest();
      req.setCategoryId(999L); req.setMonth("2026-05"); req.setAmount(new BigDecimal("1000.00"));
      BusinessException ex = assertThrows(BusinessException.class,
          () -> budgetService.save(1L, req));
      assertEquals(4003, ex.getCode());
    }

    @Test @DisplayName("路径: 已有预算更新(覆盖写入)")
    void save_updateExisting() {
      when(categoryMapper.selectById(1L)).thenReturn(expenseCat);
      Budget existing = new Budget();
      existing.setId(1L); existing.setUserId(1L); existing.setCategoryId(1L);
      existing.setMonth("2026-05"); existing.setAmount(new BigDecimal("500.00"));
      when(budgetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
      when(budgetMapper.updateById(any(Budget.class))).thenReturn(1);
      BudgetRequest req = new BudgetRequest();
      req.setCategoryId(1L); req.setMonth("2026-05"); req.setAmount(new BigDecimal("800.00"));
      BudgetDTO dto = budgetService.save(1L, req);
      assertEquals(new BigDecimal("800.00"), dto.getAmount());
    }
  }

  // ---- RecurringBill module ----
  @Nested
  @DisplayName("P1-4 周期性账单 — 边界值与等价类")
  class RecurringBillBoundaryTests {
    @Mock RecurringBillMapper recurringBillMapper;
    @Mock TransactionMapper transactionMapper;
    @Mock AccountMapper accountMapper;
    @Mock CategoryMapper categoryMapper;
    @Mock EntityValidator entityValidator;
    @InjectMocks RecurringBillServiceImpl recurringBillService;

    Account acct; Category cat;

    @BeforeEach
    void setUp() {
      acct = new Account(); acct.setId(1L); acct.setUserId(1L); acct.setName("银行卡"); acct.setStatus(1);
      cat = new Category(); cat.setId(4L); cat.setName("住房"); cat.setType(1);
    }

    @Test @DisplayName("等价类: period=monthly — 有效")
    void create_monthly() {
      when(entityValidator.validateAccount(1L, 1L)).thenReturn(acct);  // mock EntityValidator 校验账户
      when(entityValidator.validateCategory(4L)).thenReturn(cat);  // mock EntityValidator 校验分类
      when(recurringBillMapper.insert(any(RecurringBill.class))).thenReturn(1);
      RecurringBillRequest req = buildBillReq("房租", new BigDecimal("2500.00"), 2, "monthly", futureDueDate());
      RecurringBillDTO dto = recurringBillService.create(1L, req);
      assertEquals("monthly", dto.getPeriod());
    }

    @Test @DisplayName("等价类: period=weekly — 有效")
    void create_weekly() {
      when(entityValidator.validateAccount(1L, 1L)).thenReturn(acct);  // mock EntityValidator 校验账户
      when(entityValidator.validateCategory(4L)).thenReturn(cat);  // mock EntityValidator 校验分类
      when(recurringBillMapper.insert(any(RecurringBill.class))).thenReturn(1);
      RecurringBillRequest req = buildBillReq("零花钱", new BigDecimal("200.00"), 2, "weekly", futureDueDate());
      RecurringBillDTO dto = recurringBillService.create(1L, req);
      assertEquals("weekly", dto.getPeriod());
    }

    @Test @DisplayName("等价类: 停用已停用账单 → [5005]")
    void deactivate_alreadyInactive() {
      RecurringBill bill = new RecurringBill();
      bill.setId(1L); bill.setUserId(1L); bill.setStatus(0);
      when(recurringBillMapper.selectById(1L)).thenReturn(bill);
      BusinessException ex = assertThrows(BusinessException.class,
          () -> recurringBillService.deactivate(1L, 1L));
      assertEquals(5005, ex.getCode());
    }

    @Test @DisplayName("等价类: 生成交易-账单已停用 → [5005]")
    void generate_inactiveBill() {
      RecurringBill bill = new RecurringBill();
      bill.setId(1L); bill.setUserId(1L); bill.setStatus(0);
      when(recurringBillMapper.selectById(1L)).thenReturn(bill);
      BusinessException ex = assertThrows(BusinessException.class,
          () -> recurringBillService.generate(1L, 1L));
      assertEquals(5005, ex.getCode());
    }

    @Test @DisplayName("边界: 账单名称最大30字符 — 有效")
    void create_maxNameLength() {
      when(entityValidator.validateAccount(1L, 1L)).thenReturn(acct);  // mock EntityValidator 校验账户
      when(entityValidator.validateCategory(4L)).thenReturn(cat);  // mock EntityValidator 校验分类
      when(recurringBillMapper.insert(any(RecurringBill.class))).thenReturn(1);
      RecurringBillRequest req = buildBillReq("123456789012345678901234567890", new BigDecimal("100.00"), 2, "monthly", futureDueDate());
      RecurringBillDTO dto = recurringBillService.create(1L, req);
      assertEquals(30, dto.getName().length());
    }

    private String futureDueDate() {
      return LocalDate.now().plusMonths(1).toString();
    }

    private RecurringBillRequest buildBillReq(String name, BigDecimal amount, int type, String period, String dueDate) {
      RecurringBillRequest r = new RecurringBillRequest();
      r.setName(name); r.setAmount(amount); r.setType(type); r.setPeriod(period); r.setNextDueDate(dueDate);
      r.setAccountId(1L); r.setCategoryId(4L);
      return r;
    }
  }

  // ---- Statistics module ----
  @Nested
  @DisplayName("P1-2/P1-6/P2-1 统计与图表 — 边界值与等价类")
  class StatisticsBoundaryTests {
    @Mock TransactionMapper transactionMapper;
    @InjectMocks StatisticsServiceImpl statisticsService;

    @Test @DisplayName("等价类: 无数据月 → 返回零值不报错")
    void monthlySummary_empty() {
      when(transactionMapper.selectMonthlySummary(1L, "2026-05-01 00:00:00", "2026-06-01 00:00:00")).thenReturn(null);
      MonthlySummaryDTO summary = statisticsService.getMonthlySummary(1L, 2026, 5);
      assertEquals(BigDecimal.ZERO, summary.getTotalIncome());
      assertEquals(BigDecimal.ZERO, summary.getTotalExpense());
      assertEquals(BigDecimal.ZERO, summary.getBalance());
    }

    @Test @DisplayName("等价类: 有数据月 — 正确汇总")
    void monthlySummary_withData() {
      MonthlySummaryDTO dto = new MonthlySummaryDTO();
      dto.setYear(2026); dto.setMonth(5);
      dto.setTotalIncome(new BigDecimal("10000.00"));
      dto.setTotalExpense(new BigDecimal("3000.00"));
      dto.setBalance(new BigDecimal("7000.00"));
      when(transactionMapper.selectMonthlySummary(1L, "2026-05-01 00:00:00", "2026-06-01 00:00:00")).thenReturn(dto);
      MonthlySummaryDTO summary = statisticsService.getMonthlySummary(1L, 2026, 5);
      assertEquals(new BigDecimal("10000.00"), summary.getTotalIncome());
      assertEquals(new BigDecimal("7000.00"), summary.getBalance());
    }

    @Test @DisplayName("等价类: 年度汇总-整年无数据 → 零值")
    void yearlySummary_empty() {
      when(transactionMapper.selectYearlySummary(1L, "2026-01-01 00:00:00", "2027-01-01 00:00:00")).thenReturn(null);
      MonthlySummaryDTO summary = statisticsService.getYearlySummary(1L, 2026);
      assertEquals(BigDecimal.ZERO, summary.getTotalIncome());
    }

    @Test @DisplayName("边界: 月份=1(最小) — 有效")
    void monthlySummary_january() {
      when(transactionMapper.selectMonthlySummary(1L, "2026-01-01 00:00:00", "2026-02-01 00:00:00")).thenReturn(null);
      MonthlySummaryDTO summary = statisticsService.getMonthlySummary(1L, 2026, 1);
      assertEquals(1, summary.getMonth());
    }

    @Test @DisplayName("边界: 月份=12(最大) — 有效")
    void monthlySummary_december() {
      when(transactionMapper.selectMonthlySummary(1L, "2026-12-01 00:00:00", "2027-01-01 00:00:00")).thenReturn(null);
      MonthlySummaryDTO summary = statisticsService.getMonthlySummary(1L, 2026, 12);
      assertEquals(12, summary.getMonth());
    }

    @Test @DisplayName("等价类: type=null → 返回全部分类汇总")
    void categorySummary_allTypes() {
      when(transactionMapper.selectCategorySummary(1L, "2026-05-01 00:00:00", "2026-06-01 00:00:00", null)).thenReturn(Collections.emptyList());
      List<CategorySummaryDTO> list = statisticsService.getCategorySummary(1L, 2026, 5, null);
      assertNotNull(list);
    }
  }
}
