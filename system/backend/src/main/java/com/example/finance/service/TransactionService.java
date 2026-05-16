package com.example.finance.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.finance.entity.dto.TransactionDTO;
import com.example.finance.entity.dto.TransactionRequest;
import com.example.finance.entity.dto.TransferDTO;
import com.example.finance.entity.dto.TransferRequest;
import org.springframework.web.multipart.MultipartFile;

/**
 * 交易记录服务接口
 */
public interface TransactionService {

  /**
   * 查询交易记录（分页 + 筛选）
   */
  IPage<TransactionDTO> list(Long userId, Long accountId, Long categoryId,
      String startTime, String endTime, String keyword, String sortBy,
      int pageNum, int pageSize);

  /**
   * 创建交易记录
   */
  TransactionDTO create(Long userId, TransactionRequest request);

  /**
   * 更新交易记录（转账记录仅允许修改备注）
   */
  TransactionDTO update(Long userId, Long transactionId, TransactionRequest request);

  /**
   * 转账
   */
  TransferDTO transfer(Long userId, TransferRequest request);

  /**
   * 导入 CSV
   */
  String importCsv(Long userId, MultipartFile file, Long accountId);
}
