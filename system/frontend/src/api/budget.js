/**
 * 预算模块 API
 * 职责：封装预算管理相关的 HTTP请求（查询、设置、删除、进度、告警）
 * 对应后端接口：/api/budget/*
 * 对应 PRD 功能：P1 预算管理（月预算按分类设置 + 超支标记）
 *
 * 调用方：
 *   - BudgetPage.vue → getBudgetProgress / getBudgetAlert / saveBudget / deleteBudget
 *   - DashboardPage.vue → getBudgetAlert（P2-2 预算预警展示）
 */
import request from './request'

/**
 * 获取预算列表
 * → 调用 GET /api/budget
 * @param {Object} params - { year, month }
 * @returns {Array} 预算列表
 */
export function getBudgetList(params) {
  return request.get('/budget', { params })
}

/**
 * 保存/更新预算（新增或编辑同一接口）
 * → 调用 POST /api/budget
 * @param {Object} data - { categoryId, amount, month }  month 格式: "YYYY-MM"
 */
export function saveBudget(data) {
  return request.post('/budget', data)
}

/**
 * 删除预算
 * → 调用 DELETE /api/budget/:id
 * @param {Number} id - 预算 ID
 */
export function deleteBudget(id) {
  return request.delete(`/budget/${id}`)
}

/**
 * 获取预算执行进度（包含已支出金额和进度百分比）
 * → 调用 GET /api/budget/progress
 * @param {Object} params - { year, month }
 * @returns {Array} - [{ categoryId, categoryName, budgetAmount, spentAmount }, ...]
 */
export function getBudgetProgress(params) {
  return request.get('/budget/progress', { params })
}

/**
 * 获取预算超支告警
 * → 调用 GET /api/budget/alert
 * @param {Object} params - { year, month }
 * @returns {Array} 超支的预算项列表
 */
export function getBudgetAlert(params) {
  return request.get('/budget/alert', { params })
}
