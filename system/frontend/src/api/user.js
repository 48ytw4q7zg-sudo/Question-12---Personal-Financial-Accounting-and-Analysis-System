/**
 * 用户模块 API
 * 职责：封装用户相关的 HTTP 请求（登录、注册、修改密码）
 * 对应后端接口：/api/v1/user/*
 *
 * 调用方：
 *   - LoginPage.vue → login() / register()
 *   - UserSettingsPage.vue → changePassword()
 */
import request from './request'

/**
 * 用户登录
 * → 调用 POST /api/v1/user/login
 * @param {Object} data - { username, password }
 * @returns {Object} - { token, userId, username, role }
 */
export function login(data) {
  return request.post('/user/login', data)
}

/**
 * 用户注册
 * → 调用 POST /api/v1/user/register
 * @param {Object} data - { username, password }
 */
export function register(data) {
  return request.post('/user/register', data)
}

/**
 * 修改密码（需登录）
 * → 调用 POST /api/v1/user/change-password
 * @param {Object} data - { oldPassword, newPassword }
 */
export function changePassword(data) {
  return request.post('/user/change-password', data)
}
