/**
 * 交易记录模块 API
 * 职责：封装交易/收支记录相关的 HTTP 请求
 * 对应后端接口：/api/transaction/*
 * 对应 PRD 功能：
 *   - P0 收支记录（记一笔 + 改 + 列表分页）
 *   - P1 多条件筛选
 *   - P1 转账
 *   - P2 数据导入
 *
 * 调用方：
 *   - TransactionListPage.vue → getTransactionList / createTransaction / updateTransaction
 *   - TransferPage.vue → transfer
 *   - ImportPage.vue → importCsv
 */
import request from './request'

/**
 * 获取交易记录列表（支持分页 + 多条件筛选）
 * → 调用 GET /api/transaction
 * @param {Object} params - { pageNum, pageSize, startTime?, endTime?, accountId?, categoryId?, keyword? }
 * @returns {Object} - { records, total } 或分页结构
 */
export function getTransactionList(params) {
  return request.get('/transaction', { params })
}

/**
 * 新增交易记录（记一笔）
 * → 调用 POST /api/transaction
 * @param {Object} data - { accountId, categoryId, type, amount, note, time }
 */
export function createTransaction(data) {
  return request.post('/transaction', data)
}

/**
 * 编辑交易记录
 * → 调用 PUT /api/transaction/:id
 * @param {Number} id - 交易记录 ID
 * @param {Object} data - { accountId, categoryId, type, amount, note, time }
 */
export function updateTransaction(id, data) {
  return request.put(`/transaction/${id}`, data)
}

/**
 * 转账（在两个账户间转移资金，后端自动创建一对关联的收支记录）
 * → 调用 POST /api/transaction/transfer
 * @param {Object} data - { fromAccountId, toAccountId, amount, note? }
 */
export function transfer(data) {
  return request.post('/transaction/transfer', data)
}

/**
 * CSV 数据导入（上传文件）
 * → 调用 POST /api/transaction/import
 * @param {FormData} formData - 包含 file（CSV 文件）和 accountId（目标账户 ID）
 */
export function importCsv(formData) {
  return request.post('/transaction/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
