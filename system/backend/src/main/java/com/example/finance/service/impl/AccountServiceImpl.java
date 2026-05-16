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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
   * 删除账户（软删除）
   */
  @Override
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

    // R-05-issue-5: 中 - getBalance循环中每账户2次DB查询(2N)，应改为批量查询
    List<AccountBalanceDTO> result = new ArrayList<>();
    for (Account account : accounts) {
      AccountBalanceDTO dto = new AccountBalanceDTO();
      dto.setAccountId(account.getId());
      dto.setAccountName(account.getName());
      dto.setAccountType(account.getType());
      dto.setInitialBalance(account.getInitialBalance());

      // 计算该账户的总收入和总支出
      BigDecimal totalIncome = transactionMapper.selectAccountIncome(userId, account.getId());
      BigDecimal totalExpense = transactionMapper.selectAccountExpense(userId, account.getId());

      dto.setTotalIncome(totalIncome);
      dto.setTotalExpense(totalExpense);
      // 当前余额 = 初始余额 + 收入 - 支出
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
