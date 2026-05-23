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
import java.util.Objects;
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

  /** 默认币种（与前端 constants/finance.js 的 CURRENCY_LIST 对齐，默认人民币） */
  private static final String DEFAULT_CURRENCY = "CNY";

  /**
   * 解析币种（空值/空字符串兜底为默认币种CNY）
   * <p>与前端 constants/finance.js 的 CURRENCY_LIST 对齐。</p>
   *
   * @param currency 原始币种字符串
   * @return 非空币种字符串（null或空字符串则返回 DEFAULT_CURRENCY）
   */
  private String resolveCurrency(String currency) {  // 解析币种（空值+空字符串+全空白保护）
    return (currency != null && !currency.trim().isEmpty()) ? currency : DEFAULT_CURRENCY;  // 非空且非空字符串且非全空白则返回原值，否则返回默认CNY
  }

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
    List<Account> accounts = accountMapper.selectList(  // 查询用户所有活跃账户
        new LambdaQueryWrapper<Account>()
            .eq(Account::getUserId, userId)            // 筛选当前用户
            .eq(Account::getStatus, Status.ACTIVE.getValue()) // 仅查活跃账户
            .orderByDesc(Account::getCreateTime)       // 按创建时间倒序
    );
    return accounts.stream().map(this::toDTO).toList();  // 转为DTO列表返回
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
    long sameNameCount = accountMapper.selectCount(  // 查询同用户下同名账户数
        new LambdaQueryWrapper<Account>()
            .eq(Account::getUserId, userId)            // 筛选当前用户
            .eq(Account::getName, request.getName())   // 筛选同名账户
            .eq(Account::getStatus, Status.ACTIVE.getValue()) // 仅查活跃账户
    );
    if (sameNameCount > 0) {  // 同名账户已存在
      throw new BusinessException(ErrorCode.ACCOUNT_NAME_DUPLICATE.getCode(), ErrorCode.ACCOUNT_NAME_DUPLICATE.getMsg());  // 抛出业务异常（使用ErrorCode统一管理错误信息）
    }

    Account account = new Account();  // 创建账户实体
    account.setUserId(userId);  // 设置归属用户
    account.setName(request.getName());  // 设置账户名称
    account.setType(request.getType());  // 设置账户类型
    account.setInitialBalance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO);  // 设置初始余额（null 安全：空值兜底为 0，防止后续 getBalance() 第 248 行 NPE）
    account.setCurrency(resolveCurrency(request.getCurrency()));  // 设置币种(空值兜底为CNY，调用 resolveCurrency 私有方法)
    account.setStatus(Status.ACTIVE.getValue());  // 设置状态为活跃
    account.setCreateTime(LocalDateTime.now());  // 设置创建时间
    account.setUpdateTime(LocalDateTime.now());  // 设置更新时间

    accountMapper.insert(account);  // 插入数据库
    return toDTO(account);  // 转为DTO返回
  }

  /**
   * 更新账户
   *
   * @param userId    当前用户 ID（JWT 解码获取）
   * @param accountId 要更新的账户 ID
   * @param request   账户更新请求（含名称、类型、初始余额、币种）
   * @return 更新后的账户 DTO
   * @throws BusinessException 2004 账户不存在（ErrorCode.ACCOUNT_NOT_FOUND · getAccountById() 第280行抛出）
   */
  @Override
  @Transactional
  public AccountDTO update(Long userId, Long accountId, AccountRequest request) {
    Account account = getAccountById(userId, accountId);  // 查询并校验归属

    account.setName(request.getName());  // 更新账户名称
    account.setType(request.getType());  // 更新账户类型
    // null安全防护：与 create() 保持一致，null时保留原值（避免将DB中的初始余额覆盖为NULL）
    account.setInitialBalance(request.getInitialBalance() != null ? request.getInitialBalance() : account.getInitialBalance());  // 更新初始余额（null→保留原值）
    account.setCurrency(request.getCurrency() != null ? request.getCurrency() : resolveCurrency(account.getCurrency()));  // 更新币种(空则保留原值，并确保原值也非null)
    account.setUpdateTime(LocalDateTime.now());  // 更新修改时间

    accountMapper.updateById(account);  // 写入数据库
    return toDTO(account);  // 转为DTO返回
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
    Account account = getAccountById(userId, accountId);  // 查询并校验归属

    // 检查是否有非转账的交易记录（转账记录不阻塞账户删除，因为转账是内部划转而非收支）
    long transactionCount = transactionMapper.selectCount(  // 查询关联收支记录数
        new LambdaQueryWrapper<Transaction>()
            .eq(Transaction::getAccountId, accountId)  // 筛选该账户的交易
            .eq(Transaction::getUserId, userId)        // 筛选当前用户
            .isNull(Transaction::getTransferId)        // 排除转账记录
    );
    if (transactionCount > 0) {  // 存在收支记录则拒绝删除
      throw new BusinessException(ErrorCode.ACCOUNT_HAS_TRANSACTIONS.getCode(), "该账户下有 " + transactionCount + " 条收支记录，请先处理后再禁用");  // 抛出业务异常（动态消息包含准确条数，便于用户决策）
    }

    // 检查是否有启用的周期性账单
    long billCount = recurringBillMapper.selectCount(  // 查询关联周期性账单数
        new LambdaQueryWrapper<RecurringBill>()
            .eq(RecurringBill::getAccountId, accountId)  // 筛选该账户的账单
            .eq(RecurringBill::getUserId, userId)        // 筛选当前用户
            .eq(RecurringBill::getStatus, Status.ACTIVE.getValue()) // 仅查活跃账单
    );
    if (billCount > 0) {  // 存在活跃周期性账单则拒绝删除
      throw new BusinessException(ErrorCode.ACCOUNT_HAS_RECURRING_BILLS.getCode(), "该账户下有 " + billCount + " 个活跃周期性账单，请先停用后再禁用");  // 抛出业务异常
    }

    // 软删除: 将状态改为停用
    account.setStatus(Status.DISABLED.getValue());  // 设置状态为停用
    account.setUpdateTime(LocalDateTime.now());  // 更新修改时间
    accountMapper.updateById(account);  // 写入数据库
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
    List<Account> accounts = accountMapper.selectList(  // 查询用户所有活跃账户
        new LambdaQueryWrapper<Account>()
            .eq(Account::getUserId, userId)            // 筛选当前用户
            .eq(Account::getStatus, Status.ACTIVE.getValue()) // 仅查活跃账户
    );

    // R-05-issue-5: 已修复 - 批量查询消除2N，改为2次DB(GROUP BY) + 类型化DTO索引
    List<Long> accountIds = accounts.stream().map(Account::getId).toList();  // 提取账户ID列表

    // 批量查询所有账户的收入和支出（各1次DB查询 · N+1消除 · 使用类型化DTO替代Map<String,Object>）
    List<AccountBatchIncomeDTO> incomeRows = accountIds.isEmpty() ? List.of()  // 空列表保护
        : transactionMapper.selectAccountIncomeBatch(userId, accountIds);  // 批量查询各账户收入
    List<AccountBatchExpenseDTO> expenseRows = accountIds.isEmpty() ? List.of()  // 空列表保护
        : transactionMapper.selectAccountExpenseBatch(userId, accountIds);  // 批量查询各账户支出

    // 类型化 DTO → Map<Long, BigDecimal> 转换（编译期类型安全，不再需要 Number 强转）
    Map<Long, BigDecimal> incomeMap = incomeRows.stream()  // 收入列表转Map
        .collect(Collectors.toMap(AccountBatchIncomeDTO::getAccountId, AccountBatchIncomeDTO::getTotalIncome));  // 账户ID→总收入
    Map<Long, BigDecimal> expenseMap = expenseRows.stream()  // 支出列表转Map
        .collect(Collectors.toMap(AccountBatchExpenseDTO::getAccountId, AccountBatchExpenseDTO::getTotalExpense));  // 账户ID→总支出

    // 提前一次性查询汇率数据，避免循环内逐账户重复查询
    // 异常保护：汇率服务失败时回退为CNY-only模式，不影响余额查询核心功能
    Map<String, BigDecimal> ratesInverseMap = new java.util.HashMap<>();  // 汇率逆映射(外币→CNY)
    if (!accountIds.isEmpty()) {  // 有账户才查汇率
      try {
        ExchangeRateDTO ratesData = exchangeRateService.getExchangeRates();  // 获取汇率数据
        if (ratesData.getRatesInverse() != null) {  // 逆汇率非空
          ratesInverseMap.putAll(ratesData.getRatesInverse());  // 填充逆汇率映射
        }
      } catch (Exception e) {  // 汇率服务异常兜底
        log.warn("汇率数据加载失败，CNY换算暂不可用: {}", e.getMessage());  // 仅打日志不中断
      }
    }

    List<AccountBalanceDTO> result = new ArrayList<>();  // 余额结果列表
    for (Account account : accounts) {  // 遍历所有账户
      AccountBalanceDTO dto = new AccountBalanceDTO();  // 创建余额DTO
      dto.setAccountId(account.getId());  // 设置账户ID
      dto.setAccountName(account.getName());  // 设置账户名称
      dto.setAccountType(account.getType());  // 设置账户类型
      dto.setInitialBalance(account.getInitialBalance());  // 设置初始余额

      BigDecimal totalIncome = incomeMap.getOrDefault(account.getId(), BigDecimal.ZERO);  // 获取总收入(默认0)
      BigDecimal totalExpense = expenseMap.getOrDefault(account.getId(), BigDecimal.ZERO);  // 获取总支出(默认0)
      dto.setTotalIncome(totalIncome);  // 设置总收入
      dto.setTotalExpense(totalExpense);  // 设置总支出
      // 防御性编程：防止数据库中 initialBalance 为 NULL（如旧数据迁移/直接 SQL 插入绕过 Service 层默认值）导致 NPE
      BigDecimal initialBalance = account.getInitialBalance() != null ? account.getInitialBalance() : BigDecimal.ZERO;  // 空值兜底：null视为0
      BigDecimal currentBalance = initialBalance.add(totalIncome).subtract(totalExpense);  // 当前余额 = 初始余额 + 总收入 - 总支出
      dto.setCurrentBalance(currentBalance);  // 设置当前余额

      // P2-4: 多币种余额换算为 CNY 等值
      String currency = account.getCurrency() != null ? account.getCurrency() : DEFAULT_CURRENCY;  // 获取币种(默认CNY)
      dto.setCurrency(currency);  // 设置币种
      if (DEFAULT_CURRENCY.equals(currency)) {  // 人民币无需换算
        dto.setCnyEquivalentBalance(currentBalance);  // 直接使用原始余额
      } else {  // 外币需换算
        BigDecimal rate = ratesInverseMap.get(currency);  // 获取逆汇率
        if (rate == null) {  // 汇率不存在
          log.warn("不支持币种 {} 的CNY换算，accountId={}, 将使用原始余额", currency, account.getId());  // 记录警告日志
        }
        dto.setCnyEquivalentBalance(rate != null ? currentBalance.multiply(rate) : currentBalance);  // 有汇率则乘以汇率，否则用原始余额
      }

      result.add(dto);  // 加入结果列表
    }
    return result;  // 返回所有账户余额
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
    Account account = accountMapper.selectById(accountId);  // 根据ID查询账户
    // 校验：账户存在 + 归属当前用户（Integer用Objects.equals比较值，避免引用比较bug和null userId NPE）
    if (account == null || !Objects.equals(account.getUserId(), userId)) {
      throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND.getCode(), ErrorCode.ACCOUNT_NOT_FOUND.getMsg());  // 抛出业务异常
    }
    return account;  // 返回账户实体
  }

  /**
   * Entity → DTO 转换
   *
   * @param account 账户实体
   * @return 账户DTO
   */
  private AccountDTO toDTO(Account account) {  // Entity→DTO转换
    AccountDTO dto = new AccountDTO();  // 创建DTO对象
    dto.setId(account.getId());  // 设置ID
    dto.setName(account.getName());  // 设置名称
    dto.setType(account.getType());  // 设置类型
    dto.setInitialBalance(account.getInitialBalance());  // 设置初始余额
    dto.setCurrency(resolveCurrency(account.getCurrency()));  // 设置币种(空值兜底为CNY，调用 resolveCurrency 私有方法)
    dto.setStatus(account.getStatus());  // 设置状态
    dto.setCreateTime(account.getCreateTime());  // 设置创建时间
    dto.setUpdateTime(account.getUpdateTime());  // 设置更新时间
    return dto;  // 返回DTO
  }
}
