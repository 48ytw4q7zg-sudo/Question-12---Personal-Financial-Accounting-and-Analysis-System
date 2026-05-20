package com.example.finance.common.enums;

/**
 * 账户类型枚举（替代魔法值 1/2/3/4）
 *
 * 对应 PRD P0-2：type 取值 {1: 现金, 2: 银行卡, 3: 支付宝, 4: 微信}
 */
public enum AccountType {
  CASH(1, "现金"),
  BANK_CARD(2, "银行卡"),
  ALIPAY(3, "支付宝"),
  WECHAT(4, "微信");

  private final int value;
  private final String label;

  AccountType(int value, String label) {
    this.value = value;
    this.label = label;
  }

  public int getValue() { return value; }
  public String getLabel() { return label; }

  /**
   * 从数据库 Integer 值转换为枚举
   * @param value 1=现金, 2=银行卡, 3=支付宝, 4=微信
   * @return 对应枚举，非法值返回 null
   */
  public static AccountType fromValue(Integer value) {
    if (value == null) return null;
    for (AccountType t : values()) {
      if (t.value == value) return t;
    }
    return null;
  }
}
