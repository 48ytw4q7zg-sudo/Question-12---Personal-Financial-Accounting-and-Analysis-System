// 数据传输对象包（DTO 层，用于接收前端请求和返回响应）
package com.example.finance.entity.dto;

// Lombok 自动生成 getter/setter/toString/equals/hashCode
import lombok.Data;

// Java 高精度金额类型（禁止 float/double 防精度丢失）
import java.math.BigDecimal;
// Java 8 日期时间类型（CLAUDE.md §二·二 强制：时间字段一律用 LocalDateTime）
import java.time.LocalDateTime;

/**
 * 预算数据传输对象 DTO（前端 BudgetPage.vue 列表展示）
 *
 * 预算按用户+分类+月份设置，仅支出分类可参与预算
 * categoryName 由 Service 层批量 JOIN category 表填充（避免 N+1）
 *
 * 对应数据库表: budget（id/category_id/month/amount/create_time/update_time）
 * 调用方: controller/BudgetController.java → service/impl/BudgetServiceImpl.java
 */
// Lombok: 自动生成 getter/setter/toString/equals/hashCode（Lombok 1.18.46）
@Data
// 预算信息 DTO 类（Service 层查询后组装返回，Controller 用 Result<List<BudgetDTO>> 包装）
public class BudgetDTO {

  /** 预算主键 ID */
  private Long id;

  /** 关联分类 ID（FK → category.id，仅支出分类 type=1） */
  private Long categoryId;

  /** 关联分类名称（批量加载填充，前端进度条展示用） */
  private String categoryName;

  /** 预算月份（yyyy-MM 格式，如 2026-05） */
  private String month;

  /** 预算金额（DECIMAL(12,2)） */
  private BigDecimal amount;

  /** 创建时间 */
  private LocalDateTime createTime;

  /** 最后更新时间（覆盖保存时更新） */
  private LocalDateTime updateTime;
}
