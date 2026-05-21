package com.example.finance.entity.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * CSV 导入结果 DTO（PRD P2-3 导入银行 CSV · 预览导入结果）
 *
 * <p>返回结构化导入结果，前端展示成功/失败统计 + 失败明细表格。</p>
 * <p>PRD P2-3 主流程 Step 2: "系统校验数据格式 → 预览导入结果（有效条数 + 错误条数）"。</p>
 */
@Data
public class ImportResultDTO {

  /** 导入成功条数 */
  private int successCount;

  /** 导入失败条数 */
  private int failCount;

  /** 失败明细列表（行号 + 错误原因） */
  private List<FailRow> failRows = new ArrayList<>();

  /**
   * 失败明细行
   */
  @Data
  public static class FailRow {
    /** CSV 文件中的行号（从 2 开始，第 1 行是表头） */
    private int row;
    /** 失败原因描述 */
    private String reason;
  }
}
