/**
 * 用户状态管理 Store
 * 职责：管理当前登录用户的信息（userId、username），提供登录状态判断
 *
 * 状态流转：
 *   1. 登录成功 → LoginPage.vue 调用 setUser() 存储用户信息
 *   2. 退出登录 → AppLayout.vue 调用 clearUser() 清除用户信息 + 清 localStorage token
 *   3. 登录状态判断 → isLoggedIn() 检查 localStorage 是否有 token
 *
 * 调用方：
 *   - LoginPage.vue → setUser()（登录成功后写入用户信息）
 *   - AppLayout.vue → username（显示用户名）、clearUser()（退出登录）
 *   - UserSettingsPage.vue → username（显示当前用户名）
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  // 当前登录用户的 ID（从登录接口返回的 token 解析或直接返回）
  const userId = ref(null)
  // 当前登录用户的名称（显示在顶栏）
  const username = ref('')

  /**
   * 设置用户信息（登录成功后调用）
   * @param {Object} user - { userId, username }
   */
  function setUser(user) {
    userId.value = user.userId
    username.value = user.username
  }

  /**
   * 清除用户信息（退出登录时调用）
   */
  function clearUser() {
    userId.value = null
    username.value = ''
  }

  /**
   * 判断是否已登录（通过检查 localStorage 中是否存在 token）
   * @returns {Boolean}
   */
  function isLoggedIn() {
    return !!localStorage.getItem('token')
  }

  return { userId, username, setUser, clearUser, isLoggedIn }
})
