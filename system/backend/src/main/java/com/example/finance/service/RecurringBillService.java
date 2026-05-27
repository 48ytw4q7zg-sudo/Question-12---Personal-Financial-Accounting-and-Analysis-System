package com.example.finance.service;

import com.example.finance.entity.dto.RecurringBillDTO;
import com.example.finance.entity.dto.RecurringBillRequest;
import com.example.finance.entity.dto.TransactionDTO;

import java.util.List;

/**
 * 周期性账单服务接口（PRD P1-4 周期性账单提醒）
 *
 * <p>错误码参考（实现类：RecurringBillServiceImpl.java，校验器：EntityValidator.java）：</p>
 * <ul>
 *   <li>3004 ACCOUNT_NOT_FOUND_OR_DISABLED — EntityValidator.validateAccount() 抛出（账户不存在或归属不匹配或已禁用）</li>
 *   <li>3005 CATEGORY_NOT_FOUND — EntityValidator.validateCategory() 抛出（分类不存在）</li>
 *   <li>5003 BILL_ACCOUNT_DISABLED_GEN — generate() 时关联账户已禁用</li>
 *   <li>5004 BILL_DUE_DATE_INVALID — 下次到期日为空或过去日期</li>
 *   <li>5005 BILL_INACTIVE — 账单已停用</li>
 *   <li>5006 BILL_NOT_FOUND — 账单不存在</li>
 *   <li>5008 BILL_CATEGORY_NOT_FOUND — generate() 时关联分类不存在</li>
 * </ul>
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
   * 创建周期性账单（→ RecurringBillServiceImpl.create() · service/impl/RecurringBillServiceImpl.java 第121行）
   *
   * @param userId  当前用户 ID（JWT 解码获取）
   * @param request 账单创建请求（含名称、账户、分类、金额、类型、周期、到期日）
   * @return 新创建的周期性账单 DTO
   * @throws BusinessException 3004=账户不存在或已禁用（EntityValidator.validateAccount 抛出）/ 3005=分类不存在（EntityValidator.validateCategory 抛出）/ 5004=下次到期日无效
   */
  RecurringBillDTO create(Long userId, RecurringBillRequest request);

  /**
   * 更新周期性账单（→ RecurringBillServiceImpl.update() · service/impl/RecurringBillServiceImpl.java 第161行）
   *
   * @param userId  当前用户 ID（JWT 解码获取）
   * @param billId  要更新的账单 ID
   * @param request 账单更新请求（含名称、账户、分类、金额、类型、周期、到期日）
   * @return 更新后的周期性账单 DTO
   * @throws BusinessException 5006=账单不存在 / 5005=账单已停用 / 3004=账户不存在或已禁用 / 3005=分类不存在 / 5004=下次到期日无效
   */
  RecurringBillDTO update(Long userId, Long billId, RecurringBillRequest request);

  /**
   * 停用周期性账单（软删除 · status=0，停用后不可恢复 · → RecurringBillServiceImpl.deactivate() 第195行）
   *
   * @param userId 当前用户 ID（JWT 解码获取）
   * @param billId 要停用的账单 ID
   * @throws BusinessException 5006=账单不存在 / 5005=账单已停用
   */
  void deactivate(Long userId, Long billId);

  /**
   * 根据周期性账单生成交易记录（手动触发 · → RecurringBillServiceImpl.generate() 第224行）
   *
   * @param userId 当前用户 ID（JWT 解码获取）
   * @param billId 周期性账单 ID
   * @return 生成的交易记录 DTO
   * @throws BusinessException 5006=账单不存在 / 5005=账单已停用 / 5003=关联账户已禁用不可生成 / 5008=关联分类不存在 / 5004=下次到期日为空
   */
  TransactionDTO generate(Long userId, Long billId);
}
