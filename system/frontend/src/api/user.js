/**
 * 用户模块 API 封装（api/user.js）
 *
 * 职责：封装用户相关的所有 HTTP 请求（登录、注册、修改密码）
 * 通过 request.js（Axios 实例）发送请求，统一享受拦截器处理（token 注入 + 401 拦截 + 错误提示）
 *
 * 对应后端接口（对齐 API_DESIGN.md 用户模块接口）：
 *   POST /api/v1/user/login          → UserController.login()
 *   POST /api/v1/user/register       → UserController.register()
 *   POST /api/v1/user/change-password → UserController.changePassword()
 *
 * 调用方（哪些 .vue 文件使用了本模块的导出函数）：
 *   - LoginPage.vue → login() / register()（登录页表单提交时调用）
 *   - UserSettingsPage.vue → changePassword()（个人设置页修改密码弹窗中调用）
 *
 * 数据流向：
 *   .vue 组件 → api/user.js（导出函数）→ request.js（Axios 实例 + 拦截器）→ 后端 Controller → Service → Mapper → MySQL
 *                 ← Result<T> 响应 ← Axios 响应拦截器解析后返回到 .vue 组件
 *
 * 关联文件：
 *   - api/request.js：Axios 实例（baseURL=/api、timeout=10000、请求拦截器注入 token、响应拦截器处理 401/业务错）
 *   - views/LoginPage.vue：调用 login() / register()
 *   - views/UserSettingsPage.vue：调用 changePassword()
 */
import request from './request'                              // 导入 Axios 实例（→ api/request.js），包含 baseURL + 拦截器配置

/**
 * 用户登录
 *
 * 请求详情：POST /api/v1/user/login
 * 请求体：{ username: string, password: string }
 * 响应体：Result<{ token: string, userId: number, username: string, role: number }>
 *   - token：JWT 令牌（后续请求通过 Authorization: Bearer <token> 携带）
 *   - role：用户角色（1=管理员 ROLE_ADMIN，0=普通用户）
 *
 * 调用方：LoginPage.vue 登录表单提交 → 成功后将 token 存入 localStorage + username/role 存入 Pinia userStore
 *
 * @param {Object} data - 登录表单数据
 * @param {string} data.username - 用户名（必填，2-20 字符）
 * @param {string} data.password - 密码（必填，6-20 字符，明文传输 → 后端 BCrypt 比对）
 * @returns {Promise} - Axios Promise，resolve 后返回 Result 对象 { code: 200, data: { token, userId, username, role } }
 */
export function login(data) {
  // request.post(url, data)：发送 POST 请求，data 作为 JSON 体
  // 请求路径 '/user/login' 实际拼接为 '/api/v1/user/login'（baseURL 已在 request.js 中配置）
  return request.post('/user/login', data)
}

/**
 * 用户注册
 *
 * 请求详情：POST /api/v1/user/register
 * 请求体：{ username: string, password: string }
 * 响应体：Result<null>（注册成功无业务数据返回）
 *
 * 调用方：LoginPage.vue 注册表单提交 → 成功后自动跳转登录页提示"注册成功，请登录"
 *
 * @param {Object} data - 注册表单数据
 * @param {string} data.username - 用户名（必填，2-20 字符，不可与已有用户重复）
 * @param {string} data.password - 密码（必填，6-20 字符，后端用 BCrypt 加密后存入数据库）
 * @returns {Promise} - Axios Promise
 */
export function register(data) {
  // 请求路径 '/user/register' 实际拼接为 '/api/v1/user/register'
  return request.post('/user/register', data)
}

/**
 * 修改密码（需登录，请求头自动携带 JWT token）
 *
 * 请求详情：POST /api/v1/user/change-password
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 请求体：{ oldPassword: string, newPassword: string }
 * 响应体：Result<null>（修改成功无业务数据返回）
 *
 * 调用方：UserSettingsPage.vue 修改密码弹窗提交 → 成功后提示"密码修改成功"
 *
 * @param {Object} data - 密码修改数据
 * @param {string} data.oldPassword - 旧密码（用于验证身份）
 * @param {string} data.newPassword - 新密码（6-20 字符，后端 BCrypt 加密后更新数据库）
 * @returns {Promise} - Axios Promise
 */
export function changePassword(data) {
  // 请求路径 '/user/change-password' 实际拼接为 '/api/v1/user/change-password'
  return request.post('/user/change-password', data)
}
