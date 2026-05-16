package com.example.finance.service;

import com.example.finance.entity.dto.AccountBalanceDTO;
import com.example.finance.entity.dto.AccountDTO;
import com.example.finance.entity.dto.AccountRequest;

import java.util.List;

/**
 * 账户服务接口
 */
public interface AccountService {

  /**
   * 查询用户所有账户（status=1）
   */
  List<AccountDTO> list(Long userId);

  /**
   * 创建账户
   */
  AccountDTO create(Long userId, AccountRequest request);

  /**
   * 更新账户
   */
  AccountDTO update(Long userId, Long accountId, AccountRequest request);

  /**
   * 删除账户（软删除）
   */
  void delete(Long userId, Long accountId);

  /**
   * 获取账户余额统计
   */
  List<AccountBalanceDTO> getBalance(Long userId);
}
