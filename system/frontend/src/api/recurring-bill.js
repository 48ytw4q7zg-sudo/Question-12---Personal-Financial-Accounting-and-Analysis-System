/**
 * 周期性账单模块 API 封装（api/recurring-bill.js）
 *
 * 职责：封装周期性账单相关的所有 HTTP 请求（CRUD + 手动生成交易记录）
 * 通过 request.js（Axios 实例）发送请求，统一享受拦截器处理（token 注入 + 401 拦截 + 错误提示）
 *
 * 对应后端接口（对齐 API_DESIGN.md 周期账单模块接口）：
 *   GET    /api/v1/recurring-bill             → RecurringBillController.list()     获取周期账单列表
 *   POST   /api/v1/recurring-bill             → RecurringBillController.create()   新增周期账单
 *   PUT    /api/v1/recurring-bill/:id         → RecurringBillController.update()   编辑周期账单
 *   DELETE /api/v1/recurring-bill/:id         → RecurringBillController.delete()   软删除周期账单
 *   POST   /api/v1/recurring-bill/:id/generate → RecurringBillController.generate() 手动生成该周期账单对应的交易记录
 *
 * 对应 PRD 功能：P1 周期性账单（周期性收支提醒，如每月房租、订阅费、工资等）
 * 业务说明：
 *   - 周期类型（period）：monthly=每月 / weekly=每周 / yearly=每年
 *   - nextDueDate：下一个到期日，后端会自动根据 period 计算下一个周期
 *   - generate 操作：手动触发，将该周期账单生成一条实际交易记录写入 transactions 表
 *
 * 调用方（哪些 .vue 文件使用了本模块的导出函数）：
 *   - RecurringBillPage.vue → 全部 5 个方法（列表 + 新增/编辑弹窗 + 删除确认 + 生成按钮）
 *
 * 数据流向：
 *   .vue 组件 → api/recurring-bill.js（导出函数）→ request.js（Axios 实例 + 拦截器）→ RecurringBillController → RecurringBillServiceImpl → RecurringBillMapper → MySQL
 *                 ← Result<T> 响应 ← Axios 响应拦截器解析后返回到 .vue 组件
 *
 * 关联文件：
 *   - api/request.js：Axios 实例（baseURL=/api/v1、timeout=10000、请求拦截器注入 token、响应拦截器处理 401/业务错）
 *   - views/RecurringBillPage.vue：周期账单管理页面（列表 + 新增/编辑弹窗 + 生成按钮）
 *   - backend/controller/RecurringBillController.java：周期账单控制器
 *   - backend/entity/RecurringBill.java：周期账单实体（表名 recurring_bills）
 */
import request from './request'                                    // 导入 Axios 实例（→ api/request.js），包含 baseURL + 拦截器配置

/**
 * 获取当前用户的周期账单列表
 *
 * 请求详情：GET /api/v1/recurring-bill
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 响应体：Result<RecurringBill[]>
 *   每个 RecurringBill 对象含：id, name, accountId, accountName, categoryId, categoryName,
 *   amount, type, period, nextDueDate, isActive, createTime
 *
 * 调用方：RecurringBillPage.vue 页面挂载时获取列表并渲染表格
 *
 * @returns {Promise<Array>} - Axios Promise，resolve 后返回周期账单数组
 */
export function getRecurringBillList() {                           // 导出 getRecurringBillList 函数（→ RecurringBillPage.vue onMounted 调用）
  return request.get('/recurring-bill')                            // GET 请求 → /api/v1/recurring-bill（返回当前用户所有周期账单）
}

/**
 * 新增周期账单
 *
 * 请求详情：POST /api/v1/recurring-bill
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 请求体：{ name, accountId, categoryId, amount, type, period, nextDueDate }
 * 响应体：Result<RecurringBill>（返回创建后的完整周期账单对象，含自增 id）
 *
 * 调用方：RecurringBillPage.vue 新增弹窗提交
 *
 * @param {Object} data - 周期账单数据
 * @param {string} data.name - 账单名称（如 "房租"、"Netflix 订阅"、"工资"，必填）
 * @param {number} data.accountId - 关联账户 ID（必填，表示该账单从哪个账户扣款或收入）
 * @param {number} data.categoryId - 关联分类 ID（必填，如 "居住"、"订阅服务"、"工资"）
 * @param {number} data.amount - 金额（必填，单位：元，DECIMAL(12,2)，必须 > 0）
 * @param {string} data.type - 交易类型（必填，"income"=收入 / "expense"=支出）
 * @param {string} data.period - 周期类型（必填，"monthly"=每月 / "weekly"=每周 / "yearly"=每年）
 * @param {string} data.nextDueDate - 下一个到期日（必填，ISO 8601 格式如 "2026-06-01"）
 * @returns {Promise} - Axios Promise
 */
export function createRecurringBill(data) {                        // 导出 createRecurringBill 函数（→ RecurringBillPage.vue 新增弹窗提交调用）
  return request.post('/recurring-bill', data)                     // POST 请求，data 作为 JSON 请求体 → POST /api/v1/recurring-bill
}

/**
 * 编辑周期账单
 *
 * 请求详情：PUT /api/v1/recurring-bill/:id
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 请求体：{ name, accountId, categoryId, amount, type, period, nextDueDate }
 * 响应体：Result<RecurringBill>（返回更新后的完整周期账单对象）
 *
 * 调用方：RecurringBillPage.vue 编辑弹窗提交
 *
 * @param {number} id - 周期账单 ID（数据库主键，自增 ID）
 * @param {Object} data - 要更新的账单数据（字段说明同 createRecurringBill）
 * @param {string} data.name - 账单名称
 * @param {number} data.accountId - 关联账户 ID
 * @param {number} data.categoryId - 关联分类 ID
 * @param {number} data.amount - 金额
 * @param {string} data.type - 交易类型
 * @param {string} data.period - 周期类型
 * @param {string} data.nextDueDate - 下一个到期日
 * @returns {Promise} - Axios Promise
 */
export function updateRecurringBill(id, data) {                    // 导出 updateRecurringBill 函数（→ RecurringBillPage.vue 编辑弹窗提交调用）
  return request.put(`/recurring-bill/${id}`, data)                // PUT 请求，:id 替换为实际账单 ID → PUT /api/v1/recurring-bill/5
}

/**
 * 停用/删除周期账单（软删除，数据库中设置 is_deleted=1）
 *
 * 请求详情：DELETE /api/v1/recurring-bill/:id
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 响应体：Result<null>（删除成功无业务数据返回）
 *
 * 调用方：RecurringBillPage.vue 列表行点击删除按钮 → ElMessageBox 确认 → 调用此函数
 *
 * 注意：软删除后账单不再出现在列表中，但数据库中保留记录以备审计
 *
 * @param {number} id - 要删除的周期账单 ID
 * @returns {Promise} - Axios Promise
 */
export function deleteRecurringBill(id) {                          // 导出 deleteRecurringBill 函数（→ RecurringBillPage.vue 删除确认后调用）
  return request.delete(`/recurring-bill/${id}`)                   // DELETE 请求 → /api/v1/recurring-bill/:id（软删除）
}

/**
 * 手动生成该周期账单对应的交易记录（触发一次实际的收支记录写入）
 *
 * 请求详情：POST /api/v1/recurring-bill/:id/generate
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 响应体：Result<Transaction>（返回生成的实际交易记录对象）
 *
 * 业务逻辑（后端 RecurringBillServiceImpl.generate()）：
 *   1. 根据周期账单信息创建一条 transaction 记录
 *   2. 更新关联账户的 current_balance
 *   3. 自动计算并更新 nextDueDate（如 monthly 则 +1 月）
 *
 * 调用方：RecurringBillPage.vue 列表行点击"生成"按钮 → 确认后调用此函数
 *
 * @param {number} id - 周期账单 ID
 * @returns {Promise} - Axios Promise，resolve 后返回生成的交易记录对象
 */
export function generateRecurringBill(id) {                        // 导出 generateRecurringBill 函数（→ RecurringBillPage.vue 生成按钮调用）
  return request.post(`/recurring-bill/${id}/generate`)            // POST 请求 → /api/v1/recurring-bill/:id/generate（手动触发生成）
}
