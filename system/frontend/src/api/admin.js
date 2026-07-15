/**
 * 管理员 API 模块封装（api/admin.js）
 *
 * 职责：封装管理员专属的后端接口调用（用户管理：查看列表 + 删除用户 + 切换角色）
 * 通过 request.js（Axios 实例）发送请求，统一享受拦截器处理（token 注入 + 401 拦截 + 错误提示）
 *
 * 对应后端接口（对齐 API_DESIGN.md 管理员模块接口）：
 *   GET    /api/v1/admin/users           → AdminController.listUsers()   获取所有用户列表
 *   DELETE /api/v1/admin/users/:userId   → AdminController.deleteUser()  删除指定用户
 *   PUT    /api/v1/admin/users/:userId/role → AdminController.toggleRole()  切换用户角色（普通用户 ↔ 管理员）
 *
 * 权限要求：所有接口需要 JWT 鉴权（请求拦截器自动注入 token）+ 后端校验 role=1（管理员权限）
 * 对应 PRD 功能：管理员用户管理（P1 评分项，满足 ≥2 类角色要求）
 *
 * 调用方（哪些 .vue 文件使用了本模块的导出函数）：
 *   - AdminPage.vue → listUsers() / deleteUser() / toggleRole()
 *
 * 数据流向：
 *   .vue 组件 → api/admin.js（导出函数）→ request.js（Axios 实例 + 拦截器）→ AdminController → UserServiceImpl → UserMapper → MySQL
 *                 ← Result<T> 响应 ← Axios 响应拦截器解析后返回到 .vue 组件
 *
 * 关联文件：
 *   - api/request.js：Axios 实例（baseURL=/api/v1、timeout=10000、请求拦截器注入 token、响应拦截器处理 401/业务错）
 *   - views/AdminPage.vue：管理员用户管理页面（用户表格 + 删除按钮 + 角色切换开关）
 *   - stores/user.js：用户状态管理（AdminPage 读取 role 判断是否管理员、userId 防删自己/降自己）
 *   - router/index.js：路由守卫（仅 role=1 可访问 /admin 路由）
 *   - backend/controller/AdminController.java：管理员控制器
 */
import request from './request'                                    // 导入 Axios 实例（→ api/request.js），包含 baseURL + 拦截器配置

/**
 * 获取所有用户列表（仅管理员可调用）
 *
 * 请求详情：GET /api/v1/admin/users
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入，后端校验 role=1）
 * 响应体：Result<User[]>
 *   每个 User 对象含：{ id, username, role, createTime }
 *   注意：出于安全考虑，后端不返回 password 字段
 *
 * 调用方：AdminPage.vue 页面挂载时获取用户列表并渲染 el-table
 *
 * @returns {Promise<Array>} - Axios Promise，resolve 后返回用户数组
 */
export function listUsers() {                                      // 导出 listUsers 函数（→ AdminPage.vue onMounted 调用）
  return request.get('/admin/users')                               // GET 请求 → /api/v1/admin/users（后端校验 role=1 管理员权限）
}

/**
 * 删除指定用户（仅管理员可调用，不可删除自己）
 *
 * 请求详情：DELETE /api/v1/admin/users/:userId
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入，后端校验 role=1）
 * 响应体：Result<null>（删除成功无业务数据返回）
 *
 * 业务规则：后端校验不允许管理员删除自己（防止误操作导致无管理员可用）
 *
 * 调用方：AdminPage.vue 用户列表行点击删除按钮 → ElMessageBox 确认 → 调用此函数
 *
 * @param {number} userId - 要删除的用户 ID（数据库主键，自增 ID）
 * @returns {Promise} - Axios Promise
 */
export function deleteUser(userId) {                               // 导出 deleteUser 函数（→ AdminPage.vue 删除确认后调用）
  return request.delete(`/admin/users/${userId}`)                  // DELETE 请求 → /api/v1/admin/users/:userId（后端校验不允许删自己）
}

/**
 * 切换用户角色（普通用户 ↔ 管理员，仅管理员可调用，不可降级自己）
 *
 * 请求详情：PUT /api/v1/admin/users/:userId/role
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入，后端校验 role=1）
 * 响应体：Result<User>（返回更新后的用户信息，含新 role 值）
 *
 * 业务规则：
 *   - 后端校验不允许管理员降级自己（防止无管理员可用）
 *   - role 值在 0（普通用户）和 1（管理员）之间切换
 *
 * 调用方：AdminPage.vue 用户列表行点击角色切换开关 → 调用此函数
 *
 * @param {number} userId - 要切换角色的用户 ID
 * @returns {Promise<Object>} - Axios Promise，resolve 后返回更新后的用户信息
 */
export function toggleRole(userId) {                               // 导出 toggleRole 函数（→ AdminPage.vue 角色切换开关调用）
  return request.put(`/admin/users/${userId}/role`)                // PUT 请求 → /api/v1/admin/users/:userId/role（后端校验不允许降自己）
}
