package com.example.finance.common;  // 通用层：错误码枚举

/**
 * 业务错误码集中管理 — 每个枚举项 = code(int) + msg(String)，全局唯一不重复
 *
 * <p>设计意图：替代各 ServiceImpl 中的硬编码错误码字符串，实现错误码的集中定义和统一管理。</p>
 *
 * <p>号段分配（对齐 API_DESIGN.md §4.3 模块编号规范）：
 *   1001-1099 = 用户模块（注册/登录/密码）
 *   2001-2099 = 账户模块（CRUD/状态管理）
 *   3001-3099 = 交易模块（收支记录/转账/导入）
 *   4001-4099 = 预算模块（预算设置/预警）
 *   5001-5099 = 周期账单模块（设置/生成/提醒）
 *   6001-6099 = 管理员模块（用户管理/权限）
 *   7001-7099 = 参数校验模块（时间/格式/参数校验）</p>
 *
 * <p>用法：throw new BusinessException(ErrorCode.USERNAME_EXISTS.getCode(), ErrorCode.USERNAME_EXISTS.getMsg());
 *   或从 ErrorCode 中提取 code/message 后调用 Result.error(code, msg)。</p>
 *
 * <p>调用方：所有 ServiceImpl（service/impl/ 目录）在业务校验失败时引用；
 *           GlobalExceptionHandler 的权限异常处理引用 ADMIN_ACCESS_DENIED；
 *           LoginInterceptor 的 401 响应引用 UNAUTHORIZED。</p>
 *
 * <p>引用：common/BusinessException.java — 使用 ErrorCode.getCode()/getMsg() 构造异常；
 *          common/GlobalExceptionHandler.java — 权限拒绝时引用 ADMIN_ACCESS_DENIED(6004)；
 *          interceptor/LoginInterceptor.java — 未登录时引用 UNAUTHORIZED(401)。</p>
 */
public enum ErrorCode {  // 错误码枚举（每个枚举实例 = 不可变的 code + msg 对）

  // ========== 用户模块 1001-1099（每个码值唯一，不重复） ==========
  /** 1001=用户名已存在（注册时校验 · UserServiceImpl.checkUsernameExists()） */
  USERNAME_EXISTS(1001, "用户名已存在"),
  /** 1002=用户名或密码错误（登录时校验 · UserServiceImpl.login()） */
  PASSWORD_ERROR(1002, "用户名或密码错误"),
  /** 1003=用户不存在（查询/操作目标用户时校验 · UserServiceImpl.findById()） */
  USER_NOT_FOUND(1003, "用户不存在"),
  /** 1004=登录尝试过于频繁（LoginRateLimiter 限流拒绝 · UserServiceImpl.login()） */
  LOGIN_RATE_LIMIT(1004, "登录尝试过于频繁，请稍后再试"),
  /** 1005=旧密码错误（修改密码时校验 · UserServiceImpl.changePassword()） */
  OLD_PASSWORD_ERROR(1005, "旧密码错误"),
  /** 1006=新密码不能与旧密码相同（修改密码时校验 · UserServiceImpl.changePassword()） */
  SAME_PASSWORD(1006, "新密码不能与旧密码相同"),

  // ========== 账户模块 2001-2099 ==========
  /** 2002=该账户下有收支记录（禁用账户时校验 · AccountServiceImpl.disable()） */
  ACCOUNT_HAS_TRANSACTIONS(2002, "该账户下有收支记录，请先处理后再禁用"),
  /** 2003=该账户下有活跃周期性账单（禁用账户时校验 · AccountServiceImpl.disable()） */
  ACCOUNT_HAS_RECURRING_BILLS(2003, "该账户下有活跃周期性账单，请先停用后再禁用"),
  /** 2004=账户不存在（查询/操作目标账户时校验 · AccountServiceImpl.disable()/update()） */
  ACCOUNT_NOT_FOUND(2004, "账户不存在"),
  /** 2006=账户名称已存在（创建/修改账户时校验 · AccountServiceImpl.create()/update()） */
  ACCOUNT_NAME_DUPLICATE(2006, "账户名称已存在"),

  // ========== 交易模块 3001-3099 ==========
  /** 3002=仅支持 .csv 格式文件（导入时校验 · TransactionServiceImpl.importCsv()） */
  CSV_FORMAT_ONLY(3002, "仅支持 .csv 格式文件"),
  /** 3003=CSV 文件读取失败（导入时异常 · TransactionServiceImpl.importCsv()） */
  CSV_READ_ERROR(3003, "CSV 文件读取失败"),
  /** 3004=账户不存在或已禁用（创建收支记录时校验 · EntityValidator.validateAccount()） */
  ACCOUNT_NOT_FOUND_OR_DISABLED(3004, "账户不存在或已禁用"),
  /** 3005=分类不存在（创建收支记录时校验 · EntityValidator.validateCategory()） */
  CATEGORY_NOT_FOUND(3005, "分类不存在"),
  /** 3006=转账记录金额不可修改（修改转账记录时校验 · TransactionServiceImpl.update()） */
  TRANSFER_RECORD_NOT_MODIFIABLE(3006, "转账记录金额不可修改"),
  /** 3007=转账关联记录不可删除（删除转账记录时校验 · TransactionServiceImpl.delete()） */
  TRANSFER_RECORD_NOT_DELETABLE(3007, "转账关联记录不可删除"),
  /** 3008=转出账户和转入账户不能相同（转账时校验 · TransactionServiceImpl.createTransfer()） */
  SAME_TRANSFER_ACCOUNT(3008, "转出账户和转入账户不能相同"),
  /** 3009=余额不足（支出/转账时校验 · TransactionServiceImpl.create()） */
  INSUFFICIENT_BALANCE(3009, "余额不足"),
  /** 3010=文件大小不能超过 5MB（导入时校验 · TransactionServiceImpl.importCsv()） */
  FILE_TOO_LARGE(3010, "文件大小不能超过 5MB"),
  /** 3011=收支记录不存在（查询/修改/删除时校验 · TransactionServiceImpl.update()/delete()） */
  RECORD_NOT_FOUND(3011, "收支记录不存在"),
  /** 3013=请求参数不合法（通用参数校验失败 · EntityValidator + Controller 层校验） */
  PARAM_INVALID(3013, "请求参数不合法"),

  // ========== 预算模块 4001-4099 ==========
  /** 4002=预算仅可设置在支出分类上（创建/修改预算时校验 · BudgetServiceImpl.create()/update()） */
  BUDGET_EXPENSE_ONLY(4002, "预算仅可设置在支出分类上"),
  /** 4003=分类不存在（创建/修改预算时校验 · BudgetServiceImpl.create()/update()） */
  CATEGORY_NOT_FOUND_FOR_BUDGET(4003, "分类不存在"),
  /** 4005=预算不存在（查询/修改/删除时校验 · BudgetServiceImpl.update()/delete()） */
  BUDGET_NOT_FOUND(4005, "预算不存在"),

  // ========== 周期账单模块 5001-5099 ==========
  /** 5003=关联账户已禁用不可生成（生成账单时校验 · RecurringBillServiceImpl.generateBill()） */
  BILL_ACCOUNT_DISABLED_GEN(5003, "关联账户已禁用不可生成"),
  /** 5004=下次到期日必须是未来日期（创建/修改时校验 · RecurringBillServiceImpl.create()/update()） */
  BILL_DUE_DATE_INVALID(5004, "下次到期日必须是未来日期"),
  /** 5005=周期性账单已停用（操作已停用账单时校验 · RecurringBillServiceImpl） */
  BILL_INACTIVE(5005, "周期性账单已停用"),
  /** 5006=周期性账单不存在（查询/修改/删除时校验 · RecurringBillServiceImpl.update()/delete()） */
  BILL_NOT_FOUND(5006, "周期性账单不存在"),
  /** 5008=分类不存在（创建/修改账单时校验 · RecurringBillServiceImpl.create()/update()） */
  BILL_CATEGORY_NOT_FOUND(5008, "分类不存在"),

  // ========== 管理员模块 6001-6099 ==========
  /** 6001=管理员不能删除自己（AdminServiceImpl.deleteUser() 校验） */
  ADMIN_CANNOT_DELETE_SELF(6001, "管理员不能删除自己"),
  /** 6002=管理员不能修改自己的角色（AdminServiceImpl.updateUserRole() 校验） */
  ADMIN_CANNOT_MODIFY_SELF(6002, "管理员不能修改自己的角色"),
  /** 6003=用户不存在（管理员操作目标用户时校验 · AdminServiceImpl） */
  ADMIN_USER_NOT_FOUND(6003, "用户不存在"),
  /** 6004=无管理员权限（AdminInterceptor 校验 role≠1 时抛出 · AdminInterceptor.preHandle()） */
  ADMIN_ACCESS_DENIED(6004, "无管理员权限"),

  // ========== 参数校验模块 7001-7099 ==========
  /** 7002=时间范围最大跨度1年（统计查询时校验 · StatisticsServiceImpl） */
  TIME_RANGE_TOO_LARGE(7002, "时间范围最大跨度1年"),
  /** 7003=时间格式须为yyyy-MM-dd HH:mm:ss（时间参数格式校验 · EntityValidator） */
  TIME_FORMAT_INVALID(7003, "时间格式须为yyyy-MM-dd HH:mm:ss"),
  /** 7004=日期格式错误（日期参数格式校验 · EntityValidator） */
  DATE_FORMAT_INVALID(7004, "日期格式错误，应为 YYYY-MM-DD"),
  /** 7005=year需在2000-2100之间（年份参数范围校验 · EntityValidator.formatYearMonth()） */
  YEAR_OUT_OF_RANGE(7005, "year需在2000-2100之间"),
  /** 7006=month需在1-12之间（月份参数范围校验 · EntityValidator.formatYearMonth()） */
  MONTH_OUT_OF_RANGE(7006, "month需在1-12之间"),
  /** 7008=预算保存冲突请重试（乐观锁/并发冲突处理 · BudgetServiceImpl） */
  BUDGET_SAVE_CONFLICT(7008, "预算保存冲突，请重试"),
  /** 401=未登录或token已过期（LoginInterceptor 校验失败 · interceptor/LoginInterceptor.java） */
  // 注意：此码值 401 与其他号段不同，对应 HTTP 401 语义（未认证）
  UNAUTHORIZED(401, "未登录或 token 已过期");  // 最后一项以分号结尾

  // ===== 枚举字段 =====
  /** 错误码数值（int 类型，与响应 JSON 的 code 字段对应） */
  private final int code;  // final修饰：错误码不可变

  /** 错误消息（String 类型，前端 ElMessage.error 展示给用户） */
  private final String msg;  // final修饰：错误消息不可变

  /**
   * 枚举构造器（自动由枚举实例调用，每个枚举实例 = 一组 code + msg 对）
   *
   * @param code 错误码数值
   * @param msg  错误消息文本
   */
  ErrorCode(int code, String msg) {  // 枚举构造器（每个枚举实例初始化时调用）
    this.code = code;  // 存储错误码数值
    this.msg = msg;    // 存储错误消息文本
  }

  /**
   * 获取错误码数值（int 类型）
   *
   * <p>调用方：Service 层 throw new BusinessException(ErrorCode.XXX.getCode(), ErrorCode.XXX.getMsg());
   *           GlobalExceptionHandler 返回 Result.error(code, msg)。</p>
   */
  public int getCode() { return code; }  // 返回错误码数值

  /**
   * 获取错误消息文本（String 类型）
   *
   * <p>调用方：同上 getCode()。</p>
   */
  public String getMsg() { return msg; }  // 返回错误消息文本
}
