/**
 * 财务业务常量
 * 集中管理账户类型、币种、周期等映射，避免硬编码散布各页面
 */

/** 交易类型枚举值（对齐后端 TransactionType 枚举 · 1=收入, 2=支出） */
export const TRANSACTION_TYPE_INCOME = 1    // 收入类型值
export const TRANSACTION_TYPE_EXPENSE = 2   // 支出类型值

/** 分类类型枚举值（对齐后端 CategoryType 枚举 · 1=支出, 2=收入） */
export const CATEGORY_TYPE_EXPENSE = 1      // 支出分类值
export const CATEGORY_TYPE_INCOME = 2       // 收入分类值

/** 状态枚举值（对齐后端 Status 枚举 · 1=启用/活跃, 0=停用/禁用） */
export const STATUS_ACTIVE = 1              // 启用状态值
export const STATUS_DISABLED = 0            // 停用状态值

/** 预算预警级别常量（对齐后端 BudgetAlertDTO.alertLevel） */
export const ALERT_LEVEL_OVERSPENT = 'OVERSPENT'      // 超支预警
export const ALERT_LEVEL_MONTHLY_WARN = 'MONTHLY_WARN' // 月度预警
export const ALERT_LEVEL_DAILY_WARN = 'DAILY_WARN'     // 日均预警
export const ALERT_LEVEL_NORMAL = 'NORMAL'             // 正常状态

/** CSV导入文件大小上限 5MB（对齐后端 TransactionServiceImpl.CSV_MAX_FILE_SIZE） */
export const MAX_CSV_FILE_SIZE = 5 * 1024 * 1024       // 5MB文件大小上限

/** 最小交易/转账金额（对齐后端 @DecimalMin("0.01") · TransactionRequest.java 第 36 行 · TransferRequest.java 第 28 行） */
export const MIN_TRANSACTION_AMOUNT = 0.01             // 最小金额0.01元
/** 金额输入步长（el-input-number :step 属性 · budget/account/transfer 页面用100大步，transaction用1精细步进） */
export const AMOUNT_STEP_ROUGH = 100                   // 大步金额步长(元)
export const AMOUNT_STEP_PRECISE = 1                    // 精细金额步长(元)
/** 最大备注长度（对齐后端 @Size(max=200) · TransactionRequest.java 第 42 行 · TransferRequest.java 第 32 行） */
export const MAX_NOTE_LENGTH = 200                     // 最大备注字符数
/** 账户余额上限（对齐后端 DECIMAL(12,2) 允许的最大值 · AccountRequest.java 第 24 行） */
export const MAX_ACCOUNT_BALANCE = 999999999.99        // 账户余额上限
/** 默认分页大小（对齐后端 API_DESIGN.md §1 分页参数 · BudgetController.java 等Controller的默认值） */
export const DEFAULT_PAGE_SIZE = 10                    // 默认每页条数
/** 分页大小选项（Element Plus el-pagination 的 page-sizes 数组 · 通用配置） */
export const PAGE_SIZE_OPTIONS = [10, 20, 50, 100]     // 分页大小选项
/** 默认周期类型（对齐后端 RecurringPeriod 枚举 · RecurringPeriod.java 第 30 行 · MONTHLY 为默认值） */
export const DEFAULT_RECURRING_PERIOD = 'monthly'      // 默认周期类型(每月)
/** 用户名字段长度限制（对齐后端 @Size(min=3, max=20) · UserLoginRequest.java 第 12 行） */
export const USERNAME_MIN_LENGTH = 3                   // 用户名最小长度
export const USERNAME_MAX_LENGTH = 20                  // 用户名最大长度
/** 密码字段长度限制（对齐后端 @Size(min=6, max=20) · ChangePasswordRequest.java 第 14 行） */
export const PASSWORD_MIN_LENGTH = 6                   // 密码最小长度
export const PASSWORD_MAX_LENGTH = 20                  // 密码最大长度
/** 账户名称最大长度（对齐后端 @Size(max=20) · AccountRequest.java 第 20 行） */
export const ACCOUNT_NAME_MAX_LENGTH = 20              // 账户名最大长度
/** 账单名称最大长度（对齐后端 @Size(min=1, max=30) · RecurringBillRequest.java 第 22 行） */
export const BILL_NAME_MAX_LENGTH = 30                 // 账单名最大长度
/** 用户名正则表达式（对齐后端 @Pattern · UserLoginRequest.java 第 14 行） */
export const USERNAME_PATTERN = /^[a-zA-Z0-9_]+$/      // 用户名格式: 字母数字下划线
/** 用户名正则错误提示 */
export const USERNAME_PATTERN_MSG = '用户名只能包含字母、数字和下划线'  // 用户名格式错误提示

/** 账户类型映射（对齐数据库 account.type: 1=现金, 2=银行卡, 3=支付宝, 4=微信） */
export const ACCOUNT_TYPE_MAP = { 1: '现金', 2: '银行卡', 3: '支付宝', 4: '微信' }  // 账户类型映射

/** 支持币种列表（对齐后端 ExchangeRateServiceImpl 硬编码汇率覆盖的 7 种货币 + CNY） */
export const CURRENCY_LIST = [                          // 支持币种列表
  { label: '人民币 (CNY)', value: 'CNY' },             // 人民币
  { label: '美元 (USD)', value: 'USD' },               // 美元
  { label: '欧元 (EUR)', value: 'EUR' },               // 欧元
  { label: '日元 (JPY)', value: 'JPY' },               // 日元
  { label: '英镑 (GBP)', value: 'GBP' },               // 英镑
  { label: '港币 (HKD)', value: 'HKD' },               // 港币
  { label: '韩元 (KRW)', value: 'KRW' }                // 韩元
]

/** 周期类型映射（对齐后端 RecurringPeriod 枚举的 4 种周期） */
export const PERIOD_MAP = { daily: '每天', weekly: '每周', monthly: '每月', yearly: '每年' }  // 周期映射

/** 交易类型映射（1=收入, 2=支出 · 对齐后端 TransactionType 枚举） */
export const TRANSACTION_TYPE_MAP = {                   // 交易类型映射
  [TRANSACTION_TYPE_INCOME]: { label: '收入', sign: '+' },  // 收入映射
  [TRANSACTION_TYPE_EXPENSE]: { label: '支出', sign: '-' }  // 支出映射
}

/** 状态映射（1=启用, 0=停用 · 对齐后端 Status 枚举） */
export const STATUS_MAP = { [STATUS_ACTIVE]: '启用', [STATUS_DISABLED]: '停用' }  // 状态映射

/** ECharts 图表颜色常量（与 CSS 变量 --color-income/--color-expense 同源） */
export const CHART_COLORS = {                           // 图表颜色常量
  income: '#67c23a',                                    // 收入绿色
  expense: '#f56c6c'                                    // 支出红色
}