package com.example.finance.common.enums;

import lombok.Getter;

/**
 * 周期性账单周期枚举（对齐 RecurringBillServiceImpl.calculateNextDueDate 支持的 4 种周期）
 *
 * <p>枚举值与数据库 recurring_bill.period 字段存储的字符串值一一对应。</p>
 * <p>用于替代 calculateNextDueDate 中硬编码的字符串 switch 匹配，消除魔法值。</p>
 *
 * <p>周期映射:</p>
 * <ul>
 *   <li>DAILY("daily") → 下次到期日 +1天</li>
 *   <li>WEEKLY("weekly") → 下次到期日 +1周</li>
 *   <li>MONTHLY("monthly") → 下次到期日 +1月</li>
 *   <li>YEARLY("yearly") → 下次到期日 +1年</li>
 * </ul>
 */
@Getter
public enum RecurringPeriod {
  DAILY("daily", "每天"),
  WEEKLY("weekly", "每周"),
  MONTHLY("monthly", "每月"),
  YEARLY("yearly", "每年");

  /** 数据库存储值（对应 recurring_bill.period 列） */
  private final String value;
  /** 中文标签（用于前端展示） */
  private final String label;

  RecurringPeriod(String value, String label) {
    this.value = value;
    this.label = label;
  }

  /** 从字符串值获取枚举（未知值默认返回 MONTHLY，与原 calculateNextDueDate 兜底逻辑一致） */
  public static RecurringPeriod fromValue(String value) {
    for (RecurringPeriod p : values()) {
      if (p.value.equals(value)) return p;
    }
    return MONTHLY;
  }
}