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