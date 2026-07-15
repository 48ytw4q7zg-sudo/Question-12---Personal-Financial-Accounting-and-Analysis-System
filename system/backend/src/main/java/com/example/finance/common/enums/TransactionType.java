package com.example.finance.common.enums;

/**
 * 交易类型枚举（替代魔法值 1/2）
 *
 * 对应 PRD P0-4：type 取值 {1: 收入, 2: 支出}
 */
/** WARNING: 1=INCOME/2=EXPENSE。CategoryType 映射相反(1=EXPENSE/2=INCOME)，两者有意相反。 */
public enum TransactionType {
  INCOME(1, "收入"),
  EXPENSE(2, "支出");

  private final int value;
  private final String label;

  TransactionType(int value, String label) {
    this.value = value;
    this.label = label;
  }

  public int getValue() { return value; }
  public String getLabel() { return label; }

  /**
   * 从数据库 Integer 值转换为枚举
   * @param value 1=收入, 2=支出
   * @return 对应枚举，非法值返回 null
   */
  public static TransactionType fromValue(Integer value) {
    if (value == null) return null;
    for (TransactionType t : values()) {
      if (t.value == value) return t;
    }
    return null;
  }
}
