import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/LoginPage.vue'),
      meta: { requiresAuth: false, title: '登录' }
    },
    {
      path: '/',
      component: () => import('../layout/AppLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', name: 'Dashboard', component: () => import('../views/DashboardPage.vue'), meta: { title: '首页概览' } },
        { path: 'account', name: 'Account', component: () => import('../views/AccountPage.vue'), meta: { title: '账户管理' } },
        { path: 'category', name: 'Category', component: () => import('../views/CategoryPage.vue'), meta: { title: '分类管理' } },
        { path: 'transaction', name: 'Transaction', component: () => import('../views/TransactionListPage.vue'), meta: { title: '收支记录' } },
        { path: 'budget', name: 'Budget', component: () => import('../views/BudgetPage.vue'), meta: { title: '预算管理' } },
        { path: 'recurring-bill', name: 'RecurringBill', component: () => import('../views/RecurringBillPage.vue'), meta: { title: '周期账单' } },
        { path: 'transfer', name: 'Transfer', component: () => import('../views/TransferPage.vue'), meta: { title: '转账' } },
        { path: 'analytics', name: 'Analytics', component: () => import('../views/AnalyticsPage.vue'), meta: { title: '统计分析' } },
        { path: 'import', name: 'Import', component: () => import('../views/ImportPage.vue'), meta: { title: '数据导入' } },
        { path: 'settings', name: 'Settings', component: () => import('../views/UserSettingsPage.vue'), meta: { title: '个人设置' } }
      ]
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
