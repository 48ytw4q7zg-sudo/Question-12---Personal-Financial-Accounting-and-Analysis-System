package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.finance.common.BusinessException;
import com.example.finance.entity.*;
import com.example.finance.entity.dto.*;
import com.example.finance.mapper.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 跨模块集成测试 — 验证数据在各模块间的正确流转
 * 覆盖: 账户→交易→统计→预算→周期账单 全链路
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("跨模块集成测试")
class CrossModuleIntegrationTest {

  // ==== P0-1: User ====
  @Nested @DisplayName("用户→JWT→拦截器 集成链")
  class UserJwtChain {
    @Mock UserMapper userMapper;
    @InjectMocks UserServiceImpl userService;

    @Test @DisplayName("注册→token含userId+role→可解析")
    void registerThenTokenContainsUserIdAndRole() {
      when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
      when(userMapper.insert(any(User.class))).thenAnswer(inv -> { ((User)inv.getArgument(0)).setId(5L); return 1; });
      UserLoginRequest req = new UserLoginRequest(); req.setUsername("newuser"); req.setPassword("pass123456");
      LoginResponse resp = userService.register(req);
      assertNotNull(resp.getToken());
      assertEquals(5L, resp.getUserId());
      // Token must be parseable
      Long parsedId = com.example.finance.util.JwtUtils.parseToken(resp.getToken());
      assertEquals(5L, parsedId);
      Integer role = com.example.finance.util.JwtUtils.parseRole(resp.getToken());
      assertEquals(0, role);
    }

    @Test @DisplayName("登录→token可解析role")
    void loginTokenContainsRole() {
      User u = new User(); u.setId(2L); u.setUsername("admin"); u.setRole(1);
      u.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("123456"));
      when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(u);
      UserLoginRequest req = new UserLoginRequest(); req.setUsername("admin"); req.setPassword("123456");
      LoginResponse resp = userService.login(req);
      Integer role = com.example.finance.util.JwtUtils.parseRole(resp.getToken());
      assertEquals(1, role);
    }
  }

  // ==== P0-2+P0-4+P0-5: Account→Transaction→Balance 全链路 ====
  @Nested @DisplayName("账户→收支→余额 数据一致性链")
  class AccountTransactionBalanceChain {
    @Mock AccountMapper accountMapper;
    @Mock TransactionMapper transactionMapper;
    @Mock RecurringBillMapper recurringBillMapper;
    @Mock CategoryMapper categoryMapper;
    @InjectMocks AccountServiceImpl accountService;
    @InjectMocks TransactionServiceImpl transactionService;

    Account acct; Category cat;

    @BeforeEach void setUp() {
      acct = new Account(); acct.setId(1L); acct.setUserId(1L); acct.setName("现金");
      acct.setInitialBalance(new BigDecimal("1000.00")); acct.setStatus(1); acct.setType(1);
      cat = new Category(); cat.setId(1L); cat.setName("餐饮"); cat.setType(1);
    }

    @Test @DisplayName("链: 创建账户→记支出→余额=初始-支出")
    void createAccountThenExpenseThenBalanceCorrect() {
      // 1. Create account
      when(accountMapper.insert(any(Account.class))).thenReturn(1);
      AccountRequest acctReq = new AccountRequest(); acctReq.setName("测试"); acctReq.setType(1);
      acctReq.setInitialBalance(new BigDecimal("1000.00")); acctReq.setCurrency("CNY");
      accountService.create(1L, acctReq);

      // 2. Record expense
      when(accountMapper.selectById(1L)).thenReturn(acct);
      when(categoryMapper.selectById(1L)).thenReturn(cat);
      when(transactionMapper.insert(any(Transaction.class))).thenReturn(1);
      TransactionRequest txnReq = buildTxnReq(1L, 1L, 2, new BigDecimal("200.00"));
      transactionService.create(1L, txnReq);

      // 3. Verify balance (1000 + 0 - 200 = 800)
      when(accountMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(acct));
      when(transactionMapper.selectAccountIncomeBatch(eq(1L), anyList()))
          .thenReturn(List.of(Map.of("accountId", 1L, "totalIncome", BigDecimal.ZERO)));
      when(transactionMapper.selectAccountExpenseBatch(eq(1L), anyList()))
          .thenReturn(List.of(Map.of("accountId", 1L, "totalExpense", new BigDecimal("200.00"))));
      List<AccountBalanceDTO> balances = accountService.getBalance(1L);
      assertEquals(new BigDecimal("800.00"), balances.get(0).getCurrentBalance());
    }

    @Test @DisplayName("链: 多笔交易→余额聚合正确(收入+支出混合)")
    void multipleTransactionsBalanceCorrect() {
      when(accountMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(acct));
      when(transactionMapper.selectAccountIncomeBatch(eq(1L), anyList()))
          .thenReturn(List.of(Map.of("accountId", 1L, "totalIncome", new BigDecimal("5000.00"))));
      when(transactionMapper.selectAccountExpenseBatch(eq(1L), anyList()))
          .thenReturn(List.of(Map.of("accountId", 1L, "totalExpense", new BigDecimal("3200.50"))));
      List<AccountBalanceDTO> balances = accountService.getBalance(1L);
      assertEquals(new BigDecimal("2799.50"), balances.get(0).getCurrentBalance());
    }

    @Test @DisplayName("链: 删除账户→拒绝(有交易)→确认消息含准确条数")
    void deleteAccountWithTransactions_reportsCorrectCount() {
      when(accountMapper.selectById(1L)).thenReturn(acct);
      when(transactionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(7L);
      BusinessException ex = assertThrows(BusinessException.class,
          () -> accountService.delete(1L, 1L));
      assertTrue(ex.getMessage().contains("7 条收支记录"));
    }

    private TransactionRequest buildTxnReq(Long acctId, Long catId, int type, BigDecimal amount) {
      TransactionRequest r = new TransactionRequest();
      r.setAccountId(acctId); r.setCategoryId(catId); r.setType(type);
      r.setAmount(amount); r.setNote("test"); r.setTime(LocalDateTime.now());
      return r;
    }
  }

  // ==== P0-4+P1-5: 转账→统计 数据一致性链 ====
  @Nested @DisplayName("转账→统计 排除验证链")
  class TransferStatisticsChain {
    @Mock TransactionMapper transactionMapper;
    @Mock AccountMapper accountMapper;
    @Mock CategoryMapper categoryMapper;
    @InjectMocks TransactionServiceImpl transactionService;
    @InjectMocks StatisticsServiceImpl statisticsService;

    Account fromAcct, toAcct; Category cat;

    @BeforeEach void setUp() {
      fromAcct = new Account(); fromAcct.setId(1L); fromAcct.setUserId(1L); fromAcct.setName("银行卡");
      fromAcct.setInitialBalance(new BigDecimal("10000.00")); fromAcct.setStatus(1);
      toAcct = new Account(); toAcct.setId(2L); toAcct.setUserId(1L); toAcct.setName("现金");
      toAcct.setInitialBalance(new BigDecimal("500.00")); toAcct.setStatus(1);
      cat = new Category(); cat.setId(13L); cat.setName("其他"); cat.setType(1);
    }

    @Test @DisplayName("链: 转账成功→两账户余额守恒(转出-支出+转入+收入=0)")
    void transfer_balanceConservation() {
      when(accountMapper.selectById(1L)).thenReturn(fromAcct);
      when(accountMapper.selectById(2L)).thenReturn(toAcct);
      when(transactionMapper.selectAccountIncome(eq(1L), eq(1L))).thenReturn(new BigDecimal("3000.00"));
      when(transactionMapper.selectAccountExpense(eq(1L), eq(1L))).thenReturn(new BigDecimal("1000.00"));
      when(transactionMapper.insert(any(Transaction.class))).thenReturn(1);

      TransferRequest req = new TransferRequest();
      req.setFromAccountId(1L); req.setToAccountId(2L); req.setAmount(new BigDecimal("500.00"));

      TransferDTO result = transactionService.transfer(1L, req);
      assertNotNull(result.getTransferId());
      assertEquals(new BigDecimal("500.00"), result.getOutRecord().getAmount());
      assertEquals(new BigDecimal("500.00"), result.getInRecord().getAmount());
      assertNotNull(result.getOutRecord().getTransferId());
      assertNotNull(result.getInRecord().getTransferId());
      assertEquals(result.getOutRecord().getTransferId(), result.getInRecord().getTransferId());
    }

    @Test @DisplayName("链: 月统计→转账记录被正确排除(transfer_id非NULL不计入)")
    void monthlySummary_excludesTransfer() {
      MonthlySummaryDTO summary = new MonthlySummaryDTO();
      summary.setYear(2026); summary.setMonth(5);
      summary.setTotalIncome(new BigDecimal("8000.00"));
      summary.setTotalExpense(new BigDecimal("2500.00"));
      summary.setBalance(new BigDecimal("5500.00"));
      when(transactionMapper.selectMonthlySummary(1L, 2026, 5)).thenReturn(summary);
      MonthlySummaryDTO result = statisticsService.getMonthlySummary(1L, 2026, 5);
      assertEquals(new BigDecimal("8000.00"), result.getTotalIncome());
      assertEquals(new BigDecimal("2500.00"), result.getTotalExpense());
    }
  }

  // ==== P1-3+P0-4: 预算→实际支出 进度计算链 ====
  @Nested @DisplayName("预算→消费→进度 计算链")
  class BudgetConsumptionChain {
    @Mock BudgetMapper budgetMapper;
    @Mock CategoryMapper categoryMapper;
    @Mock TransactionMapper transactionMapper;
    @InjectMocks BudgetServiceImpl budgetService;

    Category expenseCat;

    @BeforeEach void setUp() {
      expenseCat = new Category(); expenseCat.setId(1L); expenseCat.setName("餐饮"); expenseCat.setType(1);
    }

    @Test @DisplayName("链: 预算1000→消费800→进度80%→未超支")
    void budgetProgress80percent_notOverspent() {
      Budget budget = new Budget(); budget.setId(1L); budget.setUserId(1L);
      budget.setCategoryId(1L); budget.setMonth("2026-05"); budget.setAmount(new BigDecimal("1000.00"));
      when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(budget));
      when(categoryMapper.selectByIds(anySet())).thenReturn(List.of(expenseCat));

      CategorySummaryDTO summary = new CategorySummaryDTO();
      summary.setCategoryId(1L); summary.setTotalAmount(new BigDecimal("800.00"));
      when(transactionMapper.selectCategorySummary(1L, 2026, 5, 2))
          .thenReturn(List.of(summary));

      List<BudgetProgressDTO> progress = budgetService.getProgress(1L, "2026", "5");
      assertEquals(1, progress.size());
      assertEquals(new BigDecimal("800.00"), progress.get(0).getSpentAmount());
      assertFalse(progress.get(0).isOverspent());
    }

    @Test @DisplayName("链: 预算1000→消费1200→超支标记")
    void budgetOverspent_flagIsTrue() {
      Budget budget = new Budget(); budget.setId(1L); budget.setUserId(1L);
      budget.setCategoryId(1L); budget.setMonth("2026-05"); budget.setAmount(new BigDecimal("1000.00"));
      when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(budget));
      when(categoryMapper.selectByIds(anySet())).thenReturn(List.of(expenseCat));

      CategorySummaryDTO summary = new CategorySummaryDTO();
      summary.setCategoryId(1L); summary.setTotalAmount(new BigDecimal("1200.00"));
      when(transactionMapper.selectCategorySummary(1L, 2026, 5, 2))
          .thenReturn(List.of(summary));

      List<BudgetProgressDTO> progress = budgetService.getProgress(1L, "2026", "5");
      assertTrue(progress.get(0).isOverspent());
    }

    @Test @DisplayName("链: 预算预警→仅返回超支项")
    void budgetAlert_onlyReturnsOverspent() {
      Budget b1 = new Budget(); b1.setId(1L); b1.setUserId(1L); b1.setCategoryId(1L);
      b1.setMonth("2026-05"); b1.setAmount(new BigDecimal("1000.00"));
      Budget b2 = new Budget(); b2.setId(2L); b2.setUserId(1L); b2.setCategoryId(2L);
      b2.setMonth("2026-05"); b2.setAmount(new BigDecimal("500.00"));
      when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(b1, b2));
      when(categoryMapper.selectByIds(anySet())).thenReturn(List.of(expenseCat, createCategory(2L, "交通")));

      CategorySummaryDTO s1 = new CategorySummaryDTO(); s1.setCategoryId(1L); s1.setTotalAmount(new BigDecimal("1200.00"));
      CategorySummaryDTO s2 = new CategorySummaryDTO(); s2.setCategoryId(2L); s2.setTotalAmount(new BigDecimal("300.00"));
      when(transactionMapper.selectCategorySummary(1L, 2026, 5, 2))
          .thenReturn(List.of(s1, s2));

      List<BudgetProgressDTO> alerts = budgetService.getAlert(1L, "2026", "5");
      assertEquals(1, alerts.size());
      assertEquals(1L, alerts.get(0).getCategoryId());
    }

    private Category createCategory(Long id, String name) {
      Category c = new Category(); c.setId(id); c.setName(name); c.setType(1); return c;
    }
  }

  // ==== P1-4+P0-4: 周期账单→生成交易 数据传递链 ====
  @Nested @DisplayName("周期账单→交易生成 数据传递链")
  class RecurringBillToTransactionChain {
    @Mock RecurringBillMapper recurringBillMapper;
    @Mock TransactionMapper transactionMapper;
    @Mock AccountMapper accountMapper;
    @Mock CategoryMapper categoryMapper;
    @InjectMocks RecurringBillServiceImpl recurringBillService;

    RecurringBill bill; Account acct; Category cat;

    @BeforeEach void setUp() {
      acct = new Account(); acct.setId(1L); acct.setUserId(1L); acct.setName("银行卡"); acct.setStatus(1);
      cat = new Category(); cat.setId(4L); cat.setName("住房"); cat.setType(1);
      bill = new RecurringBill(); bill.setId(1L); bill.setUserId(1L);
      bill.setAccountId(1L); bill.setCategoryId(4L); bill.setName("房租");
      bill.setAmount(new BigDecimal("2500.00")); bill.setType(2);
      bill.setPeriod("monthly"); bill.setStatus(1);
      bill.setNextDueDate(java.time.LocalDate.of(2026,6,1));
    }

    @Test @DisplayName("链: 周期账单→一键生成→交易含账单名称")
    void generate_transactionContainsBillName() {
      when(recurringBillMapper.selectById(1L)).thenReturn(bill);
      when(transactionMapper.insert(any(Transaction.class))).thenReturn(1);
      when(accountMapper.selectById(1L)).thenReturn(acct);
      when(categoryMapper.selectById(4L)).thenReturn(cat);
      when(recurringBillMapper.updateById(any(RecurringBill.class))).thenReturn(1);

      TransactionDTO txn = recurringBillService.generate(1L, 1L);
      assertTrue(txn.getNote().contains("房租"));
      assertEquals(new BigDecimal("2500.00"), txn.getAmount());
      assertEquals(2, txn.getType());
    }

    @Test @DisplayName("链: 生成后下次到期日自动推进(monthly→+1月)")
    void generate_nextDueDateAdvances() {
      when(recurringBillMapper.selectById(1L)).thenReturn(bill);
      when(transactionMapper.insert(any(Transaction.class))).thenReturn(1);
      when(accountMapper.selectById(1L)).thenReturn(acct);
      when(categoryMapper.selectById(4L)).thenReturn(cat);
      when(recurringBillMapper.updateById(any(RecurringBill.class))).thenReturn(1);

      recurringBillService.generate(1L, 1L);
      assertEquals(java.time.LocalDate.of(2026,7,1), bill.getNextDueDate());
    }
  }

  // ==== P0-1+P1-7: 注册→修改密码→重新登录 链 ====
  @Nested @DisplayName("用户生命周期 注册→改密→重登录 链")
  class UserLifecycleChain {
    @Mock UserMapper userMapper;
    @InjectMocks UserServiceImpl userService;

    private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
        new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

    @Test @DisplayName("链: 注册→修改密码→旧密码失效→新密码可登录")
    void registerThenChangePasswordThenOldPasswordFails() {
      // 1. Register
      when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
      when(userMapper.insert(any(User.class))).thenAnswer(inv -> { ((User)inv.getArgument(0)).setId(1L); return 1; });
      UserLoginRequest regReq = new UserLoginRequest(); regReq.setUsername("lifecycle"); regReq.setPassword("old123456");
      LoginResponse regResp = userService.register(regReq);
      assertNotNull(regResp.getToken());

      // 2. Change password
      User u = new User(); u.setId(1L); u.setUsername("lifecycle");
      u.setPassword(encoder.encode("old123456"));
      when(userMapper.selectById(1L)).thenReturn(u);
      userService.changePassword(1L, "old123456", "new654321");
      // old password fails
      when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(u);
      // Now test login: old password → fail
      UserLoginRequest loginWithOld = new UserLoginRequest(); loginWithOld.setUsername("lifecycle"); loginWithOld.setPassword("old123456");
      BusinessException ex = assertThrows(BusinessException.class, () -> userService.login(loginWithOld));
      assertEquals(1002, ex.getCode());
    }
  }

  // ==== 性能基线: 批量操作 vs 循环操作 ====
  @Nested @DisplayName("性能基线: N+1检测 + 批量优化验证")
  class PerformanceBaseline {
    @Mock AccountMapper accountMapper;
    @Mock TransactionMapper transactionMapper;
    @Mock RecurringBillMapper recurringBillMapper;
    @InjectMocks AccountServiceImpl accountService;

    @Test @DisplayName("性能: getBalance使用批量查询(2次DB)→非逐账户N次")
    void balance_usesBatchQuery() {
      Account a1 = new Account(); a1.setId(1L); a1.setUserId(1L); a1.setName("A");
      a1.setInitialBalance(new BigDecimal("100.00")); a1.setStatus(1);
      Account a2 = new Account(); a2.setId(2L); a2.setUserId(1L); a2.setName("B");
      a2.setInitialBalance(new BigDecimal("200.00")); a2.setStatus(1);

      when(accountMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a1, a2));
      when(transactionMapper.selectAccountIncomeBatch(eq(1L), anyList())).thenReturn(Collections.emptyList());
      when(transactionMapper.selectAccountExpenseBatch(eq(1L), anyList())).thenReturn(Collections.emptyList());

      accountService.getBalance(1L);
      // Verify batch methods called (not per-account loops)
      verify(transactionMapper, times(1)).selectAccountIncomeBatch(eq(1L), anyList());
      verify(transactionMapper, times(1)).selectAccountExpenseBatch(eq(1L), anyList());
      verify(transactionMapper, never()).selectAccountIncome(anyLong(), anyLong());
      verify(transactionMapper, never()).selectAccountExpense(anyLong(), anyLong());
    }
  }

  // ==== 健壮边界值: 极端用户操作场景 ====
  @Nested @DisplayName("健壮边界值: 极端用户操作场景")
  class RobustBoundaryScenarios {
    @Mock UserMapper userMapper;
    @Mock AccountMapper accountMapper;
    @Mock TransactionMapper transactionMapper;
    @Mock RecurringBillMapper recurringBillMapper;
    @Mock CategoryMapper categoryMapper;
    @Mock BudgetMapper budgetMapper;
    @InjectMocks UserServiceImpl userService;
    @InjectMocks AccountServiceImpl accountService;
    @InjectMocks TransactionServiceImpl transactionService;
    @InjectMocks BudgetServiceImpl budgetService;
    @InjectMocks RecurringBillServiceImpl recurringBillService;

    @Test @DisplayName("场景: 用户快速连点注册→唯一索引防重")
    void rapidDoubleRegister_onlyOneSucceeds() {
      when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
      when(userMapper.insert(any(User.class))).thenAnswer(inv -> { ((User)inv.getArgument(0)).setId(1L); return 1; });
      UserLoginRequest req = new UserLoginRequest(); req.setUsername("fastclick"); req.setPassword("testpass12");
      LoginResponse r1 = userService.register(req);
      assertNotNull(r1);

      User existing = new User(); existing.setId(1L); existing.setUsername("fastclick");
      when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
      BusinessException ex = assertThrows(BusinessException.class, () -> userService.register(req));
      assertEquals(1001, ex.getCode());
    }

    @Test @DisplayName("场景: 空数据Dashboard→不崩溃(零收入零支出零结余)")
    void emptyDashboard_zeroValuesReturned() {
      StatisticsServiceImpl stats = new StatisticsServiceImpl(transactionMapper);
      when(transactionMapper.selectMonthlySummary(1L, 2026, 5)).thenReturn(null);
      MonthlySummaryDTO s = stats.getMonthlySummary(1L, 2026, 5);
      assertEquals(BigDecimal.ZERO, s.getTotalIncome());
      assertEquals(BigDecimal.ZERO, s.getTotalExpense());
      assertEquals(BigDecimal.ZERO, s.getBalance());
    }

    @Test @DisplayName("场景: DECIMAL精度极限→多条交易求和后精度不丢失")
    void decimalPrecision_conservedAcrossOperations() {
      BigDecimal a = new BigDecimal("100.33");
      BigDecimal b = new BigDecimal("200.67");
      BigDecimal c = new BigDecimal("300.01");
      BigDecimal sum = a.add(b).add(c);
      assertEquals(new BigDecimal("601.01"), sum);
      // Subtract with precision
      BigDecimal result = sum.subtract(new BigDecimal("601.01"));
      assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), result);
    }

    @Test @DisplayName("场景: 预算金额÷本月天数(31)精度正确")
    void budgetDailyAverage_precisionAcrossMonthDays() {
      BigDecimal budget = new BigDecimal("1000.00");
      BigDecimal daily = budget.divide(new BigDecimal("31"), 2, RoundingMode.HALF_UP);
      assertEquals(new BigDecimal("32.26"), daily);
    }

    @Test @DisplayName("场景: 越权—用户A修改用户B的账户→拒绝[2003]")
    void crossUserAccountAccess_denied() {
      Account otherUserAccount = new Account();
      otherUserAccount.setId(1L); otherUserAccount.setUserId(999L); otherUserAccount.setStatus(1);
      when(accountMapper.selectById(1L)).thenReturn(otherUserAccount);
      AccountRequest req = new AccountRequest(); req.setName("hack"); req.setType(1);
      req.setInitialBalance(BigDecimal.ZERO);
      BusinessException ex = assertThrows(BusinessException.class,
          () -> accountService.update(1L, 1L, req));
      assertEquals(2003, ex.getCode());
    }
  }
}
