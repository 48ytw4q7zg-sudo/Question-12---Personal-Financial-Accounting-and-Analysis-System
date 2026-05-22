/**
 * 路由配置文件
 * 职责：定义前端路由表 + 路由守卫（鉴权拦截）+ 页面标题设置
 *
 * 路由结构：
 *   /login          → LoginPage（无需登录）
 *   /               → AppLayout（需登录，作为父路由包裹以下子路由）
 *     ├── /         → DashboardPage   首页概览   (PRD P0)
 *     ├── /account  → AccountPage     账户管理   (PRD P0)
 *     ├── /category → CategoryPage    分类浏览   (PRD P0)
 *     ├── /transaction → TransactionListPage 收支记录 (PRD P0)
 *     ├── /budget   → BudgetPage      预算管理   (PRD P1)
 *     ├── /recurring-bill → RecurringBillPage 周期账单 (PRD P1)
 *     ├── /transfer → TransferPage    转账       (PRD P1)
 *     ├── /analytics → AnalyticsPage  统计分析   (PRD P2)
 *     ├── /import   → ImportPage      数据导入   (PRD P2)
 *     ├── /settings → UserSettingsPage 个人设置  (PRD P1)
 *     └── /admin    → AdminPage       用户管理   (管理员 · role=1)
 */
import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ROLE_ADMIN } from '../constants/role'
import { useUserStore } from '../stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      // 登录页：独立路由，不嵌套 AppLayout，无需登录
      path: '/login',
      name: 'Login',
      component: () => import('../views/LoginPage.vue'),
      meta: { requiresAuth: false, title: '登录' }
    },
    {
      // 主布局路由：所有业务页面的父路由，嵌套在 AppLayout（侧栏+顶栏+内容区）
      path: '/',
      component: () => import('../layout/AppLayout.vue'),
      meta: { requiresAuth: true },  // 标记需要登录才能访问
      children: [
        // P0 核心功能
        { path: '', name: 'Dashboard', component: () => import('../views/DashboardPage.vue'), meta: { title: '首页概览' } },
        { path: 'account', name: 'Account', component: () => import('../views/AccountPage.vue'), meta: { title: '账户管理' } },
        { path: 'category', name: 'Category', component: () => import('../views/CategoryPage.vue'), meta: { title: '分类管理' } },
        { path: 'transaction', name: 'Transaction', component: () => import('../views/TransactionListPage.vue'), meta: { title: '收支记录' } },
        // P1 进阶功能
        { path: 'budget', name: 'Budget', component: () => import('../views/BudgetPage.vue'), meta: { title: '预算管理' } },
        { path: 'recurring-bill', name: 'RecurringBill', component: () => import('../views/RecurringBillPage.vue'), meta: { title: '周期账单' } },
        { path: 'transfer', name: 'Transfer', component: () => import('../views/TransferPage.vue'), meta: { title: '转账' } },
        // P2 扩展功能
        { path: 'analytics', name: 'Analytics', component: () => import('../views/AnalyticsPage.vue'), meta: { title: '统计分析' } },
        { path: 'import', name: 'Import', component: () => import('../views/ImportPage.vue'), meta: { title: '数据导入' } },
        // P1 个人设置
        { path: 'settings', name: 'Settings', component: () => import('../views/UserSettingsPage.vue'), meta: { title: '个人设置' } },
        // 管理员功能（评分标准要求 ≥2 类用户角色 · 仅管理员可访问）
        { path: 'admin', name: 'Admin', component: () => import('../views/AdminPage.vue'), meta: { title: '用户管理', requiresAdmin: true } }
      ]
    },
    // 404 路由：未匹配路径重定向到首页
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

/**
 * 全局前置路由守卫
 * 逻辑：
 *   1. 需要登录的页面（meta.requiresAuth=true）→ 无 token 或 token 已过期时跳转 /login
 *   2. 已登录用户访问 /login → 自动跳转到首页 /
 *   3. 需要管理员权限的页面（meta.requiresAdmin=true）→ 非 ROLE_ADMIN 时跳转首页
 *   4. 其他情况直接放行
 *
 * 用户信息恢复：Pinia store 初始化时从 JWT payload 解码恢复（防 localStorage 独立篡改 role）
 * Token 过期预检：解码 JWT exp 字段，过期则立即清除 token 并跳登录页（避免延迟鉴权体验）
 */
router.beforeEach(async (to, from, next) => {
  const token = localStorage.getItem('token')
  let tokenExpired = false

  // JWT 过期预检：解码 payload 的 exp 字段，提前发现过期（避免先进入页面再被 401 跳走）
  if (token) {
    try {
      const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
      const decoded = decodeURIComponent(atob(base64).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join(''))
      const payload = JSON.parse(decoded)
      if (payload.exp && payload.exp * 1000 < Date.now()) {
        tokenExpired = true
      }
    } catch {
      // token 格式异常或被篡改 → 视为过期，记录日志便于安全排查
      console.warn('JWT token 格式异常或被篡改，视为过期')
      tokenExpired = true
    }
  }

  // 检查当前路由或其任意父路由是否要求登录（修复子路由 meta 不继承问题）
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  if (requiresAuth && (!token || tokenExpired)) {
    // 未登录或 token 过期 → 清除 localStorage + 跳登录页
    if (tokenExpired) {
      localStorage.removeItem('token')
      ElMessage.warning('登录已过期，请重新登录')
    }
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (to.path === '/login' && token && !tokenExpired) {
    // 已登录且 token 有效，还访问登录页 → 跳首页
    next('/')
  } else if (to.matched.some(record => record.meta.requiresAdmin)) {
    // 需要管理员权限 → 从 Pinia store 读取 role
    const userStore = useUserStore()
    if (userStore.role !== ROLE_ADMIN) {
      ElMessage.warning('无权限访问该页面')
      next('/')
      return
    }
    next()
  } else {
    next()
  }
})

/**
 * 全局后置钩子：根据路由 meta.title 动态设置浏览器标签页标题
 */
router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 个人财务记账` : '个人财务记账与分析系统'
})

// Chunk 加载失败处理 — 网络异常时动态 import 可能失败，提示用户刷新页面
router.onError((error) => {
  if (error.message.includes('Failed to fetch dynamically imported module')) {
    ElMessage.error('页面加载失败，请刷新浏览器重试')
  }
})

export default router
