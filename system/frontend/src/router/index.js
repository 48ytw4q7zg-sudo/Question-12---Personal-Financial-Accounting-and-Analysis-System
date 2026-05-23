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
import { createRouter, createWebHistory } from 'vue-router' // 导入路由核心函数
import { ElMessage } from 'element-plus'                    // 导入消息提示
import { ROLE_ADMIN } from '../constants/role'              // 导入管理员角色常量
import { useUserStore } from '../stores/user'               // 导入用户状态store
import { isTokenExpired } from '../utils/jwt'               // 导入JWT工具函数（消除router和stores中的重复解码逻辑）

const router = createRouter({                               // 创建路由实例
  history: createWebHistory(),                              // HTML5历史模式
  routes: [
    {
      // 登录页：独立路由，不嵌套 AppLayout，无需登录
      path: '/login',                                       // 登录页路径
      name: 'Login',                                        // 路由名称
      component: () => import('../views/LoginPage.vue'),    // 懒加载登录页
      meta: { requiresAuth: false, title: '登录' }          // 不需登录
    },
    {
      // 主布局路由：所有业务页面的父路由，嵌套在 AppLayout（侧栏+顶栏+内容区）
      path: '/',                                            // 根路径
      component: () => import('../layout/AppLayout.vue'),   // 懒加载布局组件
      meta: { requiresAuth: true },                         // 标记需要登录才能访问
      children: [
        // P0 核心功能
        { path: '', name: 'Dashboard', component: () => import('../views/DashboardPage.vue'), meta: { title: '首页概览' } },          // 首页
        { path: 'account', name: 'Account', component: () => import('../views/AccountPage.vue'), meta: { title: '账户管理' } },        // 账户管理
        { path: 'category', name: 'Category', component: () => import('../views/CategoryPage.vue'), meta: { title: '分类管理' } },      // 分类管理
        { path: 'transaction', name: 'Transaction', component: () => import('../views/TransactionListPage.vue'), meta: { title: '收支记录' } }, // 收支记录
        // P1 进阶功能
        { path: 'budget', name: 'Budget', component: () => import('../views/BudgetPage.vue'), meta: { title: '预算管理' } },            // 预算管理
        { path: 'recurring-bill', name: 'RecurringBill', component: () => import('../views/RecurringBillPage.vue'), meta: { title: '周期账单' } }, // 周期账单
        { path: 'transfer', name: 'Transfer', component: () => import('../views/TransferPage.vue'), meta: { title: '转账' } },          // 转账
        // P2 扩展功能
        { path: 'analytics', name: 'Analytics', component: () => import('../views/AnalyticsPage.vue'), meta: { title: '统计分析' } },    // 统计分析
        { path: 'import', name: 'Import', component: () => import('../views/ImportPage.vue'), meta: { title: '数据导入' } },            // 数据导入
        // P1 个人设置
        { path: 'settings', name: 'Settings', component: () => import('../views/UserSettingsPage.vue'), meta: { title: '个人设置' } },  // 个人设置
        // 管理员功能（评分标准要求 ≥2 类用户角色 · 仅管理员可访问）
        { path: 'admin', name: 'Admin', component: () => import('../views/AdminPage.vue'), meta: { title: '用户管理', requiresAdmin: true } } // 管理员页面
      ]
    },
    // 404 路由：未匹配路径重定向到首页
    { path: '/:pathMatch(.*)*', redirect: '/' }             // 未匹配路径重定向
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
  const token = localStorage.getItem('token')               // 读取本地token
  let tokenExpired = false                                  // token过期标志

  // JWT 过期预检：使用 utils/jwt.js 的 isTokenExpired() 检查 token 是否过期
  // 消除 router/index.js 和 stores/user.js 中的重复解码逻辑
  if (token) {
    tokenExpired = isTokenExpired(token)  // 调用公共工具函数检查过期（含异常兜底）
  }

  // 检查当前路由或其任意父路由是否要求登录（修复子路由 meta 不继承问题）
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth) // 检查是否需登录
  if (requiresAuth && (!token || tokenExpired)) {
    // 未登录或 token 过期 → 清除 localStorage + 跳登录页
    if (tokenExpired) {
      localStorage.removeItem('token')                      // 清除过期token
      ElMessage.warning('登录已过期，请重新登录')             // 提示过期
    }
    next({ path: '/login', query: { redirect: to.fullPath } }) // 跳转登录页带回调
  } else if (to.path === '/login' && token && !tokenExpired) {
    // 已登录且 token 有效，还访问登录页 → 跳首页
    next('/')                                               // 重定向到首页
  } else if (to.matched.some(record => record.meta.requiresAdmin)) {
    // 需要管理员权限 → 从 Pinia store 读取 role
    const userStore = useUserStore()                        // 获取用户store
    if (userStore.role !== ROLE_ADMIN) {                    // 非管理员
      ElMessage.warning('无权限访问该页面')                  // 无权限提示
      next('/')                                             // 重定向到首页
      return
    }
    next()                                                  // 管理员放行
  } else {
    next()                                                  // 其他情况放行
  }
})

/**
 * 全局后置钩子：根据路由 meta.title 动态设置浏览器标签页标题
 */
router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 个人财务记账` : '个人财务记账与分析系统' // 动态设置页面标题
})

// Chunk 加载失败处理 — 网络异常时动态 import 可能失败，提示用户刷新页面
router.onError((error) => {
  if (error.message.includes('Failed to fetch dynamically imported module')) { // 检测chunk加载失败
    ElMessage.error('页面加载失败，请刷新浏览器重试')        // 提示刷新
  }
})

export default router                                       // 导出路由实例