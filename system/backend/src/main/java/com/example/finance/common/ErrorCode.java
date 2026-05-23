package com.example.finance.common;

/**
 * 业务错误码集中管理（对应 PRD §3 统一错误码规范 · 每个码值全局唯一）
 *
 * 号段分配：
 *   1001-1099 = 用户模块
 *   2001-2099 = 账户模块
 *   3001-3099 = 交易模块
 *   4001-4099 = 预算模块
 *   5001-5099 = 周期账单模块
 *   6001-6099 = 管理员模块
 *
 * 用法：throw new BusinessException(ErrorCode.USERNAME_EXISTS.getCode(), ErrorCode.USERNAME_EXISTS.getMsg());
 */
public enum ErrorCode {

  // ========== 用户模块 1001-1099（每个码值唯一，不重复） ==========
  USERNAME_EXISTS(1001, "用户名已存在"),
  PASSWORD_ERROR(1002, "用户名或密码错误"),
  USER_NOT_FOUND(1003, "用户不存在"),
  LOGIN_RATE_LIMIT(1004, "登录尝试过于频繁，请稍后再试"),
  OLD_PASSWORD_ERROR(1005, "旧密码错误"),
  SAME_PASSWORD(1006, "新密码不能与旧密码相同"),

  // ========== 账户模块 2001-2099（每个码值唯一，不重复） ==========
  ACCOUNT_NAME_EMPTY(2001, "账户名称不能为空"),
  ACCOUNT_HAS_TRANSACTIONS(2002, "该账户下有收支记录，请先处理后再禁用"), // 实际抛出时动态拼接记录数量: "该账户下有 N 条收支记录..."
  ACCOUNT_HAS_RECURRING_BILLS(2003, "该账户下有活跃周期性账单，请先停用后再禁用"), // 实际抛出时动态拼接账单数量: "该账户下有 N 个活跃周期性账单..."
  ACCOUNT_NOT_FOUND(2004, "账户不存在"),
  ACCOUNT_DISABLED(2005, "关联账户已禁用"),
  ACCOUNT_NAME_DUPLICATE(2006, "账户名称已存在"),

  // ========== 交易模块 3001-3099（每个码值唯一，不重复） ==========
  AMOUNT_INVALID(3001, "金额必须大于 0"),
  CSV_FORMAT_ONLY(3002, "仅支持 .csv 格式文件"),
  CSV_READ_ERROR(3003, "CSV 文件读取失败"),
  ACCOUNT_NOT_FOUND_OR_DISABLED(3004, "账户不存在或已禁用"),
  CATEGORY_NOT_FOUND(3005, "分类不存在"),
  TRANSFER_RECORD_NOT_MODIFIABLE(3006, "转账记录金额不可修改"),
  TRANSFER_RECORD_NOT_DELETABLE(3007, "转账关联记录不可删除"),
  SAME_TRANSFER_ACCOUNT(3008, "转出账户和转入账户不能相同"),
  INSUFFICIENT_BALANCE(3009, "余额不足"),
  FILE_TOO_LARGE(3010, "文件大小不能超过 5MB"),
  RECORD_NOT_FOUND(3011, "收支记录不存在"),
  RECORDS_TOO_MANY(3012, "单次导入不能超过 1000 条"),
  PARAM_INVALID(3013, "请求参数不合法"),

  // ========== 预算模块 4001-4099（每个码值唯一，不重复） ==========
  BUDGET_AMOUNT_INVALID(4001, "预算金额必须大于 0"),
  BUDGET_EXPENSE_ONLY(4002, "预算仅可设置在支出分类上"),
  CATEGORY_NOT_FOUND_FOR_BUDGET(4003, "分类不存在"),
  CATEGORY_NOT_FOUND_OR_INCOME(4004, "分类不存在或为收入分类"),
  BUDGET_NOT_FOUND(4005, "预算不存在"),

  // ========== 周期账单模块 5001-5099（每个码值唯一，不重复） ==========
  BILL_NAME_EMPTY(5001, "周期性账单名称不能为空"),
  BILL_ACCOUNT_DISABLED(5002, "关联账户已禁用，不可生成"),
  BILL_ACCOUNT_DISABLED_GEN(5003, "关联账户已禁用不可生成"),
  BILL_DUE_DATE_INVALID(5004, "下次到期日必须是未来日期"),
  BILL_INACTIVE(5005, "周期性账单已停用"),
  BILL_NOT_FOUND(5006, "周期性账单不存在"),
  BILL_ACCOUNT_NOT_FOUND(5007, "账户不存在"),
  BILL_CATEGORY_NOT_FOUND(5008, "分类不存在"),

  // ========== 管理员模块 6001-6099（独立号段，不与用户模块冲突） ==========
  ADMIN_CANNOT_DELETE_SELF(6001, "管理员不能删除自己"),
  ADMIN_CANNOT_MODIFY_SELF(6002, "管理员不能修改自己的角色"),
  ADMIN_USER_NOT_FOUND(6003, "用户不存在"),
  ADMIN_ACCESS_DENIED(6004, "无管理员权限"),

  // ========== 参数校验模块 7001-7099（通用参数校验错误） ==========
  SORT_PARAM_INVALID(7001, "排序参数只能是time/amount_asc/amount_desc"),
  TIME_RANGE_TOO_LARGE(7002, "时间范围最大跨度1年"),
  TIME_FORMAT_INVALID(7003, "时间格式须为yyyy-MM-dd HH:mm:ss"),
  DATE_FORMAT_INVALID(7004, "日期格式错误，应为 YYYY-MM-DD"),
  YEAR_OUT_OF_RANGE(7005, "year需在2000-2100之间"),
  MONTH_OUT_OF_RANGE(7006, "month需在1-12之间"),
  TRANSFER_CATEGORY_MISSING(7007, "分类不存在：未找到「其他」分类，请检查种子数据"),
  BUDGET_SAVE_CONFLICT(7008, "预算保存冲突，请重试"),
  UNAUTHORIZED(401, "未登录或 token 已过期");

  private final int code;
  private final String msg;

  ErrorCode(int code, String msg) {
    this.code = code;
    this.msg = msg;
  }

  public int getCode() { return code; }
  public String getMsg() { return msg; }
}
