package com.example.finance.common;

/**
 * 业务错误码集中管理（对应 PRD §3 统一错误码规范）
 *
 * 号段分配：
 *   1001-1099 = 用户模块
 *   2001-2099 = 账户模块
 *   3001-3099 = 交易模块
 *   4001-4099 = 预算模块
 *   5001-5099 = 周期账单模块
 *
 * 用法：throw new BusinessException(ErrorCode.USERNAME_EXISTS.getCode(), ErrorCode.USERNAME_EXISTS.getMsg());
 */
public enum ErrorCode {

  // ========== 用户模块 1001-1099 ==========
  USERNAME_EXISTS(1001, "用户名已存在"),
  USER_NOT_FOUND(1003, "用户不存在"),
  PASSWORD_ERROR(1002, "用户名或密码错误"),
  OLD_PASSWORD_ERROR(1002, "旧密码错误"),
  SAME_PASSWORD(1003, "新密码不能与旧密码相同"),

  // ========== 账户模块 2001-2099 ==========
  ACCOUNT_NAME_EMPTY(2001, "账户名称不能为空"),
  ACCOUNT_HAS_TRANSACTIONS(2002, "该账户下有收支记录，请先处理后再禁用"),
  ACCOUNT_HAS_RECURRING_BILLS(2002, "该账户下有活跃周期性账单，请先停用后再禁用"),
  ACCOUNT_DISABLED(2003, "关联账户已禁用"),

  // ========== 交易模块 3001-3099 ==========
  AMOUNT_INVALID(3001, "金额必须大于 0"),
  ACCOUNT_OR_CATEGORY_NOT_FOUND(3002, "账户或分类不存在"),
  TRANSFER_RECORD_NOT_MODIFIABLE(3003, "转账记录金额不可修改"),
  SAME_TRANSFER_ACCOUNT(3004, "转出账户和转入账户不能相同"),
  INSUFFICIENT_BALANCE(3005, "余额不足"),
  FILE_TOO_LARGE(3006, "文件大小不能超过 5MB"),
  RECORDS_TOO_MANY(3007, "单次导入不能超过 1000 条"),

  // ========== 预算模块 4001-4099 ==========
  BUDGET_AMOUNT_INVALID(4001, "预算金额必须大于 0"),
  CATEGORY_NOT_FOUND_OR_INCOME(4002, "分类不存在或为收入分类"),

  // ========== 周期账单模块 5001-5099 ==========
  BILL_NAME_EMPTY(5001, "周期性账单名称不能为空"),
  BILL_ACCOUNT_DISABLED(5002, "关联账户已禁用，不可生成"),
  BILL_DUE_DATE_UPDATED(5003, "到期日已被更新，请重试"),
  BILL_INACTIVE(5004, "周期性账单已停用");

  private final int code;
  private final String msg;

  ErrorCode(int code, String msg) {
    this.code = code;
    this.msg = msg;
  }

  public int getCode() { return code; }
  public String getMsg() { return msg; }
}
