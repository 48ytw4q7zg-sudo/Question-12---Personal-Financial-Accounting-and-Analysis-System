/**
 * 预算模块 API 封装（api/budget.js）
 *
 * 职责：封装预算管理相关的所有 HTTP 请求（查询列表 + 新增/编辑 + 删除 + 执行进度 + 超支告警）
 * 通过 request.js（Axios 实例）发送请求，统一享受拦截器处理（token 注入 + 401 拦截 + 错误提示）
 *
 * 对应后端接口（对齐 API_DESIGN.md 预算模块接口）：
 *   GET    /api/v1/budget          → BudgetController.list()           获取指定年月的预算列表
 *   POST   /api/v1/budget          → BudgetController.save()           新增或更新预算（同一接口，含 upsert 逻辑）
 *   DELETE /api/v1/budget/:id      → BudgetController.delete()         删除指定预算
 *   GET    /api/v1/budget/progress → BudgetController.progress()       获取预算执行进度（含已支出金额和百分比）
 *   GET    /api/v1/budget/alert    → BudgetController.alert()          获取超支告警列表
 *
 * 对应 PRD 功能：P1 预算管理（月预算按分类设置 + 超支标记）
 *
 * 调用方（哪些 .vue 文件使用了本模块的导出函数）：
 *   - BudgetPage.vue → getBudgetList() / saveBudget() / deleteBudget() / getBudgetProgress() / getBudgetAlert()
 *   - DashboardPage.vue → getBudgetAlert()（P2-2 首页预算预警卡片展示）
 *
 * 数据流向：
 *   .vue 组件 → api/budget.js（导出函数）→ request.js（Axios 实例 + 拦截器）→ BudgetController → BudgetServiceImpl → BudgetMapper → MySQL
 *                 ← Result<T> 响应 ← Axios 响应拦截器解析后返回到 .vue 组件
 *
 * 关联文件：
 *   - api/request.js：Axios 实例（baseURL=/api/v1、timeout=10000、请求拦截器注入 token、响应拦截器处理 401/业务错）
 *   - views/BudgetPage.vue：预算管理页面（预算列表 + 新增/编辑弹窗 + 进度条展示 + 超支告警）
 *   - views/DashboardPage.vue：首页概览（P2-2 预算预警卡片）
 *   - backend/controller/BudgetController.java：预算控制器
 *   - backend/entity/Budget.java：预算实体（表名 budgets）
 */
import request from './request'                                    // 导入 Axios 实例（→ api/request.js），包含 baseURL + 拦截器配置

/**
 * 获取预算列表（指定年月）
 *
 * 请求详情：GET /api/v1/budget
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 查询参数：{ year: number, month: number }
 * 响应体：Result<Budget[]>
 *   每个 Budget 对象含：id, categoryId, categoryName, amount, month, spentAmount, createTime
 *
 * 调用方：BudgetPage.vue 页面挂载时获取预算列表并渲染表格
 *
 * @param {Object} params - 查询参数
 * @param {number} params.year - 年份（如 2026）
 * @param {number} params.month - 月份（1-12）
 * @returns {Promise<Array>} - Axios Promise，resolve 后返回预算数组
 */
export function getBudgetList(params) {                            // 导出 getBudgetList 函数（→ BudgetPage.vue onMounted 调用）
  return request.get('/budget', { params })                        // GET 请求，params 对象序列化为 URL 查询字符串 → /api/v1/budget?year=2026&month=5
}

/**
 * 保存/更新预算（新增或编辑同一接口，后端含 upsert 逻辑）
 *
 * 请求详情：POST /api/v1/budget
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 请求体：{ categoryId, amount, month }
 *   - month 格式："YYYY-MM"（如 "2026-05"）
 * 响应体：Result<Budget>（返回保存后的完整预算对象，含自增 id）
 *
 * 调用方：BudgetPage.vue 新增/编辑预算弹窗提交
 *
 * @param {Object} data - 预算数据
 * @param {number} data.categoryId - 分类 ID（必填，关联分类表，仅支出分类可设置预算）
 * @param {number} data.amount - 预算金额（必填，单位：元，DECIMAL(12,2)，必须 > 0）
 * @param {string} data.month - 预算月份（必填，格式 "YYYY-MM"，如 "2026-05"）
 * @returns {Promise} - Axios Promise
 */
export function saveBudget(data) {                                 // 导出 saveBudget 函数（→ BudgetPage.vue 弹窗提交调用）
  return request.post('/budget', data)                             // POST 请求，data 作为 JSON 请求体 → POST /api/v1/budget
}

/**
 * 删除预算
 *
 * 请求详情：DELETE /api/v1/budget/:id
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 响应体：Result<null>（删除成功无业务数据返回）
 *
 * 调用方：BudgetPage.vue 列表行点击删除按钮 → ElMessageBox 确认 → 调用此函数
 *
 * @param {number} id - 预算 ID（数据库主键，自增 ID）
 * @returns {Promise} - Axios Promise
 */
export function deleteBudget(id) {                                 // 导出 deleteBudget 函数（→ BudgetPage.vue 删除确认后调用）
  return request.delete(`/budget/${id}`)                           // DELETE 请求，:id 替换为实际预算 ID → DELETE /api/v1/budget/3
}

/**
 * 获取预算执行进度（包含已支出金额和进度百分比）
 *
 * 请求详情：GET /api/v1/budget/progress
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 查询参数：{ year, month }
 * 响应体：Result<BudgetProgress[]>
 *   每个 BudgetProgress 对象含：{ categoryId, categoryName, budgetAmount, spentAmount, progressPercent }
 *
 * 调用方：BudgetPage.vue 进度条列渲染（el-progress 组件数据源）
 *
 * @param {Object} params - 查询参数
 * @param {number} params.year - 年份
 * @param {number} params.month - 月份（1-12）
 * @returns {Promise<Array>} - Axios Promise，resolve 后返回各分类预算的执行进度数组
 */
export function getBudgetProgress(params) {                        // 导出 getBudgetProgress 函数（→ BudgetPage.vue 进度条展示调用）
  return request.get('/budget/progress', { params })               // GET 请求，params 序列化为 URL 查询字符串 → /api/v1/budget/progress?year=2026&month=5
}

/**
 * 获取预算超支告警列表（当月已支出金额超过预算金额的分类）
 *
 * 请求详情：GET /api/v1/budget/alert
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 查询参数：{ year, month }
 * 响应体：Result<BudgetAlert[]>
 *   每个 BudgetAlert 对象含：{ categoryId, categoryName, budgetAmount, spentAmount, overspendAmount, overspendPercent }
 *
 * 调用方：
 *   - BudgetPage.vue → 超支行高亮红色显示
 *   - DashboardPage.vue → 首页预算预警卡片渲染（P2-2 评分项）
 *
 * @param {Object} params - 查询参数
 * @param {number} params.year - 年份
 * @param {number} params.month - 月份（1-12）
 * @returns {Promise<Array>} - Axios Promise，resolve 后返回超支的预算项数组
 */
export function getBudgetAlert(params) {                           // 导出 getBudgetAlert 函数（→ BudgetPage.vue + DashboardPage.vue 调用）
  return request.get('/budget/alert', { params })                  // GET 请求 → /api/v1/budget/alert?year=2026&month=5
}
