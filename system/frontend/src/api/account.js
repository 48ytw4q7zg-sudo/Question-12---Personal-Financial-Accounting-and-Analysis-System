/**
 * 账户模块 API 封装（api/account.js）
 *
 * 职责：封装账户相关的所有 HTTP 请求（CRUD + 余额汇总查询）
 * 通过 request.js（Axios 实例）发送请求，统一享受拦截器处理（token 注入 + 401 拦截 + 错误提示）
 *
 * 对应后端接口（对齐 API_DESIGN.md 账户模块接口）：
 *   GET    /api/v1/account          → AccountController.list()         获取账户列表
 *   POST   /api/v1/account          → AccountController.create()       新增账户
 *   PUT    /api/v1/account/:id      → AccountController.update()       编辑账户
 *   DELETE /api/v1/account/:id      → AccountController.delete()       软删除账户
 *   GET    /api/v1/account/balance  → AccountController.balance()      账户余额汇总
 *
 * 对应 PRD 功能：P0 账户 CRUD（多账户管理，如现金/银行卡/支付宝等）
 *
 * 调用方（多个页面的下拉选择框和表单使用本模块）：
 *   - AccountPage.vue → getAccountList() / createAccount() / updateAccount() / deleteAccount() / getAccountBalance()
 *   - TransactionListPage.vue → getAccountList()（记一笔时的账户下拉选择框）
 *   - RecurringBillPage.vue → getAccountList()（周期账单关联账户下拉框）
 *   - TransferPage.vue → getAccountList()（转账的源/目标账户下拉框）
 *   - ImportPage.vue → getAccountList()（CSV 导入的目标账户下拉框）
 *
 * 数据流向：
 *   .vue 组件 → api/account.js（导出函数）→ request.js（Axios 实例 + 拦截器）→ AccountController → AccountServiceImpl → AccountMapper → MySQL
 *                 ← Result<T> 响应 ← Axios 响应拦截器解析后返回到 .vue 组件
 *
 * 关联文件：
 *   - api/request.js：Axios 实例（baseURL=/api、timeout=10000、请求拦截器注入 token、响应拦截器处理 401/业务错）
 *   - views/AccountPage.vue：账户管理页面（列表 + 新增/编辑弹窗 + 删除确认）
 *   - backend/controller/AccountController.java：账户控制器
 *   - backend/entity/Account.java：账户实体（表名 accounts）
 */
import request from './request'                              // 导入 Axios 实例（→ api/request.js），包含 baseURL + 拦截器配置

/**
 * 获取当前用户的账户列表（含余额信息）
 *
 * 请求详情：GET /api/v1/account
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 响应体：Result<Account[]>
 *   每个 Account 对象含：id, name, type, initialBalance, currentBalance, currency, createTime, updateTime
 *
 * 调用方：AccountPage.vue 列表渲染 / TransactionListPage.vue 下拉框 / TransferPage.vue 下拉框 等
 *
 * @returns {Promise<Array>} - Axios Promise，resolve 后返回账户数组
 */
export function getAccountList() {
  // request.get(url)：发送 GET 请求（无请求体）
  // 请求路径 '/account' 实际拼接为 '/api/v1/account'
  return request.get('/account')
}

/**
 * 新增账户
 *
 * 请求详情：POST /api/v1/account
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 请求体：{ name, type, initialBalance, currency }
 * 响应体：Result<Account>（返回创建后的完整账户对象，含 id 和 currentBalance）
 *
 * 调用方：AccountPage.vue 新增账户弹窗提交
 *
 * @param {Object} data - 账户数据
 * @param {string} data.name - 账户名称（如 "工商银行储蓄卡"、"微信零钱"，必填）
 * @param {string} data.type - 账户类型（如 "bank" 银行卡、"cash" 现金、"alipay" 支付宝等）
 * @param {number} data.initialBalance - 初始余额（单位：元，DECIMAL(12,2)，可为负数表示负债）
 * @param {string} [data.currency='CNY'] - 货币代码（可选，默认 CNY 人民币）
 * @returns {Promise} - Axios Promise
 */
export function createAccount(data) {
  // request.post(url, data)：发送 POST 请求，data 作为 JSON 请求体
  return request.post('/account', data)
}

/**
 * 编辑账户（仅可修改名称/类型/货币，余额通过交易自动更新）
 *
 * 请求详情：PUT /api/v1/account/:id
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 请求体：{ name, type, initialBalance, currency }
 * 响应体：Result<Account>（返回更新后的账户对象）
 *
 * 调用方：AccountPage.vue 编辑账户弹窗提交
 *
 * @param {number} id - 账户 ID（数据库主键，自增 ID）
 * @param {Object} data - 要更新的字段
 * @param {string} data.name - 账户名称
 * @param {string} data.type - 账户类型
 * @param {number} data.initialBalance - 初始余额
 * @param {string} data.currency - 货币代码
 * @returns {Promise} - Axios Promise
 */
export function updateAccount(id, data) {
  // 使用模板字符串拼接 URL 路径参数，如 PUT /api/v1/account/5
  return request.put(`/account/${id}`, data)
}

/**
 * 删除账户（软删除，数据库中设置 is_deleted=1，不物理删除记录）
 *
 * 请求详情：DELETE /api/v1/account/:id
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 响应体：Result<null>（删除成功无业务数据返回）
 *
 * 调用方：AccountPage.vue 列表行点击删除按钮 → ElMessageBox 确认 → 调用此函数
 *
 * 注意：如果账户下有关联交易记录，后端可能拒绝删除（外键约束保护）
 *
 * @param {number} id - 要删除的账户 ID
 * @returns {Promise} - Axios Promise
 */
export function deleteAccount(id) {
  // 发送 DELETE 请求，URL 中的 :id 替换为实际账户 ID
  return request.delete(`/account/${id}`)
}

/**
 * 获取所有账户的余额汇总（DashboardPage 首页概览用）
 *
 * 请求详情：GET /api/v1/account/balance
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 响应体：Result<BalanceSummary[]>
 *   每个 BalanceSummary 对象含：{ accountId, accountName, currentBalance }
 *   所有账户余额之和即为用户总资产
 *
 * 调用方：DashboardPage.vue 首页资产总览卡片 / AccountPage.vue 列表余额列
 *
 * @returns {Promise<Array>} - Axios Promise，resolve 后返回各账户的当前余额数组
 */
export function getAccountBalance() {
  // 请求路径 '/account/balance' 实际拼接为 '/api/v1/account/balance'
  return request.get('/account/balance')
}
