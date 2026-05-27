package com.example.finance.common.enums;

/**
 * 用户角色枚举（替代魔法值 0/1）
 *
 * 对应数据库 user.role 字段：0=普通用户, 1=管理员
 */
public enum UserRole {
  NORMAL(0, "普通用户"),
  ADMIN(1, "管理员");

  private final int value;
  private final String label;

  UserRole(int value, String label) {
    this.value = value;
    this.label = label;
  }

  public int getValue() { return value; }
  public String getLabel() { return label; }

  /**
   * 从数据库 Integer 值转换为枚举
   * @param value 0=普通用户, 1=管理员
   * @return 对应枚举，非法值返回 null
   */
  public static UserRole fromValue(Integer value) {
    if (value == null) return null;
    for (UserRole r : values()) {
      if (r.value == value) return r;
    }
    return null;
  }
}
