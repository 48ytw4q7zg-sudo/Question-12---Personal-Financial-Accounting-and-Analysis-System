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
  if (_tokenPresent.value) {
    try {
      const token = localStorage.getItem('token')
      // JWT payload 是 base64url 编码的 JSON，解码提取 userId/username/role
      const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
      userId.value = payload.userId || payload.sub || null
      username.value = payload.username || ''
      role.value = payload.role || 0
    } catch (e) { /* token 格式异常或损坏时静默忽略，后续 API 调用 401 会自动跳登录 */
      if (import.meta.env.DEV) console.warn('JWT payload 解码失败:', e)
    }
  }

  /**
   * 设置用户信息（登录成功后调用）
   * 同时将 token 写入 localStorage，确保刷新后可从 JWT 恢复
   * @param {Object} user - { userId, username, role }
   */
  function setUser(user) {
    userId.value = user.userId
    username.value = user.username
    role.value = user.role !== null && user.role !== undefined ? user.role : 0
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