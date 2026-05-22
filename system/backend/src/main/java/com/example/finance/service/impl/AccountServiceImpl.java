package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.common.BusinessException;
import com.example.finance.common.ErrorCode;
import com.example.finance.common.enums.Status;
import com.example.finance.entity.Account;
import com.example.finance.entity.RecurringBill;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.dto.AccountBatchExpenseDTO;
import com.example.finance.entity.dto.AccountBatchIncomeDTO;
import com.example.finance.entity.dto.AccountBalanceDTO;
import com.example.finance.entity.dto.AccountDTO;
import com.example.finance.entity.dto.AccountRequest;
import com.example.finance.entity.dto.ExchangeRateDTO;
import com.example.finance.mapper.AccountMapper;
import com.example.finance.mapper.RecurringBillMapper;
import com.example.finance.mapper.TransactionMapper;
import com.example.finance.service.AccountService;
import com.example.finance.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 账户服务实现（PRD P0-2 账户 CRUD + P0-5 账户余额汇总）
 *
 * 关键业务规则：
 * - 删除前检查 transaction 和 recurring_bill 关联（拒绝级联删除）
 * - 余额实时计算 = 初始余额 + 收入 - 支出（批量查询消除 N+1）
 * - 软删除 status=0 后不可恢复
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

  /** 默认币种 */
  private static final String DEFAULT_CURRENCY = "CNY";

  /** → AccountMapper：账户 CRUD 数据访问 */
  private final AccountMapper accountMapper;
  /** → TransactionMapper：删除时检查关联交易记录 + 余额批量查询 */
  private final TransactionMapper transactionMapper;
  /** → RecurringBillMapper：删除时检查活跃周期性账单引用 */
  private final RecurringBillMapper recurringBillMapper;
  /** → ExchangeRateService：P2-4 多币种余额换算为 CNY 等值 */
  private final ExchangeRateService exchangeRateService;

  /**
   * 查询用户所有活跃账户（status=1）
   *
   * @param userId 当前用户 ID（JWT 解码）
   * @return 账户列表（按创建时间倒序）
   */
  @Override
  @Transactional(readOnly = true)
  public List<AccountDTO> list(Long userId) {
    List<Account> accounts = accountMapper.selectList(
        new LambdaQueryWrapper<Account>()
            .eq(Account::getUserId, userId)
            .eq(Account::getStatus, Status.ACTIVE.getValue())
            .orderByDesc(Account::getCreateTime)
    );
    return accounts.stream().map(this::toDTO).toList();
  }

  /**
   * 创建账户
   *
   * @param userId  当前用户 ID（JWT 解码获取）
   * @param request 账户创建请求（含名称、类型、初始余额、币种）
   * @return 新创建的账户 DTO
   */
  @Override
  @Transactional
  public AccountDTO create(Long userId, AccountRequest request) {
    // 校验同用户下账户名不重复（防止创建多个同名账户）
    long sameNameCount = accountMapper.selectCount(
        new LambdaQueryWrapper<Account>()
            .eq(Account::getUserId, userId)
            .eq(Account::getName, request.getName())
            .eq(Account::getStatus, Status.ACTIVE.getValue())
    );
    if (sameNameCount > 0) {
      throw new BusinessException(ErrorCode.ACCOUNT_NAME_EMPTY.getCode(), "账户名「" + request.getName() + "」已存在");
    }

    Account account = new Account();
    account.setUserId(userId);
    account.setName(request.getName());
    account.setType(request.getType());
    account.setInitialBalance(request.getInitialBalance());
    account.setCurrency(request.getCurrency() != null ? request.getCurrency() : DEFAULT_CURRENCY);
    account.setStatus(Status.ACTIVE.getValue());
    account.setCreateTime(LocalDateTime.now());
    account.setUpdateTime(LocalDateTime.now());

    accountMapper.insert(account);
    return toDTO(account);
  }

  /**
   * 更新账户
   *
   * @param userId    当前用户 ID（JWT 解码获取）
   * @param accountId 要更新的账户 ID
   * @param request   账户更新请求（含名称、类型、初始余额、币种）
   * @return 更新后的账户 DTO
   * @throws BusinessException 2003 账户不存在
   */
  @Override
  @Transactional
  public AccountDTO update(Long userId, Long accountId, AccountRequest request) {
    Account account = getAccountById(userId, accountId);

    account.setName(request.getName());
    account.setType(request.getType());
    account.setInitialBalance(request.getInitialBalance());
    account.setCurrency(request.getCurrency() != null ? request.getCurrency() : account.getCurrency());
    account.setUpdateTime(LocalDateTime.now());

    accountMapper.updateById(account);
    return toDTO(account);
  }

  /**
   * 删除账户（软删除 · @Transactional 保证检查和写入原子性）
   *
   * @param userId    当前用户 ID
   * @param accountId 要删除的账户 ID
   * @throws BusinessException 2004 账户不存在
   * @throws BusinessException 2002 该账户下有收支记录
   * @throws BusinessException 2003 该账户下有活跃周期性账单
   */
  @Override
  @Transactional
  public void delete(Long userId, Long accountId) {
    Account account = getAccountById(userId, accountId);

    // 检查是否有非转账的交易记录（转账记录不阻塞账户删除，因为转账是内部划转而非收支）
    long transactionCount = transactionMapper.selectCount(
        new LambdaQueryWrapper<Transaction>()
            .eq(Transaction::getAccountId, accountId)
            .eq(Transaction::getUserId, userId)
            .isNull(Transaction::getTransferId)
    );
    if (transactionCount > 0) {
      throw new BusinessException(ErrorCode.ACCOUNT_HAS_TRANSACTIONS.getCode(), "该账户下有 " + transactionCount + " 条收支记录，请先处理后再禁用");
    }

    // 检查是否有启用的周期性账单
    long billCount = recurringBillMapper.selectCount(
        new LambdaQueryWrapper<RecurringBill>()
            .eq(RecurringBill::getAccountId, accountId)
            .eq(RecurringBill::getUserId, userId)
            .eq(RecurringBill::getStatus, Status.ACTIVE.getValue())
    );
    if (billCount > 0) {
      throw new BusinessException(ErrorCode.ACCOUNT_HAS_RECURRING_BILLS.getCode(), "该账户下有 " + billCount + " 个活跃周期性账单，请先停用后再禁用");
    }

    // 软删除
    account.setStatus(Status.DISABLED.getValue());
    account.setUpdateTime(LocalDateTime.now());
    accountMapper.updateById(account);
  }

  /**
   * 获取账户余额统计（批量查询消除 N+1）
   *
   * 当前余额 = 初始余额 + 总收入 - 总支出（实时计算）
   * 优化策略：2 次 DB 查询（GROUP BY 批量聚合）+ Map 索引匹配
   *
   * @param userId 当前用户 ID（JWT 解码获取）
   * @return 各账户余额统计列表（含初始余额、总收入、总支出、当前余额）
   */
  @Override
  @Transactional(readOnly = true)
  public List<AccountBalanceDTO> getBalance(Long userId) {
    List<Account> accounts = accountMapper.selectList(
        new LambdaQueryWrapper<Account>()
            .eq(Account::getUserId, userId)
            .eq(Account::getStatus, Status.ACTIVE.getValue())
    );

    // R-05-issue-5: 已修复 - 批量查询消除2N，改为2次DB(GROUP BY) + 类型化DTO索引
    List<Long> accountIds = accounts.stream().map(Account::getId).toList();

    // 批量查询所有账户的收入和支出（各1次DB查询 · N+1消除 · 使用类型化DTO替代Map<String,Object>）
    List<AccountBatchIncomeDTO> incomeRows = accountIds.isEmpty() ? List.of()
        : transactionMapper.selectAccountIncomeBatch(userId, accountIds);
    List<AccountBatchExpenseDTO> expenseRows = accountIds.isEmpty() ? List.of()
        : transactionMapper.selectAccountExpenseBatch(userId, accountIds);

    // 类型化 DTO → Map<Long, BigDecimal> 转换（编译期类型安全，不再需要 Number 强转）
    Map<Long, BigDecimal> incomeMap = incomeRows.stream()
        .collect(Collectors.toMap(AccountBatchIncomeDTO::getAccountId, AccountBatchIncomeDTO::getTotalIncome));
    Map<Long, BigDecimal> expenseMap = expenseRows.stream()
        .collect(Collectors.toMap(AccountBatchExpenseDTO::getAccountId, AccountBatchExpenseDTO::getTotalExpense));

    // 提前一次性查询汇率数据，避免循环内逐账户重复查询
    // 异常保护：汇率服务失败时回退为CNY-only模式，不影响余额查询核心功能
    Map<String, BigDecimal> ratesInverseMap = new java.util.HashMap<>();
    if (!accountIds.isEmpty()) {
      try {
        ExchangeRateDTO ratesData = exchangeRateService.getExchangeRates();
        if (ratesData.getRatesInverse() != null) {
          ratesInverseMap.putAll(ratesData.getRatesInverse());
        }
      } catch (Exception e) {
        log.warn("汇率数据加载失败，CNY换算暂不可用: {}", e.getMessage());
      }
    }

    List<AccountBalanceDTO> result = new ArrayList<>();
    for (Account account : accounts) {
      AccountBalanceDTO dto = new AccountBalanceDTO();
      dto.setAccountId(account.getId());
      dto.setAccountName(account.getName());
      dto.setAccountType(account.getType());
      dto.setInitialBalance(account.getInitialBalance());

      BigDecimal totalIncome = incomeMap.getOrDefault(account.getId(), BigDecimal.ZERO);
      BigDecimal totalExpense = expenseMap.getOrDefault(account.getId(), BigDecimal.ZERO);
      dto.setTotalIncome(totalIncome);
      dto.setTotalExpense(totalExpense);
      BigDecimal currentBalance = account.getInitialBalance().add(totalIncome).subtract(totalExpense);
      dto.setCurrentBalance(currentBalance);

      // P2-4: 多币种余额换算为 CNY 等值
      String currency = account.getCurrency() != null ? account.getCurrency() : DEFAULT_CURRENCY;
      dto.setCurrency(currency);
      if (DEFAULT_CURRENCY.equals(currency)) {
        dto.setCnyEquivalentBalance(currentBalance);
      } else {
        BigDecimal rate = ratesInverseMap.get(currency);
        if (rate == null) {
          log.warn("不支持币种 {} 的CNY换算，accountId={}, 将使用原始余额", currency, account.getId());
        }
        dto.setCnyEquivalentBalance(rate != null ? currentBalance.multiply(rate) : currentBalance);
      }

      result.add(dto);
    }
    return result;
  }

  /**
   * 根据ID查询账户（校验归属权）
   *
   * @param userId    当前用户ID
   * @param accountId 要查询的账户ID
   * @return 账户实体
   * @throws BusinessException 2004 账户不存在
   */
  private Account getAccountById(Long userId, Long accountId) {
    Account account = accountMapper.selectById(accountId);
    if (account == null || !account.getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND.getCode(), ErrorCode.ACCOUNT_NOT_FOUND.getMsg());
    }
    return account;
  }

  /**
   * Entity → DTO 转换
   *
   * @param account 账户实体
   * @return 账户DTO
   */
  private AccountDTO toDTO(Account account) {
    AccountDTO dto = new AccountDTO();
    dto.setId(account.getId());
    dto.setName(account.getName());
    dto.setType(account.getType());
    dto.setInitialBalance(account.getInitialBalance());
    dto.setCurrency(account.getCurrency() != null ? account.getCurrency() : DEFAULT_CURRENCY);
    dto.setStatus(account.getStatus());
    dto.setCreateTime(account.getCreateTime());
    dto.setUpdateTime(account.getUpdateTime());
    return dto;
  }
}
