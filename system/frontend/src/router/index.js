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
 *     └── /settings → UserSettingsPage 个人设置  (PRD P1)
 */
import { createRouter, createWebHistory } from 'vue-router'

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
        { path: 'settings', name: 'Settings', component: () => import('../views/UserSettingsPage.vue'), meta: { title: '个人设置' } }
      ]
    }
  ]
})

/**
 * 全局前置路由守卫
 * 逻辑：
 *   1. 需要登录的页面（meta.requiresAuth=true）→ 无 token 时跳转 /login，并携带 redirect 参数用于登录后回跳
 *   2. 已登录用户访问 /login → 自动跳转到首页 /
 *   3. 其他情况直接放行
 */
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth && !token) {
    // 未登录访问需鉴权页面 → 跳登录页，记住原目标路径
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (to.path === '/login' && token) {
    // 已登录还访问登录页 → 跳首页
    next('/')
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

export default router
