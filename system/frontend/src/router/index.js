import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/LoginPage.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/',
      component: () => import('../layout/AppLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'Dashboard',
          component: () => import('../views/DashboardPage.vue')
        },
        {
          path: 'account',
          name: 'Account',
          component: () => import('../views/AccountPage.vue')
        },
        {
          path: 'category',
          name: 'Category',
          component: () => import('../views/CategoryPage.vue')
        },
        {
          path: 'transaction',
          name: 'Transaction',
          component: () => import('../views/TransactionListPage.vue')
        },
        {
          path: 'budget',
          name: 'Budget',
          component: () => import('../views/BudgetPage.vue')
        },
        {
          path: 'recurring-bill',
          name: 'RecurringBill',
          component: () => import('../views/RecurringBillPage.vue')
        },
        {
          path: 'transfer',
          name: 'Transfer',
          component: () => import('../views/TransferPage.vue')
        },
        {
          path: 'analytics',
          name: 'Analytics',
          component: () => import('../views/AnalyticsPage.vue')
        },
        {
          path: 'import',
          name: 'Import',
          component: () => import('../views/ImportPage.vue')
        },
        {
          path: 'settings',
          name: 'Settings',
          component: () => import('../views/UserSettingsPage.vue')
        }
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
