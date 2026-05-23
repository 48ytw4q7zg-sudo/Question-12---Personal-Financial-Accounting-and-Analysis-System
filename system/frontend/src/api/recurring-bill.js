/**
 * 周期性账单模块 API
 * 职责：封装周期性账单相关的 HTTP 请求（CRUD + 手动生成交易记录）
 * 对应后端接口：/api/v1/recurring-bill/*
 * 对应 PRD 功能：P1 周期性账单（周期性收支提醒）
 *
 * 调用方：
 *   - RecurringBillPage.vue → 全部 5 个方法
 */
import request from './request'

/**
 * 获取周期账单列表
 * → 调用 GET /api/v1/recurring-bill
 * @returns {Array} 周期账单数组
 */
export function getRecurringBillList() {
  return request.get('/recurring-bill')
}

/**
 * 新增周期账单
 * → 调用 POST /api/v1/recurring-bill
 * @param {Object} data - { name, accountId, categoryId, amount, type, period, nextDueDate }
 */
export function createRecurringBill(data) {
  return request.post('/recurring-bill', data)
}

/**
 * 编辑周期账单
 * → 调用 PUT /api/v1/recurring-bill/:id
 * @param {Number} id - 账单 ID
 * @param {Object} data - { name, accountId, categoryId, amount, type, period, nextDueDate }
 */
export function updateRecurringBill(id, data) {
  return request.put(`/recurring-bill/${id}`, data)
}

/**
 * 停用/删除周期账单（软删除）
 * → 调用 DELETE /api/v1/recurring-bill/:id
 * @param {Number} id - 账单 ID
 */
export function deleteRecurringBill(id) {
  return request.delete(`/recurring-bill/${id}`)
}

/**
 * 手动生成该周期账单对应的交易记录
 * → 调用 POST /api/v1/recurring-bill/:id/generate
 * @param {Number} id - 账单 ID
 */
export function generateRecurringBill(id) {
  return request.post(`/recurring-bill/${id}/generate`)
}
