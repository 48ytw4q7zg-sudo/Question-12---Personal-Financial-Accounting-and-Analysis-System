package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.common.BusinessException;
import com.example.finance.entity.Account;
import com.example.finance.entity.RecurringBill;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.dto.AccountBalanceDTO;
import com.example.finance.entity.dto.AccountDTO;
import com.example.finance.entity.dto.AccountRequest;
import com.example.finance.mapper.AccountMapper;
import com.example.finance.mapper.RecurringBillMapper;
import com.example.finance.mapper.TransactionMapper;
import com.example.finance.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 账户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

  private static final int STATUS_ACTIVE = 1;
  private static final int STATUS_INACTIVE = 0;

  private final AccountMapper accountMapper;
  private final TransactionMapper transactionMapper;
  private final RecurringBillMapper recurringBillMapper;

  /**
   * 查询用户所有账户（status=1）
   */
  @Override
  public List<AccountDTO> list(Long userId) {
    List<Account> accounts = accountMapper.selectList(
        new LambdaQueryWrapper<Account>()
            .eq(Account::getUserId, userId)
            .eq(Account::getStatus, STATUS_ACTIVE)
            .orderByDesc(Account::getCreateTime)
    );
    return accounts.stream().map(this::toDTO).toList();
  }

  /**
   * 创建账户
   */
  @Override
  public AccountDTO create(Long userId, AccountRequest request) {
    Account account = new Account();
    account.setUserId(userId);
    account.setName(request.getName());
    account.setType(request.getType());
    account.setInitialBalance(request.getInitialBalance());
    account.setCurrency(request.getCurrency() != null ? request.getCurrency() : "CNY");
    account.setStatus(STATUS_ACTIVE);
    account.setCreateTime(LocalDateTime.now());
    account.setUpdateTime(LocalDateTime.now());

    accountMapper.insert(account);
    return toDTO(account);
  }

  /**
   * 更新账户
   */
  @Override
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
   */
  @Override
  @Transactional
  public void delete(Long userId, Long accountId) {
    Account account = getAccountById(userId, accountId);

    // 检查是否有交易记录
    long transactionCount = transactionMapper.selectCount(
        new LambdaQueryWrapper<Transaction>()
            .eq(Transaction::getAccountId, accountId)
            .eq(Transaction::getUserId, userId)
    );
    if (transactionCount > 0) {
      throw new BusinessException(2002, "该账户下有 " + transactionCount + " 条收支记录，请先处理后再禁用");
    }

    // 检查是否有启用的周期性账单
    long billCount = recurringBillMapper.selectCount(
        new LambdaQueryWrapper<RecurringBill>()
            .eq(RecurringBill::getAccountId, accountId)
            .eq(RecurringBill::getUserId, userId)
            .eq(RecurringBill::getStatus, STATUS_ACTIVE)
    );
    if (billCount > 0) {
      throw new BusinessException(2002, "该账户下有 " + billCount + " 个活跃周期性账单，请先停用后再禁用");
    }

    // 软删除
    account.setStatus(STATUS_INACTIVE);
    account.setUpdateTime(LocalDateTime.now());
    accountMapper.updateById(account);
  }

  /**
   * 获取账户余额统计
   */
  @Override
  public List<AccountBalanceDTO> getBalance(Long userId) {
    List<Account> accounts = accountMapper.selectList(
        new LambdaQueryWrapper<Account>()
            .eq(Account::getUserId, userId)
            .eq(Account::getStatus, STATUS_ACTIVE)
    );

    // R-05-issue-5: 已修复 - 批量查询消除2N，改为2次DB(GROUP BY) + Map索引
    List<Long> accountIds = accounts.stream().map(Account::getId).toList();

    // 批量查询所有账户的收入和支出（各1次DB查询 · N+1消除）
    List<Map<String, Object>> incomeRows = accountIds.isEmpty() ? List.of()
        : transactionMapper.selectAccountIncomeBatch(userId, accountIds);
    List<Map<String, Object>> expenseRows = accountIds.isEmpty() ? List.of()
        : transactionMapper.selectAccountExpenseBatch(userId, accountIds);

    Map<Long, BigDecimal> incomeMap = new java.util.HashMap<>();
    for (Map<String, Object> row : incomeRows) {
      incomeMap.put(((Number) row.get("accountId")).longValue(), (BigDecimal) row.get("totalIncome"));
    }
    Map<Long, BigDecimal> expenseMap = new java.util.HashMap<>();
    for (Map<String, Object> row : expenseRows) {
      expenseMap.put(((Number) row.get("accountId")).longValue(), (BigDecimal) row.get("totalExpense"));
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
      dto.setCurrentBalance(account.getInitialBalance().add(totalIncome).subtract(totalExpense));

      result.add(dto);
    }
    return result;
  }

  /**
   * 根据ID查询账户（校验归属）
   */
  private Account getAccountById(Long userId, Long accountId) {
    Account account = accountMapper.selectById(accountId);
    if (account == null || !account.getUserId().equals(userId)) {
      throw new BusinessException(2003, "账户不存在");
    }
    return account;
  }

  /**
   * Entity → DTO 转换
   */
  private AccountDTO toDTO(Account account) {
    AccountDTO dto = new AccountDTO();
    BeanUtils.copyProperties(account, dto);
    return dto;
  }
}
