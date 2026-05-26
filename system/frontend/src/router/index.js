// ╔══════════════════════════════════════════════════════════════════════╗
// ║  📋 答辩文件 ⑦/⑦ — 核心代码讲解 → 路由守卫：前端鉴权拦截                 ║
// ║                                                                      ║
// ║  【文件整体实现什么】                                                    ║
// ║  router/index.js — Vue Router 路由配置，放在 router/ 目录                    ║
// ║  路由表（第 73-128 行）：/login + /（AppLayout 父路由 + 11 个子路由）           ║
// ║  路由守卫（第 151-213 行）：router.beforeEach 在每次页面切换前执行 6 步检查          ║
// ║  afterEach（第 231-237 行）：设置浏览器标签页标题                             ║
// ║  onError（第 243-249 行）：JS chunk 加载失败时提示用户刷新                        ║
// ║                                                                      ║
// ║  【答辩要讲什么】                                                        ║
// ║  重点讲 router.beforeEach 路由守卫（第 151-213 行）——6 步鉴权流程                  ║
// ║  在第 151 行之前先花 10 秒介绍路由表结构（/login 独立，/ 是 AppLayout 父路由）       ║
// ║                                                                      ║
// ║  【6 步讲解流程 + 对应行号】                                               ║
// ║  第1步 第153行：读localStorage token                                   ║
// ║  第2步 第156行：isTokenExpired()预检过期（为什么不等后端401？更快）              ║
// ║  第3步 第170行：需登录没token→跳/login带redirect（为什么to.matched.some？）      ║
// ║  第4步 第210行：已登录访问/login→跳首页（防重复登录）                          ║
// ║  第5步 第215行：管理员检查（为什么用Pinia而非重解析JWT？单一数据源）               ║
// ║  第6步 第229行：正常放行                                                 ║
// ║                                                                      ║
// ║  🏁 答辩结束时串讲完整全栈链路：                      ║
// ║  前端4个(LoginPage→api/user.js→request.js→axios) → 网络(Vite Proxy) →        ║
// ║  后端6个(Tomcat→CorsFilter→Dispatcher→LoginInterceptor→Controller→Service) →║
// ║  数据层(MySQL) → 前端3个(response.js→LoginPage→userStore+router)              ║
// ╚══════════════════════════════════════════════════════════════════════╝
//
// ▶ 答辩结束。全部 7 个文件已讲完。
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
// ========== 全局前置路由守卫（router.beforeEach）==========
// Vue Router 的核心安全机制——每次路由切换前（用户点击链接 / router.push() / 浏览器前进后退）
// 都会执行此函数。返回 false 或调用 next(false) 取消导航；调用 next() 或 next('/path') 放行或重定向。
// 此处实现 6 步检查链：token 读取 → 过期预检 → 鉴权 → 防重复登录 → 管理员权限 → 放行。
router.beforeEach(async (to, from, next) => {
  // === 第 1 步：从 localStorage 读取 JWT token ===
  // 为什么用 localStorage 而非 cookie 存储和读取 token？
  // ① JWT 是无状态验证——不需要 cookie 的自动携带机制，前端显式控制发送时机更安全。
  // ② CSRF 防御：cookie 会自动附加到同域请求中，恶意站点可利用浏览器的"自动带 cookie"行为
  // 发起跨站请求伪造攻击（攻击者看不到 cookie 内容，但浏览器会自动发送）。
  // localStorage 中的 token 只有 JS 代码显式读取并放入 Authorization 头才会发送——
  // 攻击者无法通过简单的 <form> 或 <img> 标签触发 JWT 携带。
  // ③ API 简洁：getItem/setItem/removeItem 操作比 document.cookie 的字符串解析方便得多。
  // 键名 'token' 与 stores/user.js 的 setUser() 写入 + api/request.js 请求拦截器读取的键名完全一致。
  const token = localStorage.getItem('token')               // 【做什么】从浏览器 localStorage 按 'token' 键读取 JWT /【为什么】JWT 无状态，不需要 session/cookie 的自动携带——localStorage 三种优势：① CSRF 防御（cookie 自动附加到同域请求会被 CSRF 利用，localStorage 只能由 JS 显式读取后放入 Authorization 头）② API 简洁（getItem/setItem/removeItem 比 document.cookie 的字符串操作简单）③ 刷新页面 token 仍然在（与 sessionStorage 的区别）
  let tokenExpired = false                                  // 【做什么】声明过期标志变量，默认为 false /【为什么】默认认为 token 有效——只有当 token 存在且 isTokenExpired() 返回 true 时才置 true；初始 false 的语义是"无罪推定"而非"有罪推定"，避免误拦截有效 token

  // === 第 2 步：JWT 过期预检（路由层提前拦截，不等后端 401） ===
  // isTokenExpired() 在 utils/jwt.js 中实现：Base64 解码 token 的 payload 部分 → 读取 exp 字段
  // （UNIX 秒级时间戳）→ 与 Date.now() / 1000 比较。纯本地计算，耗时 < 1ms，无需网络请求。
  // 为什么在路由层预检而非等后端返回 401 再跳？
  // → 体验差异巨大：如果等后端 401 → 用户点击"账户管理" → 路由放行 → 页面渲染 → mounted 发 API →
  // → 后端校验 token → 返回 401 → 响应拦截器跳 /login → 用户看到页面闪烁后再跳到登录页。
  // 路由层预检 → 用户点击"账户管理" → 守卫发现过期 → 直接跳 /login（原页面根本不渲染）→ 零闪烁。
  // 这是"前端乐观鉴权"优于"后端悲观鉴权"的典型场景——在用户感知层解决问题比修复结果更重要。
  if (token) {
    tokenExpired = isTokenExpired(token)  // 【做什么】调用 isTokenExpired() 解码 JWT payload.exp 字段并与当前时间比较 /【为什么】为什么在路由层预检而非等后端返回 401？等后端 401 意味着用户先看到目标页面（已渲染、已发 API），再被踢到 /login——页面闪烁体验极差。路由层预检在页面切换前就拦截——零渲染、零 API 调用、零用户感知延迟。isTokenExpired 是纯 CPU 计算（Base64 解码 + 时间戳比较），< 1ms，不阻塞导航
  }

  // === 第 3 步：鉴权检查——需要登录但无有效 token → 跳 /login ===
  // to.matched 是当前目标路由到根路由的完整匹配链数组。例如访问 /account：
  //   to.matched = [ { path:'/', meta:{ requiresAuth:true } }, { path:'account', meta:{ title:'账户管理' } } ]
  // 为什么用 to.matched.some() 遍历而非直接读 to.meta.requiresAuth？
  // → Vue Router 的 meta 字段不会自动从父路由继承到子路由。本项目 requiresAuth:true 定义在父路由
  // AppLayout（/）上，而 11 个子路由（/account、/budget 等）的 meta 中只有 title 没有 requiresAuth。
  // 如果直接读 to.meta.requiresAuth → 永远得到 undefined → 路由守卫认为"不需要登录" → 未登录用户
  // 进入页面 → 组件 mounted 发 API → 401 → 页面内容已加载出来（浪费带宽）→ 然后跳 /login（体验差）。
  // some() 遍历整个 matched 链：只要任一父路由标了 requiresAuth=true 即判定需登录——这是
  // Vue Router 官方推荐的嵌套路由 meta 继承模式（类比 JS 原型链查找）。
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth) // 【做什么】遍历 to.matched 数组（目标路由→根路由的完整链路），检查是否任一祖先路由标记了 requiresAuth /【为什么】为什么用 to.matched.some() 而非直接读 to.meta.requiresAuth？Vue Router 的 meta 字段不会自动从父路由继承到子路由——本项目 requiresAuth:true 定义在 AppLayout（/，父路由）上，11 个子路由（/account、/budget 等）的 meta 中只有 title 没有 requiresAuth。如果直接读 to.meta.requiresAuth——子路由返回 undefined——守卫认为不需要登录——放行——页面渲染后 mounted 发 API——401——浪费带宽 + 页面闪烁。some() 遍历完整 matched 链，any ancestor 标了即返回 true，这是 Vue Router 官方推荐的嵌套路由 meta 继承模式，类比 JS 原型链查找
  if (requiresAuth && (!token || tokenExpired)) {
    // 需要登录 +（无 token 或 token 已过期）→ 拦截并跳转登录页
    if (tokenExpired) {
      // 过期 token 必须清除：如果不清除，后续请求拦截器仍会从 localStorage 读到它并注入 Authorization 头，
      // 导致每个请求都 401 → 响应拦截器重复弹"登录过期"→ 用户看到一连串提示，体验极差。
      localStorage.removeItem('token')                      // 【做什么】删除 localStorage 中过期的 token /【为什么】不删除的话——请求拦截器读到它注入请求头——后端返回 401——响应拦截器再弹一次"登录过期"——用户被"踢两次"（路由守卫踢一次 + API 401 踢一次），体验极差
      ElMessage.warning('登录已过期，请重新登录')             // 【做什么】弹出黄色警告提示用户 token 已过期 /【为什么】用 warning（黄）而非 error（红）——过期是正常的会话安全策略不是系统故障，黄色温和提醒用户重新登录
    }
    // next({ path: '/login', query: { redirect: to.fullPath } })——为什么带 redirect 参数？
    // → 这是用户体验闭环的关键设计。用户原本想访问 /account?id=1，被拦截后 URL 变成：
    //   /login?redirect=/account?id=1
    // LoginPage 在 handleLogin 成功后：const redirect = this.$route.query.redirect || '/'
    // → router.push(redirect) → 用户感觉"只是登了个录"，自动回到刚才想去的页面——无缝衔接。
    // to.fullPath（含 query 参数）而非 to.path（只有路径）：保留完整的导航意图。
    // 如 /transaction?categoryId=5 → 登录后跳回时自动带上分类筛选条件。
    next({ path: '/login', query: { redirect: to.fullPath } }) // 【做什么】调用 next() 导航到 /login，query 中携带 redirect=to.fullPath /【为什么】① next({path, query}) 是 Vue Router 的导航重定向写法——不是调 next() 再手动 router.push，是官方 API；② to.fullPath 含完整路径+query（如 /account?id=1），LoginPage 登录成功后 router.push(redirect) 精确回到被打断的页面——用户感觉"只是登了个录"；③ next() 是导航的通行证——守卫必须调用 next() 来解析导航，不调页面永远不切换

  // === 第 4 步：已登录用户访问 /login → 跳首页（防止重复登录） ===
  // 为什么需要这一步？已登录用户不需要看到登录表单，直接送进系统更合理。场景包括：
  // ① 用户登录后在地址栏手动输入 /login  ② 点浏览器后退按钮回到登录页  ③ 从书签打开 /login。
  // 如果不拦截：已登录用户看到登录表单 → 再次提交 → 后端签发新 token → 覆盖旧 token →
  // Pinia store 状态可能与新 token 的角色信息不一致（如旧 token 对应管理员但新 token 却对应普通用户时）。
  // 更简单的原则：用户在任一时刻应该只看到一个合理的页面，登录页对已登录用户来说不合理。
  } else if (to.path === '/login' && token && !tokenExpired) {
    next('/')                                               // 【做什么】已有有效 token 却访问 /login——直接重定向到首页 / /【为什么】已登录用户不需要看到登录表单——如果不拦截，用户可能再次提交登录→后端签发新 token 覆盖旧 token→Pinia store 状态与新 token 角色不一致；next('/') 是 Vue Router 内置重定向简写，等价于 next({path:'/'})

  // === 第 5 步：管理员权限检查 ===
  // 进入条件：to.matched 链中任一路由的 meta.requiresAdmin === true（当前仅 /admin 路由）。
  } else if (to.matched.some(record => record.meta.requiresAdmin)) {
    // 从 Pinia store 读取 role 字段。为什么用 Pinia store 而非重新解析 JWT token 的 payload？
    // → ① 避免重复计算：JWT payload 是 Base64 编码 → 需要 atob + JSON.parse 才能读取 role 字段。
    // 登录时已在 stores/user.js 中解码过一次存入 store，路由守卫直接用现成的——不重复劳动。
    // ② 单一数据源（Single Source of Truth）：整个前端应用中，role 应该只有一个存储位置。
    // 如果路由守卫自己解析 JWT 而组件用 store，可能出现"store 说 role=1 但 JWT 被手动刷新说 role=2"
    // 的不一致——两套判断逻辑各自读取不同来源是典型的 bug 温床。
    // ③ 响应式同步：userStore.role 变化时 UI 自动更新，路由守卫也能读到最新值，无需手动同步。
    const userStore = useUserStore()                        // 【做什么】获取 Pinia user store 实例 /【为什么】为什么用 Pinia store 而非重新解析 JWT？三方面优势——① 避免重复计算：JWT payload 是 Base64 编码，登录时已在 stores/user.js 解码过一次存入 store，路由守卫直接读现成的 ② 单一数据源：整个应用中 role 只存一处——如果路由守卫自己解析 JWT 而组件用 store，可能出现 "store.role=1 但 JWT 显示 role=2" 的不一致 ③ 响应式同步：store.role 变化时路由守卫读到最新值，无需手动轮询
    if (userStore.role !== ROLE_ADMIN) {                    // 【做什么】用严格不等号 !== 比较当前用户角色和 ROLE_ADMIN 常量（值=1）/【为什么】严格比较防止 JS 类型转换绕过——'1' !== 1 为 true 而 '1' != 1 为 false；ROLE_ADMIN 来自 constants/role.js 的语义化常量，优于硬编码魔数 1
      ElMessage.warning('无权限访问该页面')                  // 【做什么】弹出黄色警告提示非管理员用户 /【为什么】用 warning（黄）非 error（红）——用户已成功登录，只是角色权限不足，这不是系统故障，黄色温和提示"这个页面不给你看"
      next('/')                                             // 【做什么】重定向到首页 / /【为什么】跳首页而非 /login——用户已登录，跳登录页会造成困惑"我明明登录了为什么又让登录"；让用户感知"页面不可访问"而非"我被登出了"
      return                                                // 【做什么】提前 return 退出守卫函数 /【为什么】不 return 会 fall-through 到第六步的 else { next() }——导致 next('/') 后又调用 next()——Vue Router 检测到同一导航被解析两次，抛出 NavigationDuplicated 错误
    }
    next()                                                  // 【做什么】role === ROLE_ADMIN 时调用无参 next() 放行 /【为什么】管理员权限确认通过——无参 next() 表示不做任何重定向，正常解析到 /admin 路由并渲染 AdminPage

  // === 第 6 步：其余情况正常放行 ===
  // 涵盖以下场景：① 访问不需要登录的页面（如 /login，且未登录——正常看登录表单）
  // ② 已登录 + 有效 token + 访问需登录页面（最常见场景——正常使用系统）③ 未来可能的公开页面。
  } else {
    next()                                                  // 【做什么】调用无参 next() 无条件放行 /【为什么】到达此处说明前面 5 步检查全部通过：token 有效 + 未过期 + 页面不需要特殊权限——正常导航解析到目标路由，页面正常渲染
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
