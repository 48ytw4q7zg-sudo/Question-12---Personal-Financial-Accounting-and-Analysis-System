package com.example.finance.common.enums;

/**
 * 通用状态枚举（替代魔法值 0/1）
 *
 * 对应 PRD：
 *   - 账户 status：1=正常, 0=禁用（P0-2）
 *   - 周期账单 status：1=活跃, 0=停用（P1-4）
 */
public enum Status {
  DISABLED(0, "禁用/停用"),
  ACTIVE(1, "正常/活跃");

  private final int value;
  private final String label;

  Status(int value, String label) {
    this.value = value;
    this.label = label;
  }

  public int getValue() { return value; }
  public String getLabel() { return label; }

  /**
   * 从数据库 Integer 值转换为枚举
   * @param value 0=禁用, 1=正常
   * @return 对应枚举，非法值返回 null
   */
  public static Status fromValue(Integer value) {
    if (value == null) return null;
    for (Status s : values()) {
      if (s.value == value) return s;
    }
    return null;
  }
}
