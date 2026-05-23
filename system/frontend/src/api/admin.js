/**
 * 管理员 API 模块
 * 职责: 封装管理员相关的后端接口调用（用户管理）
 *
 * 对应后端: AdminController.java (/api/v1/admin/**)
 * 所有接口需要 JWT 鉴权 + role=1(管理员权限)
 *
 * 调用方: → AdminPage.vue
 */
import request from './request'

/**
 * 获取所有用户列表（仅管理员）
 * → 调用 GET /api/v1/admin/users
 * @returns {Promise<Array>} 用户列表 [{ id, username, role, createTime }]
 */
export function listUsers() {
  return request.get('/admin/users')
}

/**
 * 删除指定用户（仅管理员）
 * → 调用 DELETE /api/v1/admin/users/:userId
 * @param {Number} userId - 用户ID
 * @returns {Promise}
 */
export function deleteUser(userId) {
  return request.delete(`/admin/users/${userId}`)
}

/**
 * 切换用户角色（普通用户↔管理员 · 仅管理员）
 * → 调用 PUT /api/v1/admin/users/:userId/role
 * @param {Number} userId - 用户ID
 * @returns {Promise<Object>} 更新后的用户信息
 */
export function toggleRole(userId) {
  return request.put(`/admin/users/${userId}/role`)
}