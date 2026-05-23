/**
 * 路由配置文件（router/index.js）
 *
 * 职责：定义前端路由表 + 路由守卫（鉴权拦截）+ 页面标题设置 + Chunk 加载容错
 *
 * 被 main.js 导入并注册到 Vue 应用实例（app.use(router)）
 *
 * 路由结构（父路由 AppLayout 包裹所有业务页面）：
 *   /login          → LoginPage（无需登录，独立路由，不嵌套 AppLayout）
 *   /               → AppLayout（需登录，作为父路由包裹以下 11 个子路由）
 *     ├── /         → DashboardPage   首页概览 (PRD P0)
 *     ├── /account  → AccountPage     账户管理 (PRD P0)
 *     ├── /category → CategoryPage    分类浏览 (PRD P0)
 *     ├── /transaction → TransactionListPage 收支记录 (PRD P0)
 *     ├── /budget   → BudgetPage      预算管理 (PRD P1)
 *     ├── /recurring-bill → RecurringBillPage 周期账单 (PRD P1)
 *     ├── /transfer → TransferPage    转账     (PRD P1)
 *     ├── /settings → UserSettingsPage 个人设置 (PRD P1)
 *     ├── /analytics → AnalyticsPage  统计分析 (PRD P2)
 *     ├── /import   → ImportPage      数据导入 (PRD P2)
 *     └── /admin    → AdminPage       用户管理 (管理员专属 · role=1)
 *
 * 路由守卫流程（router.beforeEach 全局前置钩子）：
 *   1. 读取 localStorage 中的 token
 *   2. 用 utils/jwt.js 的 isTokenExpired() 预检 token 是否过期
 *   3. 需要登录的页面（meta.requiresAuth=true）→ 无 token 或过期时跳 /login
 *   4. 已登录用户访问 /login → 自动跳回首页 /
 *   5. 需要管理员权限的页面（meta.requiresAdmin=true）→ 非 ROLE_ADMIN 时提示无权限并跳首页
 *   6. 后置钩子 afterEach → 动态设置浏览器标签页标题
 *
 * 关联文件：
 *   - stores/user.js：路由守卫通过 useUserStore().role 检查管理员权限
 *   - utils/jwt.js：路由守卫通过 isTokenExpired() 预检 token 过期
 *   - constants/role.js：导入 ROLE_ADMIN 常量进行管理员角色判断
 *   - layout/AppLayout.vue：根路径的父路由组件（侧栏+顶栏+内容区）
 */
import { createRouter, createWebHistory } from 'vue-router' // 导入 Vue Router 核心函数：createRouter（创建路由实例）、createWebHistory（HTML5 History 模式）
import { ElMessage } from 'element-plus'                    // 导入 Element Plus 消息提示组件（路由守卫中提示过期/无权限）
import { ROLE_ADMIN } from '../constants/role'              // 导入管理员角色常量（值为 1，→ constants/role.js）
import { useUserStore } from '../stores/user'               // 导入用户 Pinia store（路由守卫中读取 role 字段） → stores/user.js
import { isTokenExpired } from '../utils/jwt'               // 导入 JWT 过期检测工具函数 → utils/jwt.js（消除 router 和 stores 中的重复 JWT 解码逻辑）

// ========== 创建路由实例 ==========
const router = createRouter({
  // HTML5 History 模式：URL 不带 # 号，如 /account 而非 /#/account
  // 后端需配合将所有未匹配路径 fallback 到 index.html（Nginx try_files / SpringBoot addResourceHandler）
  history: createWebHistory(),
  routes: [
    // ========== 登录页（独立路由，不嵌套 AppLayout，无需登录校验） ==========
    {
      path: '/login',                                       // 登录页路径（浏览器地址栏显示 /login）
      name: 'Login',                                        // 路由名称（编程式导航使用：router.push({ name: 'Login' })）
      component: () => import('../views/LoginPage.vue'),    // 路由懒加载：仅在首次访问 /login 时才加载 LoginPage.vue 的 JS chunk（→ views/LoginPage.vue）
      meta: { requiresAuth: false, title: '登录' }          // meta.requiresAuth=false 表示无需登录即可访问；meta.title 用于设置浏览器标签页标题
    },

    // ========== 主布局路由（所有业务页面的父路由，需登录） ==========
    {
      path: '/',                                            // 根路径（浏览器地址栏显示 /）
      component: () => import('../layout/AppLayout.vue'),   // 懒加载布局组件（→ layout/AppLayout.vue，提供顶栏+侧栏+内容区框架）
      meta: { requiresAuth: true },                         // meta.requiresAuth=true 标记所有子路由需要登录后才能访问
      children: [
        // --- P0 核心功能（PRD 最高优先级） ---
        // 首页概览：仪表盘显示收支汇总、账户余额、近期交易
        { path: '', name: 'Dashboard', component: () => import('../views/DashboardPage.vue'), meta: { title: '首页概览' } },          // → views/DashboardPage.vue
        // 账户管理：多账户列表 + 新增 + 编辑 + 软删除（PRD P0）
        { path: 'account', name: 'Account', component: () => import('../views/AccountPage.vue'), meta: { title: '账户管理' } },        // → views/AccountPage.vue
        // 分类管理：收支分类列表（种子数据只读，PRD P0）
        { path: 'category', name: 'Category', component: () => import('../views/CategoryPage.vue'), meta: { title: '分类管理' } },      // → views/CategoryPage.vue
        // 收支记录：列表分页 + 记一笔 + 改 + 删除 + 多条件筛选（PRD P0 + P1）
        { path: 'transaction', name: 'Transaction', component: () => import('../views/TransactionListPage.vue'), meta: { title: '收支记录' } }, // → views/TransactionListPage.vue

        // --- P1 进阶功能 ---
        // 预算管理：按月按分类设置预算 + 超支标记提醒（PRD P1）
        { path: 'budget', name: 'Budget', component: () => import('../views/BudgetPage.vue'), meta: { title: '预算管理' } },            // → views/BudgetPage.vue
        // 周期账单：定期收支提醒管理（PRD P1）
        { path: 'recurring-bill', name: 'RecurringBill', component: () => import('../views/RecurringBillPage.vue'), meta: { title: '周期账单' } }, // → views/RecurringBillPage.vue
        // 转账：两个账户间资金转移（PRD P1）
        { path: 'transfer', name: 'Transfer', component: () => import('../views/TransferPage.vue'), meta: { title: '转账' } },          // → views/TransferPage.vue

        // --- P2 扩展功能 ---
        // 统计分析：ECharts 图表（收支趋势图+分类饼图+预算对比）
        { path: 'analytics', name: 'Analytics', component: () => import('../views/AnalyticsPage.vue'), meta: { title: '统计分析' } },    // → views/AnalyticsPage.vue
        // 数据导入：CSV 文件导入交易记录（PRD P2）
        { path: 'import', name: 'Import', component: () => import('../views/ImportPage.vue'), meta: { title: '数据导入' } },            // → views/ImportPage.vue

        // --- P1 个人设置 ---
        // 个人设置：修改用户名 + 修改密码（PRD P1）
        { path: 'settings', name: 'Settings', component: () => import('../views/UserSettingsPage.vue'), meta: { title: '个人设置' } },  // → views/UserSettingsPage.vue

        // --- 管理员功能（评分标准要求 >=2 类用户角色：普通用户 + 管理员） ---
        // 仅管理员可访问（meta.requiresAdmin=true）→ 路由守卫中检查 userStore.role === ROLE_ADMIN
        { path: 'admin', name: 'Admin', component: () => import('../views/AdminPage.vue'), meta: { title: '用户管理', requiresAdmin: true } } // → views/AdminPage.vue
      ]
    },

    // ========== 404 路由：所有未匹配的路径重定向到首页 ==========
    // 如 /foo/bar、/unknown 等未定义路径均自动跳转到根路径 /
    { path: '/:pathMatch(.*)*', redirect: '/' }             // Vue Router 4 通配语法（替代 Vue Router 3 的 path: '*'）
  ]
})

/**
 * ========== 全局前置路由守卫（router.beforeEach） ==========
 *
 * 每次路由切换前执行，按以下优先级依次检查：
 *   1. Token 过期预检 → 解码 JWT exp 字段，过期则立即清除 token 并跳登录页（避免延迟鉴权体验差）
 *   2. 鉴权检查 → 需登录但无 token → 跳 /login 并带上 redirect 参数（登录后可跳回原页面）
 *   3. 已登录拦截 → 已登录访问 /login → 自动跳转到首页 /
 *   4. 管理员权限检查 → 需 ROLE_ADMIN 但角色不符 → 提示无权限并跳首页
 *   5. 其他情况正常放行
 *
 * 关联文件：
 *   - utils/jwt.js：isTokenExpired() 解码 JWT payload.exp 字段判断过期（含 try-catch 异常兜底）
 *   - stores/user.js：useUserStore().role 读取当前登录用户的角色（login 时从后端返回的角色赋值）
 *   - constants/role.js：ROLE_ADMIN 常量（值 = 1）
 */
router.beforeEach(async (to, from, next) => {
  // --- 第一步：从 localStorage 读取 JWT token ---
  const token = localStorage.getItem('token')               // 键名 'token' 对齐 CLAUDE.md §三·六 JWT 存储规范
  let tokenExpired = false                                  // token 过期标志（false=有效，true=已过期）

  // --- 第二步：JWT 过期预检 ---
  // 调用 utils/jwt.js 的 isTokenExpired() 检查 token 的 exp 字段是否已过期
  // 比等后端返回 401 再跳登录页更快（在路由切换时即拦截，用户体验更好）
  if (token) {
    tokenExpired = isTokenExpired(token)  // 返回 true=已过期/解码失败，false=未过期 → utils/jwt.js
  }

  // --- 第三步：鉴权检查（to.matched 遍历当前路由及其所有父路由的 meta，修复子路由 meta 不继承问题） ---
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth) // some() 找到任一父路由 meta.requiresAuth=true 即需登录
  if (requiresAuth && (!token || tokenExpired)) {
    // 未登录或 token 已过期 → 清除过期 token，跳转登录页（携带 redirect 参数供登录成功后回跳）
    if (tokenExpired) {
      localStorage.removeItem('token')                      // 清除过期 token，防止后续请求带无效 token
      ElMessage.warning('登录已过期，请重新登录')             // Element Plus 弹出警告提示（黄色）
    }
    // 跳登录页：?redirect= 参数保存当前目标路径，LoginPage 登录成功后读取并跳回
    next({ path: '/login', query: { redirect: to.fullPath } }) // to.fullPath 含路径+query 参数，如 /account?id=1

  // --- 第四步：已登录用户访问 /login → 自动跳转首页（防止重复登录） ---
  } else if (to.path === '/login' && token && !tokenExpired) {
    next('/')                                               // 重定向到首页路由 /（→ DashboardPage.vue）

  // --- 第五步：管理员权限检查 ---
  } else if (to.matched.some(record => record.meta.requiresAdmin)) {
    // 进入此分支 = 当前路由或其父路由标记了 meta.requiresAdmin=true
    const userStore = useUserStore()                        // 获取 Pinia user store 实例（→ stores/user.js）
    if (userStore.role !== ROLE_ADMIN) {                    // 非管理员角色（ROLE_ADMIN 常量为 1，→ constants/role.js）
      ElMessage.warning('无权限访问该页面')                  // 弹出"无权限"黄色警告
      next('/')                                             // 重定向到首页，不跳登录页（用户已登录但无管理员权限）
      return                                                // 提前返回，避免继续执行
    }
    next()                                                  // role === ROLE_ADMIN，管理员放行

  // --- 第六步：其他情况正常放行 ---
  } else {
    next()                                                  // 不需登录 or 已登录访问需登录页面，正常放行
  }
})

/**
 * ========== 全局后置钩子（router.afterEach）==========
 *
 * 路由切换完成后执行：根据路由 meta.title 动态设置浏览器标签页标题
 * 标题格式：{页面名} - 个人财务记账 （如 "账户管理 - 个人财务记账"）
 *         无 title 时显示默认标题 "个人财务记账与分析系统"
 */
router.afterEach((to) => {
  document.title = to.meta.title                            // 三元表达式：有 title 时拼接项目名
    ? `${to.meta.title} - 个人财务记账`                      // 示例：'账户管理 - 个人财务记账'
    : '个人财务记账与分析系统'                               // 默认标题（登录页或无 title 的路由）
})

/**
 * ========== Chunk 加载失败处理（router.onError）==========
 *
 * 场景：用户首次访问时 Vite 构建的 JS chunk 文件（路由懒加载产物）可能因网络异常或文件名 hash 不匹配而加载失败
 * 处理：检测错误信息是否包含 "Failed to fetch dynamically imported module"，是则提示用户刷新浏览器
 */
router.onError((error) => {
  // 错误信息特征匹配：Vue Router 4 懒加载 chunk 失败的标准错误消息
  if (error.message.includes('Failed to fetch dynamically imported module')) {
    ElMessage.error('页面加载失败，请刷新浏览器重试')        // 弹出红色错误提示
  }
})

// 导出路由实例，供 main.js 导入后通过 app.use(router) 注册到 Vue 应用
export default router
