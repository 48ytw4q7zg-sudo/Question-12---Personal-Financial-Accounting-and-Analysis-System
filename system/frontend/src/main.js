/**
 * 前端应用入口文件
 * 职责：创建 Vue 应用实例，注册全局插件和组件
 */
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'

// 创建 Vue 应用实例
const app = createApp(App)

// 注册 Element Plus UI 组件库（全局）
app.use(ElementPlus)

// 注册 Vue Router 路由（→ router/index.js）
app.use(router)

// 注册 Pinia 状态管理
app.use(createPinia())

// 批量注册 Element Plus 图标组件为全局组件，模板中可直接 <el-icon><Xxx /></el-icon> 使用
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 挂载应用到 #app 节点
app.mount('#app')
