/**
 * 账户模块 API
 * 职责：封装账户相关的 HTTP 请求（CRUD + 余额查询）
 * 对应后端接口：/api/account/*
 * 对应 PRD 功能：P0 账户 CRUD（多账户管理）
 *
 * 调用方：
 *   - AccountPage.vue → getAccountList / createAccount / updateAccount / deleteAccount / getAccountBalance
 *   - TransactionListPage.vue → getAccountList（下拉选项）
 *   - RecurringBillPage.vue → getAccountList（下拉选项）
 *   - TransferPage.vue → getAccountList（下拉选项）
 *   - ImportPage.vue → getAccountList（下拉选项）
 */
import request from './request'

/**
 * 获取账户列表
 * → 调用 GET /api/account
 * @returns {Array} 账户数组
 */
export function getAccountList() {
  return request.get('/account')
}

/**
 * 新增账户
 * → 调用 POST /api/account
 * @param {Object} data - { name, type, initialBalance, currency }
 */
export function createAccount(data) {
  return request.post('/account', data)
}

/**
 * 编辑账户
 * → 调用 PUT /api/account/:id
 * @param {Number} id - 账户 ID
 * @param {Object} data - { name, type, initialBalance, currency }
 */
export function updateAccount(id, data) {
  return request.put(`/account/${id}`, data)
}

/**
 * 删除账户（软删除）
 * → 调用 DELETE /api/account/:id
 * @param {Number} id - 账户 ID
 */
export function deleteAccount(id) {
  return request.delete(`/account/${id}`)
}

/**
 * 获取账户余额汇总（按账户统计当前余额）
 * → 调用 GET /api/account/balance
 * @returns {Array} - [{ accountId, accountName, currentBalance }, ...]
 */
export function getAccountBalance() {
  return request.get('/account/balance')
}
