package com.example.finance.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.finance.entity.dto.TransactionDTO;
import com.example.finance.entity.dto.TransactionRequest;
import com.example.finance.entity.dto.TransferDTO;
import com.example.finance.entity.dto.TransferRequest;

/**
 * 交易记录服务接口（PRD P0-4 收支记录 CRUD + P1-1 多条件筛选 + P1-5 转账 + P2-3 CSV 导入）
 */
public interface TransactionService {

  /**
   * 查询交易记录（分页 + 多条件筛选）
   *
   * @param userId     当前用户 ID（JWT 解码获取）
   * @param accountId  账户筛选（null=全部）
   * @param categoryId 分类筛选（null=全部）
   * @param startTime  开始时间（yyyy-MM-dd HH:mm:ss，null=不限）
   * @param endTime    结束时间（yyyy-MM-dd HH:mm:ss，null=不限）
   * @param keyword    关键词搜索（备注模糊匹配，null=不限）
   * @param sortBy     排序字段（time/amount，默认 time）
   * @param pageNum    页码（从 1 开始）
   * @param pageSize   每页条数
   * @return 分页结果（含 accountName/categoryName JOIN 填充）
   */
  IPage<TransactionDTO> list(Long userId, Long accountId, Long categoryId,
      String startTime, String endTime, String keyword, String sortBy,
      int pageNum, int pageSize);

  /**
   * 创建交易记录（记一笔）
   *
   * @param userId  当前用户 ID（JWT 解码获取）
   * @param request 交易创建请求（含账户、分类、类型、金额、时间、备注）
   * @return 新创建的交易记录 DTO
   * @throws com.example.finance.common.BusinessException 3004 账户不存在或已禁用（对齐 ErrorCode.ACCOUNT_NOT_FOUND_OR_DISABLED）/ 3005 分类不存在（对齐 ErrorCode.CATEGORY_NOT_FOUND）
   */
  TransactionDTO create(Long userId, TransactionRequest request);

  /**
   * 更新交易记录（转账生成的记录仅允许修改备注）
   *
   * @param userId        当前用户 ID（JWT 解码获取）
   * @param transactionId 要更新的交易记录 ID
   * @param request       交易更新请求
   * @return 更新后的交易记录 DTO
   * @throws com.example.finance.common.BusinessException 3011 收支记录不存在（对齐 ErrorCode.RECORD_NOT_FOUND）
   * @throws com.example.finance.common.BusinessException 3006 转账记录金额不可修改（对齐 ErrorCode.TRANSFER_RECORD_NOT_MODIFIABLE）
   */
  TransactionDTO update(Long userId, Long transactionId, TransactionRequest request);

  /**
   * 删除交易记录（转账关联记录禁止删除）
   *
   * @param userId        当前用户 ID（JWT 解码获取）
   * @param transactionId 要删除的交易记录 ID
   * @throws com.example.finance.common.BusinessException 3011 收支记录不存在（对齐 ErrorCode.RECORD_NOT_FOUND）
   * @throws com.example.finance.common.BusinessException 3007 转账关联记录禁止删除（对齐 ErrorCode.TRANSFER_RECORD_NOT_DELETABLE）
   */
  void delete(Long userId, Long transactionId);

  /**
   * 账户间转账（生成两条关联记录：转出支出 + 转入收入）
   *
   * @param userId  当前用户 ID（JWT 解码获取）
   * @param request 转账请求（含转出账户、转入账户、金额、备注）
   * @return 转账结果（含 transferId 及两条关联记录）
   * @throws com.example.finance.common.BusinessException 3004 账户不存在或已禁用（对齐 ErrorCode.ACCOUNT_NOT_FOUND_OR_DISABLED）
   * @throws com.example.finance.common.BusinessException 3009 余额不足（对齐 ErrorCode.INSUFFICIENT_BALANCE）/ 3008 转出转入账户不可相同（对齐 ErrorCode.SAME_TRANSFER_ACCOUNT）
   */
  TransferDTO transfer(Long userId, TransferRequest request);

  // importCsv 已迁移至 TransactionImportService（从 TransactionServiceImpl 拆分, 职责单一化）
}
