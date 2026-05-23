/**
 * 分类模块 API
 * 职责：封装分类相关的 HTTP 请求（仅查询列表）
 * 对应后端接口：GET /api/v1/category
 * 对应 PRD 功能：P0 分类 GET 列表（种子数据，不做增改删）
 *
 * 调用方：
 *   - CategoryPage.vue → getCategoryList（分类浏览页面）
 *   - TransactionListPage.vue → getCategoryList（筛选/表单下拉选项）
 *   - BudgetPage.vue → getCategoryList（筛选支出分类）
 *   - RecurringBillPage.vue → getCategoryList（表单下拉选项）
 */
import request from './request'

/**
 * 获取全部分类列表（包含收入和支出分类）
 * → 调用 GET /api/v1/category
 * @returns {Array} - [{ id, name, type }, ...]  type: 1=支出, 2=收入
 */
export function getCategoryList() {
  return request.get('/category')
}
