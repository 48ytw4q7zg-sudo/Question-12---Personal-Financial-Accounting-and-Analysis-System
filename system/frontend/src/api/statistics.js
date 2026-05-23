/**
 * 统计分析模块 API
 * 职责：封装统计数据相关的 HTTP 请求（月度/年度/分类汇总 + 趋势）
 * 对应后端接口：/api/v1/statistics/*
 * 对应 PRD 功能：
 *   - P0 按账户汇总余额（首页月度统计卡片）
 *   - P1/P2 ECharts 图表（收支趋势图 + 分类饼图 + 预算对比）
 *
 * 调用方：
 *   - DashboardPage.vue → getMonthlySummary / getCategorySummary / getTrend
 *   - AnalyticsPage.vue → getTrend / getCategorySummary
 */
import request from './request'

/**
 * 获取月度收支汇总
 * → 调用 GET /api/v1/statistics/monthly
 * @param {Object} params - { year, month }
 * @returns {Object} - { totalIncome, totalExpense, balance }
 */
export function getMonthlySummary(params) {
  return request.get('/statistics/monthly', { params })
}

/**
 * 获取年度收支汇总
 * → 调用 GET /api/v1/statistics/yearly
 * @param {Object} params - { year }
 * @returns {Object} 年度汇总数据
 */
export function getYearlySummary(params) {
  return request.get('/statistics/yearly', { params })
}

/**
 * 获取分类收支汇总（饼图数据）
 * → 调用 GET /api/v1/statistics/category-summary
 * @param {Object} params - { year, month, type? }  type: 1=支出, 2=收入
 * @returns {Array} - [{ categoryId, categoryName, totalAmount }, ...]
 */
export function getCategorySummary(params) {
  return request.get('/statistics/category-summary', { params })
}

/**
 * 获取月度收支趋势（折线图/柱状图数据）
 * → 调用 GET /api/v1/statistics/trend
 * @param {Object} params - { year }
 * @returns {Array} - [{ month, totalIncome, totalExpense }, ...]
 */
export function getTrend(params) {
  return request.get('/statistics/trend', { params })
}
