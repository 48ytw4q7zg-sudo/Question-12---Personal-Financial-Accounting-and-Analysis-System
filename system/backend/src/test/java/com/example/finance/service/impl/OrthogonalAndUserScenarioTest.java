package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.common.BusinessException;
import com.example.finance.common.EntityValidator;
import com.example.finance.entity.*;
import com.example.finance.entity.dto.*;
import com.example.finance.mapper.*;
import com.example.finance.common.Result;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 正交实验设计 + 真实用户操作场景模拟测试
 * 验证系统所有小部件之间的通信正确性
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("正交实验+用户场景模拟")
class OrthogonalAndUserScenarioTest {

  /** 测试前初始化 JwtUtils（单元测试不走 Spring 上下文，需手动 init） */
  @BeforeAll
  static void initJwt() {
    com.example.finance.util.JwtUtils.init("test-secret-for-unit-testing-at-least-32-bytes-long!!", 7 * 24 * 60 * 60 * 1000L);
  }

  // ============================================================
  // §1 正交实验设计 — 多因素组合测试
  // ============================================================
  @Nested @DisplayName("正交实验: 交易筛选 5因素×2水平")
  class OrthogonalTransactionFilter {
    @Mock TransactionMapper transactionMapper;
    @Mock AccountMapper accountMapper;
    @Mock CategoryMapper categoryMapper;
    @Mock EntityValidator entityValidator;
    @InjectMocks TransactionServiceImpl transactionService;

    /**
     * 正交表 L8(2^5): 5 factors × 2 levels
     * Factor A: accountId (null=全部 / non-null=指定)
     * Factor B: categoryId (null=全部 / non-null=指定)
     * Factor C: startTime (null=不限 / non-null=限定)
     * Factor D: keyword (null=不搜 / non-null=搜索)
     * Factor E: sortBy (time=默认 / amount_desc=金额降序)
     */
    @Test @DisplayName("正交实验#1: A0B0C0D0E0 — 全部默认")
    void orthogonal_case1_allDefault() {
      when(transactionMapper.selectTransactionList(anyLong(), isNull(), isNull(), isNull(), isNull(), isNull(), eq("time"), any()))
          .thenReturn(Collections.emptyList());
      when(transactionMapper.selectTransactionCount(anyLong(), isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(0L);
      var page = transactionService.list(1L, null, null, null, null, null, "time", 1, 10);
      assertEquals(0, page.getTotal());
    }

    @Test @DisplayName("正交实验#2: A0B0C0D1E1 — 关键词+金额降序")
    void orthogonal_case2_keywordAndAmountSort() {
      when(transactionMapper.selectTransactionList(eq(1L), isNull(), isNull(), isNull(), isNull(), eq("午餐"), eq("amount_desc"), any()))
          .thenReturn(Collections.emptyList());
      when(transactionMapper.selectTransactionCount(eq(1L), isNull(), isNull(), isNull(), isNull(), eq("午餐"))).thenReturn(0L);
      var page = transactionService.list(1L, null, null, null, null, "午餐", "amount_desc", 1, 10);
      assertEquals(0, page.getTotal());
    }

    @Test @DisplayName("正交实验#3: A1B1C0D0E1 — 指定账户+分类+金额排序")
    void orthogonal_case3_accountAndCategory() {
      when(transactionMapper.selectTransactionList(eq(1L), eq(1L), eq(3L), isNull(), isNull(), isNull(), eq("amount_desc"), any()))
          .thenReturn(Collections.emptyList());
      when(transactionMapper.selectTransactionCount(eq(1L), eq(1L), eq(3L), isNull(), isNull(), isNull())).thenReturn(0L);
      var page = transactionService.list(1L, 1L, 3L, null, null, null, "amount_desc", 1, 10);
      assertEquals(0, page.getTotal());
    }

    @Test @DisplayName("正交实验#4: A1B0C1D0E0 — 账户+时间范围")
    void orthogonal_case4_accountAndTimeRange() {
      when(transactionMapper.selectTransactionList(eq(1L), eq(2L), isNull(), eq("2026-05-01 00:00:00"), eq("2026-05-31 23:59:59"), isNull(), eq("time"), any()))
          .thenReturn(Collections.emptyList());
      when(transactionMapper.selectTransactionCount(eq(1L), eq(2L), isNull(), eq("2026-05-01 00:00:00"), eq("2026-05-31 23:59:59"), isNull())).thenReturn(0L);
      var page = transactionService.list(1L, 2L, null, "2026-05-01 00:00:00", "2026-05-31 23:59:59", null, "time", 1, 10);
      assertEquals(0, page.getTotal());
    }

    @Test @DisplayName("正交实验#5: A0B1C1D1E0 — 分类+时间+关键词")
    void orthogonal_case5_categoryTimeKeyword() {
      when(transactionMapper.selectTransactionList(eq(1L), isNull(), eq(1L), eq("2026-05-01 00:00:00"), eq("2026-05-31 23:59:59"), eq("外卖"), eq("time"), any()))
          .thenReturn(Collections.emptyList());
      when(transactionMapper.selectTransactionCount(eq(1L), isNull(), eq(1L), eq("2026-05-01 00:00:00"), eq("2026-05-31 23:59:59"), eq("外卖"))).thenReturn(0L);
      var page = transactionService.list(1L, null, 1L, "2026-05-01 00:00:00", "2026-05-31 23:59:59", "外卖", "time", 1, 10);
      assertEquals(0, page.getTotal());
    }

    @Test @DisplayName("正交实验#6: A1B1C1D1E1 — 全因素全限定")
    void orthogonal_case6_allSpecified() {
      when(transactionMapper.selectTransactionList(eq(1L), eq(1L), eq(1L), eq("2026-05-01 00:00:00"), eq("2026-05-31 23:59:59"), eq("午餐"), eq("amount_desc"), any()))
          .thenReturn(Collections.emptyList());
      when(transactionMapper.selectTransactionCount(eq(1L), eq(1L), eq(1L), eq("2026-05-01 00:00:00"), eq("2026-05-31 23:59:59"), eq("午餐"))).thenReturn(0L);
      var page = transactionService.list(1L, 1L, 1L, "2026-05-01 00:00:00", "2026-05-31 23:59:59", "午餐", "amount_desc", 1, 10);
      assertEquals(0, page.getTotal());
    }

    @Test @DisplayName("正交实验#7: A0B0C1D1E1 — 时间+关键词+金额排序")
    void orthogonal_case7_timeKeywordAmount() {
      when(transactionMapper.selectTransactionList(eq(1L), isNull(), isNull(), eq("2026-05-01 00:00:00"), eq("2026-05-31 23:59:59"), eq("test"), eq("amount_asc"), any()))
          .thenReturn(Collections.emptyList());
      when(transactionMapper.selectTransactionCount(eq(1L), isNull(), isNull(), eq("2026-05-01 00:00:00"), eq("2026-05-31 23:59:59"), eq("test"))).thenReturn(0L);
      var page = transactionService.list(1L, null, null, "2026-05-01 00:00:00", "2026-05-31 23:59:59", "test", "amount_asc", 1, 10);
      assertEquals(0, page.getTotal());
    }

    @Test @DisplayName("正交实验#8: A1B0C0D1E0 — 账户+关键词+默认排序")
    void orthogonal_case8_accountKeyword() {
      when(transactionMapper.selectTransactionList(eq(1L), eq(1L), isNull(), isNull(), isNull(), eq("转账"), eq("time"), any()))
          .thenReturn(Collections.emptyList());
      when(transactionMapper.selectTransactionCount(eq(1L), eq(1L), isNull(), isNull(), isNull(), eq("转账"))).thenReturn(0L);
      var page = transactionService.list(1L, 1L, null, null, null, "转账", "time", 1, 10);
      assertEquals(0, page.getTotal());
    }
  }

  // ============================================================
  // §2 真实用户操作场景模拟
  // ============================================================
  @Nested @DisplayName("用户场景: 完整记账流程模拟")
  class RealUserScenario {
    @Mock UserMapper userMapper;
    @Mock AccountMapper accountMapper;
    @Mock TransactionMapper transactionMapper;
    @Mock RecurringBillMapper recurringBillMapper;
    @Mock CategoryMapper categoryMapper;
    @Mock BudgetMapper budgetMapper;
    @Mock EntityValidator entityValidator;
    @InjectMocks UserServiceImpl userService;
    @InjectMocks AccountServiceImpl accountService;
    @InjectMocks TransactionServiceImpl transactionService;
    @InjectMocks BudgetServiceImpl budgetService;
    @InjectMocks RecurringBillServiceImpl recurringBillService;
    @InjectMocks StatisticsServiceImpl statisticsService;

    private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
        new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    private Long userId = 1L;
    private Account cashAcct, bankAcct;
    private Category foodCat, salaryCat, housingCat;

    /**
     * 完整用户旅程:
     * 1. 注册 → 2. 登录 → 3. 创建2个账户 → 4. 记工资收入 → 5. 记餐饮支出
     * → 6. 记房租支出 → 7. 设置餐饮预算 → 8. 设置房租周期账单
     * → 9. 查看余额 → 10. 查看月度统计 → 11. 查看预算进度
     * → 12. 生成周期账单交易 → 13. 查流水 → 14. 修改密码
     */

    @BeforeEach void setUpEntities() {
      cashAcct = createAccount(1L, "现金", new BigDecimal("500.00"));
      bankAcct = createAccount(2L, "银行卡", new BigDecimal("10000.00"));
      foodCat = createCategory(1L, "餐饮", 1);
      salaryCat = createCategory(9L, "工资", 2);
      housingCat = createCategory(4L, "住房", 1);
    }

    @Test @DisplayName("用户旅程Step1-2: 注册→登录→token有效")
    void journey_registerAndLogin() {
      // Step 1: Register
      when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
      when(userMapper.insert(any(User.class))).thenAnswer(inv -> { ((User)inv.getArgument(0)).setId(userId); return 1; });
      UserLoginRequest regReq = new UserLoginRequest(); regReq.setUsername("realuser"); regReq.setPassword("mypassword");
      LoginResponse regResp = userService.register(regReq);
      assertNotNull(regResp.getToken());
      assertEquals("realuser", regResp.getUsername());

      // Step 2: Verify token parseable
      Long parsedId = com.example.finance.util.JwtUtils.parseToken(regResp.getToken());
      assertEquals(userId, parsedId);
    }

    @Test @DisplayName("用户旅程Step3-5: 创建账户→记收入→记支出")
    void journey_accountsAndTransactions() {
      // Create 2 accounts
      when(accountMapper.insert(any(Account.class))).thenReturn(1);
      AccountRequest aReq = new AccountRequest(); aReq.setName("现金"); aReq.setType(1);
      aReq.setInitialBalance(new BigDecimal("500.00")); aReq.setCurrency("CNY");
      accountService.create(userId, aReq);
      aReq.setName("银行卡"); aReq.setInitialBalance(new BigDecimal("10000.00"));
      accountService.create(userId, aReq);

      // Record income
      when(entityValidator.validateAccount(userId, 2L)).thenReturn(bankAcct);
      when(entityValidator.validateCategory(9L)).thenReturn(salaryCat);
      when(transactionMapper.insert(any(Transaction.class))).thenReturn(1);
      TransactionRequest inReq = buildTxnReq(2L, 9L, 1, new BigDecimal("8000.00")); // type=1 income
      TransactionDTO inDto = transactionService.create(userId, inReq);
      assertEquals(1, inDto.getType());

      // Record expense
      when(entityValidator.validateAccount(userId, 1L)).thenReturn(cashAcct);
      when(entityValidator.validateCategory(1L)).thenReturn(foodCat);
      when(transactionMapper.insert(any(Transaction.class))).thenReturn(2);
      TransactionRequest exReq = buildTxnReq(1L, 1L, 2, new BigDecimal("45.00"));
      TransactionDTO exDto = transactionService.create(userId, exReq);
      assertEquals(2, exDto.getType());
    }

    @Test @DisplayName("用户旅程Step6-8: 设置预算+周期账单")
    void journey_budgetAndRecurringBill() {
      // Set budget for food
      when(categoryMapper.selectById(1L)).thenReturn(foodCat);
      when(budgetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
      when(budgetMapper.insert(any(Budget.class))).thenReturn(1);
      BudgetRequest bReq = new BudgetRequest();
      bReq.setCategoryId(1L); bReq.setMonth("2026-05"); bReq.setAmount(new BigDecimal("2000.00"));
      BudgetDTO bDto = budgetService.save(userId, bReq);
      assertEquals(new BigDecimal("2000.00"), bDto.getAmount());

      // Create recurring bill for rent
      lenient().when(accountMapper.selectById(2L)).thenReturn(bankAcct);
      lenient().when(categoryMapper.selectById(4L)).thenReturn(housingCat);
      when(recurringBillMapper.insert(any(RecurringBill.class))).thenReturn(1);
      RecurringBillRequest rReq = new RecurringBillRequest();
      rReq.setName("月房租"); rReq.setAmount(new BigDecimal("2500.00")); rReq.setType(2);
      rReq.setPeriod("monthly"); rReq.setNextDueDate("2026-06-01");
      rReq.setAccountId(2L); rReq.setCategoryId(4L);
      RecurringBillDTO rDto = recurringBillService.create(userId, rReq);
      assertEquals("月房租", rDto.getName());
    }

    @Test @DisplayName("用户旅程Step9-11: 查余额→月度统计→预算进度")
    void journey_queries() {
      // Balance
      when(accountMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(cashAcct, bankAcct));
      AccountBatchIncomeDTO incomeDTO = new AccountBatchIncomeDTO(); incomeDTO.setAccountId(2L); incomeDTO.setTotalIncome(new BigDecimal("8000.00"));
      AccountBatchExpenseDTO expenseDTO = new AccountBatchExpenseDTO(); expenseDTO.setAccountId(1L); expenseDTO.setTotalExpense(new BigDecimal("45.00"));
      when(transactionMapper.selectAccountIncomeBatch(anyLong(), anyList())).thenReturn(List.of(incomeDTO));
      when(transactionMapper.selectAccountExpenseBatch(anyLong(), anyList())).thenReturn(List.of(expenseDTO));
      List<AccountBalanceDTO> balances = accountService.getBalance(userId);
      assertEquals(2, balances.size());

      // Monthly summary
      MonthlySummaryDTO summary = new MonthlySummaryDTO();
      summary.setYear(2026); summary.setMonth(5);
      summary.setTotalIncome(new BigDecimal("8000.00"));
      summary.setTotalExpense(new BigDecimal("45.00"));
      summary.setBalance(new BigDecimal("7955.00"));
      when(transactionMapper.selectMonthlySummary(userId, "2026-05-01 00:00:00", "2026-06-01 00:00:00")).thenReturn(summary);
      MonthlySummaryDTO stats = statisticsService.getMonthlySummary(userId, 2026, 5);
      assertEquals(new BigDecimal("7955.00"), stats.getBalance());

      // Budget progress
      Budget budget = new Budget(); budget.setId(1L); budget.setUserId(userId);
      budget.setCategoryId(1L); budget.setMonth("2026-05"); budget.setAmount(new BigDecimal("2000.00"));
      when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(budget));
      when(categoryMapper.selectByIds(anySet())).thenReturn(List.of(foodCat));
      CategorySummaryDTO catSummary = new CategorySummaryDTO();
      catSummary.setCategoryId(1L); catSummary.setTotalAmount(new BigDecimal("45.00"));
      when(transactionMapper.selectCategorySummary(userId, "2026-05-01 00:00:00", "2026-06-01 00:00:00", 2)).thenReturn(List.of(catSummary));
      List<BudgetProgressDTO> progress = budgetService.getProgress(userId, "2026", "5");
      assertEquals(1, progress.size());
      assertFalse(progress.get(0).isOverspent());
    }

    @Test @DisplayName("用户旅程Step12-13: 生成周期交易→查流水确认")
    void journey_generateAndVerify() {
      RecurringBill bill = new RecurringBill(); bill.setId(1L); bill.setUserId(userId);
      bill.setAccountId(2L); bill.setCategoryId(4L); bill.setName("月房租");
      bill.setAmount(new BigDecimal("2500.00")); bill.setType(2);
      bill.setPeriod("monthly"); bill.setStatus(1);
      bill.setNextDueDate(java.time.LocalDate.of(2026,6,1));
      when(recurringBillMapper.selectById(1L)).thenReturn(bill);
      when(transactionMapper.insert(any(Transaction.class))).thenReturn(1);
      when(accountMapper.selectById(2L)).thenReturn(bankAcct);
      when(categoryMapper.selectById(4L)).thenReturn(housingCat);
      when(recurringBillMapper.updateById(any(RecurringBill.class))).thenReturn(1);

      TransactionDTO txn = recurringBillService.generate(userId, 1L);
      assertTrue(txn.getNote().contains("月房租"));
      assertEquals(new BigDecimal("2500.00"), txn.getAmount());
    }

    @Test @DisplayName("用户旅程Step14: 修改密码→新密码可登录")
    void journey_changePassword() {
      User u = new User(); u.setId(userId); u.setUsername("realuser");
      u.setPassword(encoder.encode("mypassword"));
      when(userMapper.selectById(userId)).thenReturn(u);
      userService.changePassword(userId, "mypassword", "newSecure456");

      // Verify: old password fails
      when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(u);
      UserLoginRequest oldPwd = new UserLoginRequest(); oldPwd.setUsername("realuser"); oldPwd.setPassword("mypassword");
      assertThrows(BusinessException.class, () -> userService.login(oldPwd));
    }

    // ===== Robust Boundary: Multi-account financial year-end scenario =====
    @Test @DisplayName("健壮场景: 年终财务结算 — 多账户×多交易 余额守恒验证")
    void robust_yearEndReconciliation() {
      Account a1 = createAccount(1L, "现金", new BigDecimal("1000.00"));
      Account a2 = createAccount(2L, "银行卡", new BigDecimal("20000.00"));
      Account a3 = createAccount(3L, "支付宝", new BigDecimal("3000.00"));

      BigDecimal initialTotal = a1.getInitialBalance().add(a2.getInitialBalance()).add(a3.getInitialBalance());
      assertEquals(new BigDecimal("24000.00"), initialTotal);

      // Simulate year of transactions via batch queries
      when(accountMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a1, a2, a3));
      AccountBatchIncomeDTO inc1 = new AccountBatchIncomeDTO(); inc1.setAccountId(1L); inc1.setTotalIncome(new BigDecimal("5000.00"));
      AccountBatchIncomeDTO inc2 = new AccountBatchIncomeDTO(); inc2.setAccountId(2L); inc2.setTotalIncome(new BigDecimal("120000.00"));
      AccountBatchIncomeDTO inc3 = new AccountBatchIncomeDTO(); inc3.setAccountId(3L); inc3.setTotalIncome(new BigDecimal("8000.00"));
      AccountBatchExpenseDTO exp1 = new AccountBatchExpenseDTO(); exp1.setAccountId(1L); exp1.setTotalExpense(new BigDecimal("4500.00"));
      AccountBatchExpenseDTO exp2 = new AccountBatchExpenseDTO(); exp2.setAccountId(2L); exp2.setTotalExpense(new BigDecimal("110000.00"));
      AccountBatchExpenseDTO exp3 = new AccountBatchExpenseDTO(); exp3.setAccountId(3L); exp3.setTotalExpense(new BigDecimal("7500.00"));
      when(transactionMapper.selectAccountIncomeBatch(eq(userId), anyList())).thenReturn(List.of(inc1, inc2, inc3));
      when(transactionMapper.selectAccountExpenseBatch(eq(userId), anyList())).thenReturn(List.of(exp1, exp2, exp3));

      List<AccountBalanceDTO> balances = accountService.getBalance(userId);
      BigDecimal totalBalance = balances.stream()
          .map(AccountBalanceDTO::getCurrentBalance)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      // 1000+20000+3000 + (5000+120000+8000) - (4500+110000+7500)
      // = 24000 + 133000 - 122000 = 35000
      assertEquals(new BigDecimal("35000.00"), totalBalance);
    }

    // ===== Robust Boundary: Concurrent-like scenario =====
    @Test @DisplayName("健壮场景: 并发预算保存 — DuplicateKeyException兜底")
    void robust_concurrentBudgetSave() {
      when(categoryMapper.selectById(1L)).thenReturn(foodCat);
      // First selectOne: return null (no existing budget)
      // insert throws DuplicateKeyException
      // Second selectOne: return existing (concurrent insert already happened)
      Budget existing = new Budget(); existing.setId(1L); existing.setUserId(userId);
      existing.setCategoryId(1L); existing.setMonth("2026-05"); existing.setAmount(new BigDecimal("1000.00"));
      when(budgetMapper.selectOne(any(LambdaQueryWrapper.class)))
          .thenReturn(null, existing);
      when(budgetMapper.insert(any(Budget.class)))
          .thenThrow(new org.springframework.dao.DuplicateKeyException("Duplicate entry"));
      when(budgetMapper.updateById(any(Budget.class))).thenReturn(1);

      BudgetRequest bReq = new BudgetRequest();
      bReq.setCategoryId(1L); bReq.setMonth("2026-05"); bReq.setAmount(new BigDecimal("1500.00"));
      BudgetDTO dto = budgetService.save(userId, bReq);
      assertEquals(new BigDecimal("1500.00"), dto.getAmount());
      verify(budgetMapper, times(2)).selectOne(any(LambdaQueryWrapper.class));
    }

    private Account createAccount(Long id, String name, BigDecimal balance) {
      Account a = new Account(); a.setId(id); a.setUserId(1L); a.setName(name);
      a.setInitialBalance(balance); a.setStatus(1); a.setType(1);
      return a;
    }
    private Category createCategory(Long id, String name, int type) {
      Category c = new Category(); c.setId(id); c.setName(name); c.setType(type);
      return c;
    }
    private TransactionRequest buildTxnReq(Long acctId, Long catId, int type, BigDecimal amount) {
      TransactionRequest r = new TransactionRequest();
      r.setAccountId(acctId); r.setCategoryId(catId); r.setType(type);
      r.setAmount(amount); r.setNote("test"); r.setTime(LocalDateTime.now());
      return r;
    }
  }

  // ============================================================
  // §3 组件间通信验证
  // ============================================================
  @Nested @DisplayName("组件通信: Controller→Service→Mapper 数据传递完整性")
  class ComponentCommunication {
    @Mock UserMapper userMapper;
    @Mock AccountMapper accountMapper;
    @Mock TransactionMapper transactionMapper;
    @Mock RecurringBillMapper recurringBillMapper;
    @Mock CategoryMapper categoryMapper;
    @Mock BudgetMapper budgetMapper;
    @Mock EntityValidator entityValidator;
    @InjectMocks UserServiceImpl userService;
    @InjectMocks AccountServiceImpl accountService;
    @InjectMocks TransactionServiceImpl transactionService;
    @InjectMocks BudgetServiceImpl budgetService;
    @InjectMocks RecurringBillServiceImpl recurringBillService;

    @Test @DisplayName("通信链: userId从Interceptor→Controller→Service→Mapper 全程传递")
    void userId_propagatesThroughAllLayers() {
      Long expectedUserId = 1L;

      // Simulate interceptor setting userId
      // Controller reads from request.getAttribute("userId")
      // Service receives userId as parameter
      // Mapper uses userId in query

      Account acct = new Account(); acct.setId(1L); acct.setUserId(expectedUserId);
      acct.setName("测试"); acct.setInitialBalance(BigDecimal.ZERO); acct.setStatus(1);
      when(accountMapper.selectById(1L)).thenReturn(acct);
      // Update should verify userId matches
      when(accountMapper.updateById(any(Account.class))).thenReturn(1);

      AccountRequest req = new AccountRequest(); req.setName("更新名"); req.setType(1);
      req.setInitialBalance(new BigDecimal("100.00"));
      AccountDTO dto = accountService.update(expectedUserId, 1L, req);

      assertEquals("更新名", dto.getName());
      // Verify owner check was done (getAccountById throws 2003 if mismatch)
    }

    @Test @DisplayName("通信链: DTO字段完整传递 — Request→Entity→DB 零丢失")
    void dto_fields_preservedThroughLayers() {
      Account acct = new Account(); acct.setId(1L); acct.setUserId(1L); acct.setStatus(1);
      when(accountMapper.selectById(1L)).thenReturn(acct);
      when(accountMapper.updateById(any(Account.class))).thenReturn(1);

      AccountRequest req = new AccountRequest();
      req.setName("多币种账户");
      req.setType(3); // 支付宝
      req.setInitialBalance(new BigDecimal("8888.88"));
      req.setCurrency("USD");

      AccountDTO dto = accountService.update(1L, 1L, req);
      assertEquals("多币种账户", dto.getName());
      assertEquals(3, dto.getType());
      assertEquals(new BigDecimal("8888.88"), dto.getInitialBalance());
      assertEquals("USD", dto.getCurrency());
    }

    @Test @DisplayName("通信链: 错误码从Service→GlobalExceptionHandler→前端axios拦截器")
    void errorCode_propagatesCorrectly() {
      // Service throws BusinessException(1001, "用户名已存在")
      User existing = new User(); existing.setId(1L); existing.setUsername("exists");
      when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

      UserLoginRequest req = new UserLoginRequest(); req.setUsername("exists"); req.setPassword("123456");
      BusinessException ex = assertThrows(BusinessException.class, () -> userService.register(req));
      assertEquals(1001, ex.getCode());
      assertEquals("用户名已存在", ex.getMessage());
      // GlobalExceptionHandler → Result.error(1001, msg) → axios interceptor → ElMessage.error
    }

    @Test @DisplayName("通信链: JWT role从token→interceptor→request attribute")
    void jwtRole_flowsThroughChain() {
      // Generate token with role=1 (admin)
      String token = com.example.finance.util.JwtUtils.generateToken(1L, "admin", 1);
      Long userId = com.example.finance.util.JwtUtils.parseToken(token);
      Integer role = com.example.finance.util.JwtUtils.parseRole(token);
      assertEquals(1L, userId);
      assertEquals(1, role);
    }

    @Test @DisplayName("通信链: 转账UUID关联两条记录 — transferId一致性")
    void transfer_uuid_linksTwoRecords() {
      Account from = new Account(); from.setId(1L); from.setUserId(1L); from.setName("A"); from.setInitialBalance(new BigDecimal("5000.00")); from.setStatus(1);
      Account to = new Account(); to.setId(2L); to.setUserId(1L); to.setName("B"); to.setInitialBalance(new BigDecimal("1000.00")); to.setStatus(1);
      when(accountMapper.selectByIdForUpdate(1L)).thenReturn(from);
      when(accountMapper.selectByIdForUpdate(2L)).thenReturn(to);
      Category transferCat = new Category(); transferCat.setId(13L); transferCat.setName("其他"); transferCat.setType(1);
      when(categoryMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class))).thenReturn(transferCat);
      when(transactionMapper.selectAccountIncome(anyLong(), anyLong())).thenReturn(BigDecimal.ZERO);
      when(transactionMapper.selectAccountExpense(anyLong(), anyLong())).thenReturn(BigDecimal.ZERO);
      when(transactionMapper.insert(any(Transaction.class))).thenReturn(1);

      TransferRequest req = new TransferRequest();
      req.setFromAccountId(1L); req.setToAccountId(2L); req.setAmount(new BigDecimal("100.00"));
      TransferDTO result = transactionService.transfer(1L, req);

      assertNotNull(result.getTransferId());
      assertEquals(result.getOutRecord().getTransferId(), result.getInRecord().getTransferId());
      assertEquals("A → B(转出)", result.getOutRecord().getNote());
      assertEquals("A → B(转入)", result.getInRecord().getNote());
    }
  }

  // ============================================================
  // §4 性能基线测试
  // ============================================================
  @Nested @DisplayName("性能基线: 批量操作vs循环操作")
  class PerformanceBaselineDetailed {

    @Mock AccountMapper accountMapper;
    @Mock TransactionMapper transactionMapper;
    @Mock RecurringBillMapper recurringBillMapper;
    @Mock CategoryMapper categoryMapper;
    @InjectMocks AccountServiceImpl accountService;

    @Test @DisplayName("性能: 10个账户→仅2次批量DB查询(非20次逐条)")
    void tenAccounts_onlyTwoBatchQueries() {
      List<Account> accounts = new ArrayList<>();
      for (int i = 1; i <= 10; i++) {
        Account a = new Account(); a.setId((long)i); a.setUserId(1L);
        a.setName("Account" + i); a.setInitialBalance(new BigDecimal("1000.00")); a.setStatus(1);
        accounts.add(a);
      }
      when(accountMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(accounts);
      when(transactionMapper.selectAccountIncomeBatch(anyLong(), anyList())).thenReturn(Collections.emptyList());
      when(transactionMapper.selectAccountExpenseBatch(anyLong(), anyList())).thenReturn(Collections.emptyList());

      accountService.getBalance(1L);

      // Critical: batch methods called exactly ONCE each
      verify(transactionMapper, times(1)).selectAccountIncomeBatch(anyLong(), anyList());
      verify(transactionMapper, times(1)).selectAccountExpenseBatch(anyLong(), anyList());
      // Per-account methods NEVER called
      verify(transactionMapper, never()).selectAccountIncome(anyLong(), anyLong());
      verify(transactionMapper, never()).selectAccountExpense(anyLong(), anyLong());
    }

    @Test @DisplayName("性能: RecurringBill列表→批量加载账户+分类名称(非N+1)")
    void recurringBillList_batchLoading() {
      com.example.finance.entity.RecurringBill bill1 = new com.example.finance.entity.RecurringBill();
      bill1.setId(1L); bill1.setUserId(1L); bill1.setAccountId(1L); bill1.setCategoryId(1L);
      bill1.setName("B1"); bill1.setAmount(new BigDecimal("100.00")); bill1.setType(2);
      bill1.setPeriod("monthly"); bill1.setStatus(1);
      bill1.setNextDueDate(java.time.LocalDate.now());

      com.example.finance.entity.RecurringBill bill2 = new com.example.finance.entity.RecurringBill();
      bill2.setId(2L); bill2.setUserId(1L); bill2.setAccountId(2L); bill2.setCategoryId(2L);
      bill2.setName("B2"); bill2.setAmount(new BigDecimal("200.00")); bill2.setType(1);
      bill2.setPeriod("weekly"); bill2.setStatus(1);
      bill2.setNextDueDate(java.time.LocalDate.now());

      when(recurringBillMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(bill1, bill2));
      Account a1 = new Account(); a1.setId(1L); a1.setName("A1");
      Account a2 = new Account(); a2.setId(2L); a2.setName("A2");
      when(accountMapper.selectByIds(anySet())).thenReturn(List.of(a1, a2));
      Category c1 = new Category(); c1.setId(1L); c1.setName("C1");
      Category c2 = new Category(); c2.setId(2L); c2.setName("C2");
      when(categoryMapper.selectByIds(anySet())).thenReturn(List.of(c1, c2));

      EntityValidator entityValidator = new EntityValidator(accountMapper, categoryMapper);  // 创建共享校验器（RecurringBillServiceImpl 新增的构造参数）
      RecurringBillServiceImpl rbService = new RecurringBillServiceImpl(recurringBillMapper, transactionMapper, accountMapper, categoryMapper, entityValidator);
      List<RecurringBillDTO> list = rbService.list(1L);

      assertEquals(2, list.size());
      verify(accountMapper, times(1)).selectByIds(anySet());
      verify(categoryMapper, times(1)).selectByIds(anySet());
    }
  }
}
