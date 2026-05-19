// Vite 构建配置
// 开发服务器端口 5173, 代理 /api 请求到后端 localhost:8080
// 构建时按 vendor 分包(echarts/element/vue), 优化首屏加载

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,  // 开发服务器端口, 与 CLAUDE.md 约定的前端端口一致
    proxy: {
      // 将 /api 开头的请求代理到后端 Spring Boot(localhost:8080)
      // changeOrigin: true 修改请求头中的 Host 字段, 避免后端 CORS 拒绝
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    // 单个 chunk 超过 800KB 时发出警告(避免打包过大影响首屏加载)
    chunkSizeWarningLimit: 800,
    rollupOptions: {
      output: {
        // 手动分包策略: 将大型第三方库拆分为独立 chunk, 利用浏览器并行加载
        // vendor-echarts: ECharts 图表库(~800KB), 仅 AnalyticsPage/DashboardPage 使用
        // vendor-element: Element Plus UI 库 + 图标(~500KB), 全局使用
        // vendor-vue: Vue Router + Pinia(~100KB), 路由和状态管理
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('echarts')) return 'vendor-echarts'
            if (id.includes('element-plus') || id.includes('@element-plus/icons-vue')) return 'vendor-element'
            if (id.includes('vue-router') || id.includes('/pinia/')) return 'vendor-vue'
          }
        }
      }
    }
  }
})
