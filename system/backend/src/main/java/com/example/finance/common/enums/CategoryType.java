package com.example.finance.common.enums;

/**
 * 分类类型枚举（替代魔法值 1/2）
 *
 * 对应 PRD P0-3：type 取值 {1: 支出, 2: 收入}
 */
/** WARNING: 1=EXPENSE/2=INCOME。TransactionType 映射相反(1=INCOME/2=EXPENSE)。前端 RecurringBillPage 的 type翻转基于此差异。 */
public enum CategoryType {
  EXPENSE(1, "支出"),
  INCOME(2, "收入");

  private final int value;
  private final String label;

  CategoryType(int value, String label) {
    this.value = value;
    this.label = label;
  }

  public int getValue() { return value; }
  public String getLabel() { return label; }

  /**
   * 从数据库 Integer 值转换为枚举
   * @param value 1=支出, 2=收入
   * @return 对应枚举，非法值返回 null
   */
  public static CategoryType fromValue(Integer value) {
    if (value == null) return null;
    for (CategoryType t : values()) {
      if (t.value == value) return t;
    }
    return null;
  }
}
