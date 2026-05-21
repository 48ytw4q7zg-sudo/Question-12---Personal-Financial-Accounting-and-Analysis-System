package com.example.finance.service;

import com.example.finance.entity.dto.AccountBalanceDTO;
import com.example.finance.entity.dto.AccountDTO;
import com.example.finance.entity.dto.AccountRequest;

import java.util.List;

/**
 * 账户服务接口（PRD P0-2 账户 CRUD + P0-5 账户余额汇总）
 */
public interface AccountService {

  /**
   * 查询用户所有活跃账户（status=1）
   *
   * @param userId 当前用户 ID（JWT 解码获取）
   * @return 账户列表（按创建时间倒序）
   */
  List<AccountDTO> list(Long userId);

  /**
   * 创建账户
   *
   * @param userId  当前用户 ID（JWT 解码获取）
   * @param request 账户创建请求（含名称、类型、初始余额、币种）
   * @return 新创建的账户 DTO
   */
  AccountDTO create(Long userId, AccountRequest request);

  /**
   * 更新账户
   *
   * @param userId    当前用户 ID（JWT 解码获取）
   * @param accountId 要更新的账户 ID
   * @param request   账户更新请求（含名称、类型、初始余额、币种）
   * @return 更新后的账户 DTO
   * @throws com.example.finance.common.BusinessException 2004 账户不存在
   */
  AccountDTO update(Long userId, Long accountId, AccountRequest request);

  /**
   * 删除账户（软删除 · 检查关联交易记录和周期性账单）
   *
   * @param userId    当前用户 ID（JWT 解码获取）
   * @param accountId 要删除的账户 ID
   * @throws com.example.finance.common.BusinessException 2002 账户下有交易记录或活跃周期性账单
   * @throws com.example.finance.common.BusinessException 2004 账户不存在
   */
  void delete(Long userId, Long accountId);

  /**
   * 获取账户余额统计（当前余额 = 初始余额 + 总收入 - 总支出）
   *
   * @param userId 当前用户 ID（JWT 解码获取）
   * @return 各账户余额统计列表（含初始余额、总收入、总支出、当前余额）
   */
  List<AccountBalanceDTO> getBalance(Long userId);
}
