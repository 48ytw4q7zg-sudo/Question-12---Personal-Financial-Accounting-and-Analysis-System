// 数据传输对象包（DTO 层，用于接收前端请求和返回响应）
package com.example.finance.entity.dto;

// Lombok 自动生成 getter/setter/toString/equals/hashCode
import lombok.Data;

// Java 可变长数组实现类（初始化 failRows 避免 NPE）
import java.util.ArrayList;
// Java List 接口（泛型集合声明）
import java.util.List;

/**
 * CSV 导入结果 DTO（PRD P2-3 导入银行 CSV · 预览+确认导入结果）
 *
 * 返回结构化导入结果，前端展示成功/失败统计 + 失败明细表格。
 * PRD P2-3 主流程 Step 2: "系统校验数据格式 → 预览导入结果（有效条数 + 错误条数）"。
 *
 * 调用方: controller/ImportController.java → service/impl/ImportServiceImpl.java
 */
// Lombok: 自动生成 getter/setter/toString/equals/hashCode（Lombok 1.18.46）
@Data
// CSV 导入结果 DTO 类（Controller 通过 Result<ImportResultDTO> 返回前端预览弹窗）
public class ImportResultDTO {

  /** 导入成功条数（校验通过 + 入库成功的记录数） */
  private int successCount;

  /** 导入失败条数（校验失败或入库异常被跳过的记录数） */
  private int failCount;

  /** 失败明细列表（行号 + 错误原因，前端 el-table 展示用，初始化为空列表避免 NPE） */
  private List<FailRow> failRows = new ArrayList<>();

  /**
   * 失败明细行（内部静态类，被 ImportResultDTO.failRows 引用）
   *
   * 每条 FailRow 对应 CSV 中一条格式/校验失败的记录
   */
  // Lombok: 自动生成 getter/setter/toString/equals/hashCode（Lombok 1.18.46）
  @Data
  // 失败明细行静态内部类（仅 ImportResultDTO 使用，无需独立文件）
  public static class FailRow {
    /** CSV 文件中的行号（从 2 开始计数，第 1 行是表头，方便用户定位原文件） */
    private int row;
    /** 失败原因描述（如「金额格式错误」「分类不存在」，前端 el-table 列展示） */
    private String reason;
  }
}
