/**
 * 通用格式化工具函数
 * 职责：时间/日期/金额格式化
 * 调用方：TransactionListPage / AccountPage / AdminPage / DashboardPage / BudgetPage / RecurringBillPage / CategoryPage
 */

/** 格式化 ISO 时间字符串为 'YYYY-MM-DD HH:mm:ss'（简单字符串替换，不做时区转换） */
export function formatTime(time) {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 19)
}

/** 格式化 Date 对象为 'YYYY-MM-DD HH:mm:ss' 字符串 */
export function formatDateTime(date) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

/** 格式化金额为 2 位小数字符串（空值/undefined/null/非数字 默认为 0.00） */
export function formatAmount(val) {
  const num = Number(val)
  return isNaN(num) ? '0.00' : num.toFixed(2)
}

/** 格式化日期为 YYYY-MM-DD（只取前 10 位） */
export function formatDate(date) {
  if (!date) return ''
  return String(date).substring(0, 10)
}
