package com.example.finance.service;

import com.example.finance.entity.dto.ImportResultDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 交易记录批量导入服务接口（PRD P2-3 CSV 导入）
 *
 * <p>从 TransactionService 拆分而出,职责单一: CSV 文件校验 + 解析 + 批量写入。</p>
 */
public interface TransactionImportService {

  /**
   * 导入 CSV 格式的收支记录
   *
   * @param userId    当前用户 ID（JWT 解码获取）
   * @param file      上传的 CSV 文件
   * @param accountId 导入到的目标账户 ID
   * @return 结构化导入结果（成功/失败条数 + 失败明细）
   */
  ImportResultDTO importCsv(Long userId, MultipartFile file, Long accountId);
}
