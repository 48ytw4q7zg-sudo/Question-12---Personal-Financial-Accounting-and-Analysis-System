/**
 * 交易/收支记录模块 API 封装（api/transaction.js）
 *
 * 职责：封装收支记录相关的所有 HTTP 请求（分页列表 + 新增 + 编辑 + 删除 + 转账 + CSV 导入）
 * 通过 request.js（Axios 实例）发送请求，统一享受拦截器处理（token 注入 + 401 拦截 + 错误提示）
 *
 * 对应后端接口（对齐 API_DESIGN.md 交易模块接口）：
 *   GET    /api/v1/transaction          → TransactionController.list()     分页 + 多条件筛选列表
 *   POST   /api/v1/transaction          → TransactionController.create()   新增交易记录（记一笔）
 *   PUT    /api/v1/transaction/:id      → TransactionController.update()   编辑交易记录
 *   DELETE /api/v1/transaction/:id      → TransactionController.delete()   删除交易记录
 *   POST   /api/v1/transaction/transfer → TransactionController.transfer() 转账（自动创建 2 条关联记录）
 *   POST   /api/v1/transaction/import   → TransactionController.importCsv() CSV 文件导入
 *
 * 对应 PRD 功能：
 *   - P0 收支记录（记一笔 + 改 + 列表分页）
 *   - P1 多条件筛选（时间范围 + 账户 + 分类 + 关键词）
 *   - P1 转账（两个账户间资金转移）
 *   - P2 数据导入（CSV 文件批量导入交易记录）
 *
 * 调用方（哪些 .vue 文件使用了本模块的导出函数）：
 *   - TransactionListPage.vue → getTransactionList() / createTransaction() / updateTransaction() / deleteTransaction()
 *   - TransferPage.vue → transfer()（转账表单提交）
 *   - ImportPage.vue → importCsv()（CSV 文件上传）
 *
 * 数据流向：
 *   .vue 组件 → api/transaction.js（导出函数）→ request.js（Axios 实例 + 拦截器）→ TransactionController → TransactionServiceImpl → TransactionMapper → MySQL
 *                 ← Result<T> 响应 ← Axios 响应拦截器解析后返回到 .vue 组件
 *
 * 关联文件：
 *   - api/request.js：Axios 实例（baseURL=/api、timeout=10000、请求拦截器注入 token、响应拦截器处理 401/业务错）
 *   - views/TransactionListPage.vue：收支记录列表页（分页表格 + 筛选表单 + 新增/编辑弹窗）
 *   - views/TransferPage.vue：转账页面（源账户/目标账户 + 金额）
 *   - views/ImportPage.vue：CSV 导入页面（文件上传 + 目标账户选择）
 *   - backend/controller/TransactionController.java：交易控制器
 *   - backend/entity/Transaction.java：交易记录实体（表名 transactions）
 */
import request from './request'                              // 导入 Axios 实例（→ api/request.js），包含 baseURL + 拦截器配置

/**
 * 获取交易记录列表（支持分页 + 多条件筛选）
 *
 * 请求详情：GET /api/v1/transaction
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 查询参数（均通过 params 对象传入，Vite proxy 转发到后端 Controller）：
 *   { pageNum, pageSize, startTime?, endTime?, accountId?, categoryId?, type?, keyword? }
 * 响应体：Result<{ records: Transaction[], total: number }>
 *   - records：当前页交易记录数组（每条含 id, accountId, categoryId, type, amount, note, time 等字段）
 *   - total：符合筛选条件的总记录数（用于 el-pagination 分页组件计算总页数）
 *
 * 调用方：TransactionListPage.vue 列表查询 + 分页切换 + 筛选条件变化时
 *
 * @param {Object} params - 查询参数对象
 * @param {number} params.pageNum - 当前页码（从 1 开始，MyBatis-Plus Page 分页）
 * @param {number} params.pageSize - 每页条数（默认 10，前端 el-pagination 控制）
 * @param {string} [params.startTime] - 筛选起始时间（可选，ISO 8601 格式如 '2026-05-01T00:00:00'）
 * @param {string} [params.endTime] - 筛选结束时间（可选）
 * @param {number} [params.accountId] - 筛选账户 ID（可选，null 或不传表示全部账户）
 * @param {number} [params.categoryId] - 筛选分类 ID（可选，null 或不传表示全部分类）
 * @param {string} [params.type] - 交易类型（可选，"income" 收入 / "expense" 支出，不传表示全部）
 * @param {string} [params.keyword] - 关键词搜索（可选，模糊匹配 note 备注字段）
 * @returns {Promise<Object>} - Axios Promise，resolve 后返回 { records, total } 分页结构
 */
export function getTransactionList(params) {
  // request.get(url, { params })：GET 请求，params 对象自动序列化为 URL 查询字符串
  // 示例请求：GET /api/v1/transaction?pageNum=1&pageSize=10&accountId=5&type=expense
  return request.get('/transaction', { params })
}

/**
 * 新增交易记录（记一笔）
 *
 * 请求详情：POST /api/v1/transaction
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 请求体：{ accountId, categoryId, type, amount, note, time }
 * 响应体：Result<Transaction>（返回创建后的完整交易记录对象，含自增 id）
 *
 * 调用方：TransactionListPage.vue "记一笔"弹窗提交
 *
 * @param {Object} data - 交易记录数据
 * @param {number} data.accountId - 关联账户 ID（必填，表示从哪个账户支出或收入到哪个账户）
 * @param {number} data.categoryId - 关联分类 ID（必填，如"餐饮"、"工资"等分类）
 * @param {string} data.type - 交易类型（必填，"income"=收入 / "expense"=支出）
 * @param {number} data.amount - 金额（必填，单位：元，DECIMAL(12,2)，必须 > 0）
 * @param {string} [data.note] - 备注（可选，如"午餐外卖"、"本月工资"）
 * @param {string} data.time - 交易时间（必填，ISO 8601 格式如 '2026-05-23T12:30:00'）
 * @returns {Promise} - Axios Promise
 */
export function createTransaction(data) {
  // 请求路径 '/transaction' 实际拼接为 '/api/v1/transaction'
  return request.post('/transaction', data)
}

/**
 * 编辑交易记录
 *
 * 请求详情：PUT /api/v1/transaction/:id
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 请求体：{ accountId, categoryId, type, amount, note, time }
 * 响应体：Result<Transaction>（返回更新后的完整交易记录对象）
 *
 * 调用方：TransactionListPage.vue 编辑弹窗提交
 *
 * 注意：修改交易记录会导致关联账户的 current_balance 重新计算
 *
 * @param {number} id - 交易记录 ID（数据库主键，自增 ID）
 * @param {Object} data - 要更新的交易数据（字段说明同 createTransaction）
 * @param {number} data.accountId - 关联账户 ID
 * @param {number} data.categoryId - 关联分类 ID
 * @param {string} data.type - 交易类型
 * @param {number} data.amount - 金额
 * @param {string} [data.note] - 备注
 * @param {string} data.time - 交易时间
 * @returns {Promise} - Axios Promise
 */
export function updateTransaction(id, data) {
  // 使用模板字符串拼接 URL 路径参数，如 PUT /api/v1/transaction/42
  return request.put(`/transaction/${id}`, data)
}

/**
 * 删除交易记录
 *
 * 请求详情：DELETE /api/v1/transaction/:id
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 响应体：Result<null>（删除成功无业务数据返回）
 *
 * 调用方：TransactionListPage.vue 列表行点击删除按钮 → ElMessageBox 确认 → 调用此函数
 *
 * 注意：删除交易记录后，关联账户的 current_balance 会相应地回滚（收入则减，支出则加）
 *
 * @param {number} id - 要删除的交易记录 ID
 * @returns {Promise} - Axios Promise
 */
export function deleteTransaction(id) {
  // 发送 DELETE 请求
  return request.delete(`/transaction/${id}`)
}

/**
 * 转账（在两个账户之间转移资金）
 *
 * 请求详情：POST /api/v1/transaction/transfer
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 请求体：{ fromAccountId, toAccountId, amount, note? }
 * 响应体：Result<null>
 *
 * 后端行为：TransactionServiceImpl.transfer() 自动创建 2 条关联交易记录：
 *   1. FROM 账户的支出记录（type=expense）
 *   2. TO 账户的收入记录（type=income）
 *   并使用 @Transactional 保证原子性（两条记录同时成功或同时失败）
 *
 * 调用方：TransferPage.vue 转账表单提交
 *
 * @param {Object} data - 转账数据
 * @param {number} data.fromAccountId - 源账户 ID（钱从这里扣）
 * @param {number} data.toAccountId - 目标账户 ID（钱到达这里，必须与 fromAccountId 不同）
 * @param {number} data.amount - 转账金额（单位：元，DECIMAL(12,2)，必须 > 0）
 * @param {string} [data.note] - 转账备注（可选，如"还钱给张三"、"转入储蓄"）
 * @returns {Promise} - Axios Promise
 */
export function transfer(data) {
  // 请求路径 '/transaction/transfer' 实际拼接为 '/api/v1/transaction/transfer'
  return request.post('/transaction/transfer', data)
}

/**
 * CSV 文件导入交易记录
 *
 * 请求详情：POST /api/v1/transaction/import
 * 请求头：
 *   - Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 *   - Content-Type: multipart/form-data（文件上传，由 Axios 根据 FormData 自动设置）
 * 请求体：FormData { file: File, accountId: string }
 * 响应体：Result<{ successCount: number, failCount: number }>
 *   - successCount：成功导入的记录数
 *   - failCount：解析失败的记录数
 *
 * CSV 文件格式要求（对齐后端 CsvParserUtil）：
 *   type,amount,category,date,note
 *   收入,5000.00,工资,2026-05-15,5月工资
 *   支出,35.50,餐饮,2026-05-16,午餐
 *
 * 调用方：ImportPage.vue 文件选择后点击导入按钮
 *
 * @param {FormData} formData - 包含上传文件和账户 ID 的 FormData 对象
 *   formData.append('file', fileObject)  — CSV 文件对象（el-upload 组件选中的文件）
 *   formData.append('accountId', accountId) — 目标账户 ID（字符串形式）
 * @returns {Promise} - Axios Promise
 */
export function importCsv(formData) {
  // 文件上传需特殊配置：
  //   1. Content-Type 设为 multipart/form-data（浏览器会根据 FormData 自动设置正确的 boundary）
  //   2. timeout 使用 request.js 中的默认 10000ms，大文件可能需要后端调整
  return request.post('/transaction/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }       // 显式声明 multipart/form-data，支持文件上传
  })
}
