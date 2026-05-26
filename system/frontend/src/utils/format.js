/**
 * 通用格式化工具函数
 * 职责：时间/日期/金额格式化
 * 调用方：TransactionListPage / AccountPage / AdminPage / DashboardPage / BudgetPage / RecurringBillPage / CategoryPage
 */

/**
 * 格式化 ISO 时间字符串为 'YYYY-MM-DD HH:mm:ss'
 * 注意：仅做字符串替换（T→空格），不做时区转换。后端返回的时间已是 Asia/Shanghai 时区，
 * 因此直接截取前19位即可正确显示本地时间（对齐 application.yml time-zone: Asia/Shanghai 配置）
 */
export function formatTime(time) {                     // 格式化 ISO 时间字符串为 'YYYY-MM-DD HH:mm:ss'
  if (!time) return ''                      // 空值返回空字符串
  return time.replace('T', ' ').substring(0, 19)  // ISO格式 → 本地显示格式
}                                                       // formatTime 函数结束 · 调用方: TransactionListPage / AccountPage / BudgetPage 等

/** 格式化 Date 对象为 'YYYY-MM-DD HH:mm:ss' 字符串（使用本地时间，不涉及 UTC 转换）
 *  安全加固：添加 null/undefined 守卫，防止传入空值时抛出 TypeError
 *  调用方：TransactionListPage.vue（表单数据回填）、AdminPage.vue（用户列表时间显示）
 */
export function formatDateTime(date) {                     // 格式化 Date 对象为 'YYYY-MM-DD HH:mm:ss'（使用本地时间）
  if (!date) return ''                              // 空值守卫（null/undefined/0/''），防止 TypeError
  const pad = (n) => String(n).padStart(2, '0')     // 两位数补零辅助函数（如 5 → '05'）
  // 拼接年-月-日 时:分:秒 格式（月从0开始需+1）
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}                                                       // formatDateTime 函数结束 · 调用方: TransactionListPage.vue（表单回填）/ AdminPage.vue（用户列表时间）

/** 格式化金额为 2 位小数字符串（空值/undefined/null/非数字 默认为 0.00） */
export function formatAmount(val) {                          // 格式化金额数值为 2 位小数字符串
  const num = Number(val)                      // 转为数字类型（空字符串/null/undefined → NaN）
  return isNaN(num) ? '0.00' : num.toFixed(2)  // 非数字返回默认值 '0.00'，否则保留两位小数
}                                                       // formatAmount 函数结束 · 调用方: DashboardPage / AccountPage / TransactionListPage 等

/** 格式化日期为 YYYY-MM-DD（截取前10位，适用于 ISO/后端日期字符串） */
export function formatDate(date) {                           // 格式化日期字符串为 YYYY-MM-DD
  if (!date) return ''                         // 空值返回空字符串
  return String(date).substring(0, 10)         // 截取 YYYY-MM-DD 部分（ISO 格式前 10 位）
}                                                       // formatDate 函数结束 · 调用方: 多页面通用日期显示
