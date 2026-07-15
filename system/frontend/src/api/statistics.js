/**
 * 统计分析模块 API 封装（api/statistics.js）
 *
 * 职责：封装统计数据相关的所有 HTTP 请求（月度汇总 + 年度汇总 + 分类汇总 + 月度趋势）
 * 通过 request.js（Axios 实例）发送请求，统一享受拦截器处理（token 注入 + 401 拦截 + 错误提示）
 *
 * 对应后端接口（对齐 API_DESIGN.md 统计模块接口）：
 *   GET /api/v1/statistics/monthly          → StatisticsController.monthlySummary()     月度收支汇总（收入/支出/结余）
 *   GET /api/v1/statistics/yearly           → StatisticsController.yearlySummary()      年度收支汇总（12 个月数据）
 *   GET /api/v1/statistics/category-summary → StatisticsController.categorySummary()    分类收支汇总（饼图数据源）
 *   GET /api/v1/statistics/trend            → StatisticsController.trend()              月度收支趋势（折线图/柱状图数据源）
 *
 * 对应 PRD 功能：
 *   - P0 按账户汇总余额（DashboardPage 首页月度统计卡片）
 *   - P1/P2 ECharts 图表（收支趋势图 + 分类饼图 + 预算对比）
 *
 * 调用方（哪些 .vue 文件使用了本模块的导出函数）：
 *   - DashboardPage.vue → getMonthlySummary() / getCategorySummary() / getTrend()
 *   - AnalyticsPage.vue → getMonthlySummary() / getYearlySummary() / getCategorySummary() / getTrend()
 *
 * 数据流向：
 *   .vue 组件 → api/statistics.js（导出函数）→ request.js（Axios 实例 + 拦截器）→ StatisticsController → StatisticsServiceImpl → 各 Mapper 聚合查询 → MySQL
 *                 ← Result<T> 响应 ← Axios 响应拦截器解析后返回到 .vue 组件
 *
 * 关联文件：
 *   - api/request.js：Axios 实例（baseURL=/api/v1、timeout=10000、请求拦截器注入 token、响应拦截器处理 401/业务错）
 *   - views/DashboardPage.vue：首页概览（月度统计卡片 + 趋势迷你图 + 分类饼图）
 *   - views/AnalyticsPage.vue：数据分析页面（ECharts 完整图表展示）
 *   - backend/controller/StatisticsController.java：统计控制器
 */
import request from './request'                                    // 导入 Axios 实例（→ api/request.js），包含 baseURL + 拦截器配置

/**
 * 获取月度收支汇总（首页统计卡片数据源）
 *
 * 请求详情：GET /api/v1/statistics/monthly
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 查询参数：{ year: number, month: number }
 * 响应体：Result<MonthlySummary>
 *   字段：{ totalIncome: number, totalExpense: number, balance: number }
 *   - totalIncome：当月所有 type=income 交易金额之和
 *   - totalExpense：当月所有 type=expense 交易金额之和（取绝对值）
 *   - balance = totalIncome - totalExpense（当月结余）
 *
 * 调用方：DashboardPage.vue 首页统计卡片（"本月收入"/"本月支出"/"本月结余"）
 *
 * @param {Object} params - 查询参数
 * @param {number} params.year - 年份（如 2026）
 * @param {number} params.month - 月份（1-12）
 * @returns {Promise<Object>} - Axios Promise，resolve 后返回 { totalIncome, totalExpense, balance }
 */
export function getMonthlySummary(params) {                        // 导出 getMonthlySummary 函数（→ DashboardPage.vue + AnalyticsPage.vue 调用）
  return request.get('/statistics/monthly', { params })            // GET 请求，params 序列化为 URL 查询字符串 → /api/v1/statistics/monthly?year=2026&month=5
}

/**
 * 获取年度收支汇总（包含 1-12 月每月汇总）
 *
 * 请求详情：GET /api/v1/statistics/yearly
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 查询参数：{ year: number }
 * 响应体：Result<YearlySummary>
 *   字段：{ totalIncome: number, totalExpense: number, balance: number, monthly: MonthlyData[] }
 *   monthly 数组每项含：{ month: number, income: number, expense: number, balance: number }
 *
 * 调用方：AnalyticsPage.vue 年度汇总概览区域
 *
 * @param {Object} params - 查询参数
 * @param {number} params.year - 年份（如 2026）
 * @returns {Promise<Object>} - Axios Promise，resolve 后返回年度汇总数据
 */
export function getYearlySummary(params) {                         // 导出 getYearlySummary 函数（→ AnalyticsPage.vue 年度汇总调用）
  return request.get('/statistics/yearly', { params })             // GET 请求 → /api/v1/statistics/yearly?year=2026
}

/**
 * 获取分类收支汇总（ECharts 饼图数据源）
 *
 * 请求详情：GET /api/v1/statistics/category-summary
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 查询参数：{ year, month, type? }
 *   - type 可选：1=支出 / 2=收入（不传则返回全部）
 * 响应体：Result<CategorySummary[]>
 *   每项含：{ categoryId, categoryName, totalAmount, percentage }
 *
 * 调用方：
 *   - DashboardPage.vue → 首页支出分类饼图（type=1）
 *   - AnalyticsPage.vue → 分析页分类饼图（支持切换收入/支出）
 *
 * @param {Object} params - 查询参数
 * @param {number} params.year - 年份
 * @param {number} params.month - 月份（1-12）
 * @param {number} [params.type] - 分类类型（可选，1=支出 / 2=收入，不传返回全部）
 * @returns {Promise<Array>} - Axios Promise，resolve 后返回各分类汇总金额数组
 */
export function getCategorySummary(params) {                       // 导出 getCategorySummary 函数（→ DashboardPage.vue + AnalyticsPage.vue ECharts 饼图调用）
  return request.get('/statistics/category-summary', { params })   // GET 请求 → /api/v1/statistics/category-summary?year=2026&month=5&type=1
}

/**
 * 获取月度收支趋势（ECharts 折线图/柱状图数据源）
 *
 * 请求详情：GET /api/v1/statistics/trend
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 查询参数：{ year: number }
 * 响应体：Result<TrendData[]>
 *   每项含：{ month: number, totalIncome: number, totalExpense: number, balance: number }
 *   按月份升序排列（1 月 → 12 月），即使某月无数据也返回 balance=0
 *
 * 调用方：
 *   - DashboardPage.vue → 首页月度收支趋势迷你图
 *   - AnalyticsPage.vue → 分析页完整趋势图（支持切换折线图/柱状图）
 *
 * @param {Object} params - 查询参数
 * @param {number} params.year - 年份（如 2026）
 * @returns {Promise<Array>} - Axios Promise，resolve 后返回 12 条月度趋势数据
 */
export function getTrend(params) {                                 // 导出 getTrend 函数（→ DashboardPage.vue + AnalyticsPage.vue ECharts 趋势图调用）
  return request.get('/statistics/trend', { params })              // GET 请求 → /api/v1/statistics/trend?year=2026
}
