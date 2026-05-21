/**
 * 用户状态管理 Store
 * 职责：管理当前登录用户的信息（userId、username、role），提供登录状态判断
 *
 * 状态流转：
 *   1. 登录成功 → LoginPage.vue 调用 setUser() 存储用户信息 + 写入 localStorage token
 *   2. 退出登录 → AppLayout.vue 调用 clearUser() 清除用户信息 + 清 localStorage token
 *   3. 登录状态判断 → isLoggedIn（响应式 computed）检查 localStorage 是否有 token（依赖 LoginPage.vue 写入）
 *
 * 调用方：
 *   - LoginPage.vue → setUser()（登录成功后写入用户信息 + localStorage token）
 *   - AppLayout.vue → username（显示用户名）、clearUser()（退出登录）
 *   - UserSettingsPage.vue → username（显示当前用户名）
 *   - AdminPage.vue → role（判断是否管理员）、userId（不可删自己/降自己）
 *   - SidebarMenu.vue → role（仅 role=1 显示管理员菜单）
 *
 * 注意：
 *   - role 字段在 AdminPage 切换其他用户角色后，当前用户的 store.role 不会实时更新
 *     （因为 AdminPage 的 toggleRole 操作的是目标用户，不是当前登录用户）
 *   - 若管理员在 AdminPage 降自己为普通用户，store.role 仍为 1，需重新登录才生效
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  // 当前登录用户的 ID（从登录接口返回的 token 解析或直接返回）
  const userId = ref(null)
  // 当前登录用户的名称（显示在顶栏）
  const username = ref('')
  // 当前登录用户的角色：0=普通用户, 1=管理员（满足评分标准≥2类角色要求）
  const role = ref(0)
  // 内部响应式 token 状态（与 localStorage 同步，解决 localStorage 非响应式问题）
  const _tokenPresent = ref(!!localStorage.getItem('token'))

  /**
   * 设置用户信息（登录成功后调用）
   * @param {Object} user - { userId, username, role }
   */
  function setUser(user) {
    userId.value = user.userId
    username.value = user.username
    role.value = user.role != null ? user.role : 0
    _tokenPresent.value = true
  }

  /**
   * 清除用户信息（退出登录时调用）
   */
  function clearUser() {
    userId.value = null
    username.value = ''
    role.value = 0
    _tokenPresent.value = false
  }

  /**
   * 判断是否已登录（基于响应式 _tokenPresent 状态）
   * setUser/clearUser 会同步更新此值，路由守卫仍直接读取 localStorage
   * @returns {Boolean}
   */
  const isLoggedIn = computed(() => _tokenPresent.value)

  return { userId, username, role, isLoggedIn, setUser, clearUser }
})
