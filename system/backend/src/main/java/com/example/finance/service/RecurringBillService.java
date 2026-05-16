package com.example.finance.service;

import com.example.finance.entity.dto.RecurringBillDTO;
import com.example.finance.entity.dto.RecurringBillRequest;
import com.example.finance.entity.dto.TransactionDTO;

import java.util.List;

/**
 * 周期性账单服务接口
 */
public interface RecurringBillService {

  /**
   * 查询用户周期性账单列表（status=1）
   */
  List<RecurringBillDTO> list(Long userId);

  /**
   * 创建周期性账单
   */
  RecurringBillDTO create(Long userId, RecurringBillRequest request);

  /**
   * 更新周期性账单
   */
  RecurringBillDTO update(Long userId, Long billId, RecurringBillRequest request);

  /**
   * 停用周期性账单（软删除）
   */
  void deactivate(Long userId, Long billId);

  /**
   * 生成交易记录（根据周期性账单）
   */
  TransactionDTO generate(Long userId, Long billId);
}
