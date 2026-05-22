/**
 * 前端应用入口文件
 * 职责：创建 Vue 应用实例，注册全局插件和组件
 */
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import {
  Plus, Wallet, Menu, List, Money, Calendar, Sort,
  TrendCharts, Upload, Setting, SwitchButton,
  HomeFilled, UserFilled, Expand, Fold
} from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
// 全局颜色 CSS 变量（必须在 App 挂载前引入，确保 :root 变量对所有组件生效）
import './styles/color-vars.css'
import { createPinia } from 'pinia'

// 创建 Vue 应用实例
const app = createApp(App)

// 注册 Element Plus UI 组件库（全局）
app.use(ElementPlus)

// 注册 Pinia 状态管理（必须在 Router 之前，因为路由守卫使用 Pinia store）
app.use(createPinia())

// 注册 Vue Router 路由（→ router/index.js）
app.use(router)

// 按需注册 Element Plus 图标组件（仅注册项目中实际使用的图标，减少打包体积）
// 使用图标: SidebarMenu(8) + AppLayout(3: Expand/Fold) + 各页面Plus(4) + Dashboard(3)
const icons = {
  Plus, Wallet, Menu, List, Money, Calendar, Sort,
  TrendCharts, Upload, Setting, SwitchButton,
  HomeFilled, UserFilled, Expand, Fold
}
for (const [key, component] of Object.entries(icons)) {
  app.component(key, component)
}

// 挂载应用到 #app 节点
app.mount('#app')
