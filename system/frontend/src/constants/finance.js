/**
 * 财务业务常量
 * 集中管理账户类型、币种、周期等映射，避免硬编码散布各页面
 */

/** 交易类型枚举值（对齐后端 TransactionType 枚举 · 1=收入, 2=支出） */
export const TRANSACTION_TYPE_INCOME = 1
export const TRANSACTION_TYPE_EXPENSE = 2

/** 分类类型枚举值（对齐后端 CategoryType 枚举 · 1=支出, 2=收入） */
export const CATEGORY_TYPE_EXPENSE = 1
export const CATEGORY_TYPE_INCOME = 2

/** 状态枚举值（对齐后端 Status 枚举 · 1=启用/活跃, 0=停用/禁用） */
export const STATUS_ACTIVE = 1
export const STATUS_DISABLED = 0

/** 预算预警级别常量（对齐后端 BudgetAlertDTO.alertLevel） */
export const ALERT_LEVEL_OVERSPENT = 'OVERSPENT'
export const ALERT_LEVEL_MONTHLY_WARN = 'MONTHLY_WARN'
export const ALERT_LEVEL_DAILY_WARN = 'DAILY_WARN'
export const ALERT_LEVEL_NORMAL = 'NORMAL'

/** CSV导入文件大小上限 5MB（对齐后端 TransactionServiceImpl.CSV_MAX_FILE_SIZE） */
export const MAX_CSV_FILE_SIZE = 5 * 1024 * 1024

/** 账户类型映射（对齐数据库 account.type: 1=现金, 2=银行卡, 3=支付宝, 4=微信） */
export const ACCOUNT_TYPE_MAP = { 1: '现金', 2: '银行卡', 3: '支付宝', 4: '微信' }

/** 支持币种列表（对齐后端 ExchangeRateServiceImpl 硬编码汇率覆盖的 7 种货币 + CNY） */
export const CURRENCY_LIST = [
  { label: '人民币 (CNY)', value: 'CNY' },
  { label: '美元 (USD)', value: 'USD' },
  { label: '欧元 (EUR)', value: 'EUR' },
  { label: '日元 (JPY)', value: 'JPY' },
  { label: '英镑 (GBP)', value: 'GBP' },
  { label: '港币 (HKD)', value: 'HKD' },
  { label: '韩元 (KRW)', value: 'KRW' }
]

/** 周期类型映射（对齐后端 RecurringPeriod 枚举的 4 种周期） */
export const PERIOD_MAP = { daily: '每天', weekly: '每周', monthly: '每月', yearly: '每年' }

/** 交易类型映射（1=收入, 2=支出 · 对齐后端 TransactionType 枚举） */
export const TRANSACTION_TYPE_MAP = {
  [TRANSACTION_TYPE_INCOME]: { label: '收入', sign: '+' },
  [TRANSACTION_TYPE_EXPENSE]: { label: '支出', sign: '-' }
}

/** 状态映射（1=启用, 0=停用 · 对齐后端 Status 枚举） */
export const STATUS_MAP = { [STATUS_ACTIVE]: '启用', [STATUS_DISABLED]: '停用' }

/** ECharts 图表颜色常量（与 CSS 变量 --color-income/--color-expense 同源） */
export const CHART_COLORS = {
  income: '#67c23a',
  expense: '#f56c6c'
}