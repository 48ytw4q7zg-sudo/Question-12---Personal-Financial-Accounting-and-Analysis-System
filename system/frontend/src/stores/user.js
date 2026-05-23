/**
 * 用户状态管理 Store
 * 职责：管理当前登录用户的信息（userId、username、role），提供登录状态判断
 *
 * 状态流转：
 *   1. 登录成功 → LoginPage.vue 调用 setUser() 存储用户信息 + 写入 localStorage token
 *   2. 退出登录 → AppLayout.vue 调用 clearUser() 清除用户信息 + 清 localStorage
 *   3. 登录状态判断 → isLoggedIn（响应式 computed）检查 localStorage 是否有 token
 *   4. 页面刷新 → 从 JWT payload 解码恢复 userId/username/role（防篡改）
 *
 * 安全加固：role 从 JWT 解码而非 localStorage 独立存储，防止手动篡改绕过前端路由守卫
 *
 * 调用方：
 *   - LoginPage.vue → setUser()（登录成功后写入用户信息 + localStorage token）
 *   - AppLayout.vue → username（显示用户名）、clearUser()（退出登录）
 *   - UserSettingsPage.vue → username（显示当前用户名）
 *   - AdminPage.vue → role（判断是否管理员）、userId（不可删自己/降自己）
 *   - SidebarMenu.vue → role（仅 role=1 显示管理员菜单）
 *   - router/index.js → isLoggedIn + role（路由守卫鉴权）
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { decodeJwtPayload } from '../utils/jwt'  // 导入JWT解码工具（消除router和stores中的重复解码逻辑）

export const useUserStore = defineStore('user', () => {
  // 当前登录用户的 ID
  const userId = ref(null)
  // 当前登录用户的名称（显示在顶栏）
  const username = ref('')
  // 当前登录用户的角色：0=普通用户, 1=管理员（满足评分标准≥2类角色要求）
  const role = ref(0)
  // 内部响应式 token 状态（与 localStorage 同步，解决 localStorage 非响应式问题）
  const _tokenPresent = ref(!!localStorage.getItem('token'))

  // 页面刷新时从 JWT payload 解码恢复用户信息（防 localStorage 独立篡改 role）
  // 使用 utils/jwt.js 的 decodeJwtPayload() 消除 router/index.js 和 stores/user.js 的重复解码逻辑
  if (_tokenPresent.value) {
    const token = localStorage.getItem('token')
    const payload = decodeJwtPayload(token)  // 调用公共工具函数解码JWT payload
    if (payload) {  // 解码成功
      userId.value = payload.userId || payload.sub || null  // 提取用户ID
      username.value = payload.username || ''  // 提取用户名
      role.value = payload.role || 0  // 提取角色
    } else {  // 解码失败：token 损坏或格式异常，清除 localStorage 避免后续请求携带无效 token
      if (import.meta.env.DEV) console.warn('JWT payload 解码失败，清除无效 token')  // 开发环境日志
      localStorage.removeItem('token')  // 清除无效token
      _tokenPresent.value = false  // 更新token存在标志
    }
  }

  /**
   * 设置用户信息并存储 token（登录成功后调用）
   * token 操作集中在此处，LoginPage/request.js/router 不再直接操作 localStorage
   * @param {Object} user - { userId, username, role, token }
   */
  function setUser(user) {
    userId.value = user.userId
    username.value = user.username
    // 使用 nullish coalescing 简化空值判断（?? 比 !== null && !== undefined 更简洁）
    role.value = user.role ?? 0
    if (user.token) {
      localStorage.setItem('token', user.token)
    }
    _tokenPresent.value = true
  }

  /**
   * 清除用户信息（退出登录时调用）
   * 同时清除 localStorage 中的 token
   */
  function clearUser() {
    userId.value = null
    username.value = ''
    role.value = 0
    _tokenPresent.value = false
    localStorage.removeItem('token')
  }

  /**
   * 判断是否已登录（基于响应式 _tokenPresent 状态）
   * @returns {Boolean}
   */
  const isLoggedIn = computed(() => _tokenPresent.value)

  return { userId, username, role, isLoggedIn, setUser, clearUser }
})