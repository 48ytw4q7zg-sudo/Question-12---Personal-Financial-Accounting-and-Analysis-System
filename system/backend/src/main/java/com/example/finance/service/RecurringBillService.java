package com.example.finance.service;

import com.example.finance.entity.dto.RecurringBillDTO;
import com.example.finance.entity.dto.RecurringBillRequest;
import com.example.finance.entity.dto.TransactionDTO;

import java.util.List;

/**
 * 周期性账单服务接口（PRD P1-4 周期性账单提醒）
 */
public interface RecurringBillService {

  /**
   * 查询用户周期性账单列表（status=1 活跃账单）
   *
   * @param userId 当前用户 ID（JWT 解码获取）
   * @return 周期性账单列表（按创建时间倒序）
   */
  List<RecurringBillDTO> list(Long userId);

  /**
   * 创建周期性账单
   *
   * @param userId  当前用户 ID（JWT 解码获取）
   * @param request 账单创建请求（含名称、账户、分类、金额、类型、周期、到期日）
   * @return 新创建的周期性账单 DTO
   * @throws com.example.finance.common.BusinessException 5007 账户不存在（对齐 ErrorCode.BILL_ACCOUNT_NOT_FOUND）/ 5002 关联账户已禁用
   */
  RecurringBillDTO create(Long userId, RecurringBillRequest request);

  /**
   * 更新周期性账单
   *
   * @param userId  当前用户 ID（JWT 解码获取）
   * @param billId  要更新的账单 ID
   * @param request 账单更新请求（含名称、账户、分类、金额、类型、周期、到期日）
   * @return 更新后的周期性账单 DTO
   * @throws com.example.finance.common.BusinessException 5006 账单不存在（对齐 ErrorCode.BILL_NOT_FOUND）/ 5005 账单已停用（对齐 ErrorCode.BILL_INACTIVE）
   */
  RecurringBillDTO update(Long userId, Long billId, RecurringBillRequest request);

  /**
   * 停用周期性账单（软删除 · status=0，停用后不可恢复）
   *
   * @param userId 当前用户 ID（JWT 解码获取）
   * @param billId 要停用的账单 ID
   * @throws com.example.finance.common.BusinessException 5006 账单不存在（对齐 ErrorCode.BILL_NOT_FOUND）/ 5005 账单已停用（对齐 ErrorCode.BILL_INACTIVE）
   */
  void deactivate(Long userId, Long billId);

  /**
   * 根据周期性账单生成交易记录（手动触发）
   *
   * @param userId 当前用户 ID（JWT 解码获取）
   * @param billId 周期性账单 ID
   * @return 生成的交易记录 DTO
   * @throws com.example.finance.common.BusinessException 5006 账单不存在（对齐 ErrorCode.BILL_NOT_FOUND）/ 5005 账单已停用（对齐 ErrorCode.BILL_INACTIVE）/ 5002 关联账户已禁用
   */
  TransactionDTO generate(Long userId, Long billId);
}
