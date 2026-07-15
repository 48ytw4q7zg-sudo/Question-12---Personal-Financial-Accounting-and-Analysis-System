/**
 * 前端应用入口（main.js）—— 整个 Vue 3 应用的启动文件
 *
 * 技术栈版本（对齐 CLAUDE.md §一·一）：
 *   Vue 3.5.34 + Vue Router 5.0.6 + Pinia 3.0.4 + Element Plus 2.13.7 + Vite 8.0.0
 *
 * 执行流程（按行顺序）：
 *   1. 导入依赖（Vue、Element Plus、图标、根组件、路由、全局样式、Pinia）
 *   2. 创建 Vue 3 应用实例（createApp）
 *   3. 注册 Element Plus UI 组件库（全局注册，所有 .vue 可直接使用 el-button/el-table 等）
 *   4. 注册 Pinia 状态管理（必须在 Router 之前，因为路由守卫使用了 Pinia store）
 *   5. 注册 Vue Router（路由表 + 守卫，→ router/index.js）
 *   6. 按需注册 Element Plus 图标组件（仅 16 个项目中实际使用的图标，减小打包体积）
 *   7. 挂载应用到 #app DOM 节点（index.html 中的 <div id="app"></div>）
 *
 * 插件注册顺序（重要！不可调换）：
 *   ElementPlus → createPinia() → router
 *   原因：router.beforeEach 守卫内部调用 useUserStore()（Pinia store），因此 Pinia 必须在 Router 之前注册
 *
 * 关联文件：
 *   - index.html：根 HTML 文件，包含 <div id="app"></div> 挂载点
 *   - App.vue：根组件（仅含 <router-view /> + 全局 reset CSS）
 *   - router/index.js：路由配置（路由表 + beforeEach 守卫 + afterEach 标题设置）
 *   - stores/user.js：用户 Pinia store（路由守卫中通过 useUserStore() 读取角色）
 *   - styles/color-vars.css：全局 CSS 自定义属性（--color-income 绿色 / --color-expense 红色 等）
 */
import { createApp } from 'vue'                              // Vue 3 核心 API：createApp 创建应用实例（替代 Vue 2 的 new Vue()）
import ElementPlus from 'element-plus'                      // Element Plus UI 组件库整体导入（全局注册 el-button/el-table/el-form 等组件）
import 'element-plus/dist/index.css'                         // Element Plus 默认 CSS 样式文件（包含所有组件的默认主题样式，暗黑模式变量等）
// ========== Element Plus 图标按需导入（仅 16 个项目中实际使用的图标，Vite tree-shaking 自动移除未使用图标） ==========
import {
  Plus,           // 加号图标   — 新增按钮通用（DashboardPage / AccountPage 等）
  Wallet,         // 钱包图标   — SidebarMenu 账户菜单项
  Menu,           // 菜单图标   — SidebarMenu 菜单切换
  List,           // 列表图标   — SidebarMenu 交易记录菜单项
  Money,          // 金额图标   — SidebarMenu 预算菜单项
  Calendar,       // 日历图标   — SidebarMenu 周期账单菜单项
  Sort,           // 排序图标   — SidebarMenu 分类菜单项
  TrendCharts,    // 趋势图图标 — SidebarMenu 统计分析菜单项
  Upload,         // 上传图标   — SidebarMenu 数据导入菜单项
  Setting,        // 设置图标   — SidebarMenu 个人设置菜单项
  SwitchButton,   // 切换图标   — AppLayout 退出登录按钮
  HomeFilled,     // 首页图标   — SidebarMenu 首页菜单项
  UserFilled,     // 用户图标   — SidebarMenu 个人设置菜单项
  Expand,         // 展开图标   — AppLayout 展开侧栏按钮
  Fold            // 折叠图标   — AppLayout 折叠侧栏按钮
} from '@element-plus/icons-vue'                             // Element Plus 图标库（@element-plus/icons-vue 独立包，按需导入以减小打包体积）
import App from './App.vue'                                  // 根组件：仅含 <router-view /> + 全局 CSS reset → App.vue
import router from './router'                                // Vue Router 实例：含路由表 + beforeEach 守卫 → router/index.js
// ========== 全局颜色 CSS 变量（必须在 App 挂载前导入，确保 :root 变量对所有子组件生效） ==========
// 定义的变量包括：
//   --color-income: #67c23a（收入绿色，用于金额显示 + 图表）
//   --color-expense: #f56c6c（支出红色，用于金额显示 + 图表）
//   --color-title / --color-subtitle（标题/副标题颜色）
// 各 .vue 文件中通过 var(--color-income) / var(--color-expense) 引用
import './styles/color-vars.css'                             // → styles/color-vars.css
import { createPinia } from 'pinia'                          // Pinia 3.x 状态管理核心 API：createPinia 创建 store 容器（替代 Vuex）

// ========== 步骤 1：创建 Vue 3 应用实例 ==========
// createApp(App)：以 App.vue 为根组件构建整个 Vue 组件树
const app = createApp(App)

// ========== 步骤 2：注册 Element Plus UI 组件库 ==========
// 全局注册后，所有 .vue 文件中可直接使用 el-button / el-table / el-form / el-dialog 等 50+ 组件
// 无需在每个组件中手动 import Element Plus 组件
app.use(ElementPlus)

// ========== 步骤 3：注册 Pinia 状态管理 ==========
// createPinia() 返回 Pinia 实例，app.use() 将其安装到 Vue 应用中
// CRITICAL：必须在 Router 之前注册，因为 router/index.js 的 beforeEach 守卫内部调用了 useUserStore()（Pinia store）
// 如果 Router 先注册，守卫执行时 Pinia 尚未安装，会报 "getActivePinia()" was called but there was no active Pinia 错误
app.use(createPinia())

// ========== 步骤 4：注册 Vue Router ==========
// router 实例来自 router/index.js，包含：
//   - 12 路由定义：/login + /(AppLayout 父路由) + 11 个子路由 + 404 catch-all
//   - beforeEach 全局前置守卫：鉴权拦截（token 过期预检 + 登录拦截 + 管理员权限检查）
//   - afterEach 全局后置钩子：动态设置浏览器标签页标题
app.use(router)

// ========== 步骤 5：按需注册 Element Plus 图标组件 ==========
// Element Plus 图标以 Vue 组件形式使用（模板中写 <Plus /> 而非 <el-icon><Plus /></el-icon>）
// 此处通过循环批量注册 16 个图标为全局组件（模板中可直接用组件名，共 9 个页面 + 2 个组件使用）
const icons = {
  Plus, Wallet, Menu, List, Money, Calendar, Sort,          // 业务相关图标（新增/钱包/菜单/列表/金额/日期/排序）
  TrendCharts, Upload, Setting, SwitchButton,               // 功能相关图标（趋势图/上传/设置/切换按钮）
  HomeFilled, UserFilled, Expand, Fold                      // 导航相关图标（首页/用户/展开侧栏/折叠侧栏）
}
// 遍历图标对象，将每个图标组件按 key（变量名）全局注册
// 注册后模板中写 <component :is="图标名"> 或直接写 <Plus />（需配合 <el-icon> 包裹）
// 示例：<el-icon><Plus /></el-icon> 渲染为加号图标
for (const [key, component] of Object.entries(icons)) {
  app.component(key, component)                              // Vue 3 app.component(name, definition) 注册全局组件
}

// ========== 步骤 6：挂载应用到 DOM ==========
// 将整个 Vue 应用渲染到 index.html 中的 <div id="app"></div> 节点内
// 此后 Vue 接管该 DOM 节点，根据 URL 匹配路由 → 渲染对应组件（如 / → AppLayout.vue → DashboardPage.vue）
app.mount('#app')
