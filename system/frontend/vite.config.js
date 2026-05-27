// ========== Vite 构建配置文件 ==========
// 开发服务器端口 5173，代理 /api 请求到后端 SpringBoot localhost:8080
// 构建时按 vendor 分包，ECharts 按需拆分（echarts-core/charts/components/renderers），优化首屏加载
// 引用方：vite dev / vite build 命令自动读取本文件

import { defineConfig } from 'vite'                           // 从 vite 库导入 defineConfig 辅助函数（提供 IDE 类型提示）
import vue from '@vitejs/plugin-vue'                          // 从 @vitejs/plugin-vue 导入 Vue SFC 编译插件（处理 .vue 单文件组件）

export default defineConfig({                                 // 导出 Vite 配置对象（defineConfig 包裹以获得智能提示）
  plugins: [vue()],                                            // 注册 Vue SFC 编译插件（必需，否则无法解析 .vue 文件）
  server: {                                                    // 开发服务器配置
    port: 5173,                                                // 开发服务器端口，与 CLAUDE.md 约定的前端端口一致
    proxy: {                                                   // 开发代理配置（解决前后端跨域问题）
      // 将 /api 开头的请求代理到后端 Spring Boot（localhost:8080）
      // changeOrigin: true 修改请求头中的 Host 字段，避免后端 CORS 拒绝
      '/api': {                                                // 匹配所有 /api 前缀的请求
        target: 'http://localhost:8080',                       // 代理目标：Spring Boot 后端地址
        changeOrigin: true                                     // 修改 Host 头为目标地址，防后端 CORS 校验失败
      }                                                        // /api 代理规则结束
    }                                                          // proxy 配置结束
  },                                                           // server 配置结束
  build: {                                                     // 生产构建配置
    // 生产构建禁止 sourcemap，防止源代码暴露（安全加固）
    sourcemap: false,                                          // 禁用 sourcemap 避免生产环境泄露源码
    // 单个 chunk 超过 1000KB 时发出警告（Element Plus 945KB 无法进一步拆分，设为 1000KB 减少噪音）
    chunkSizeWarningLimit: 1000,                               // 分包大小警告阈值 1000KB
    rollupOptions: {                                           // Rollup 打包选项（Vite 生产构建底层使用 Rollup）
      output: {                                                // 输出配置
        // 手动分包策略：将大型第三方库拆分为独立 chunk，利用浏览器 HTTP/2 并行加载
        // ECharts 按需拆分：core/charts/components/renderers 各自独立 chunk
        //   仅 AnalyticsPage/DashboardPage 通过 echarts-lazy.js（→ utils/echarts-lazy.js）动态 import 使用
        // vendor-element: Element Plus UI 库，全局使用
        // vendor-icons: Element Plus 图标库（@element-plus/icons-vue），按需加载
        // vendor-router: Vue Router + Pinia（路由和状态管理，非 Vue 核心库）
        // vendor-axios: Axios HTTP 库（→ api/request.js）
        manualChunks(id) {                                     // 手动分包函数：根据模块 ID 决定归属 chunk
          if (id.includes('node_modules')) {                   // 仅处理 node_modules 中的第三方库（业务代码走默认策略）
            // ECharts 按需加载拆分：core/charts/components/renderers 各自独立 chunk
            // 不合并为单一 vendor-echarts，让浏览器按动态 import 并行加载
            if (id.includes('echarts/core') || id.includes('echarts/lib')) return 'echarts-core' // ECharts 核心模块
            if (id.includes('echarts/charts')) return 'echarts-charts'                           // ECharts 图表类型（PieChart/BarChart/LineChart）
            if (id.includes('echarts/renderers')) return 'echarts-renderers'                     // ECharts 渲染器（CanvasRenderer）
            if (id.includes('@element-plus/icons-vue')) return 'vendor-icons'                    // Element Plus 图标库
            if (id.includes('element-plus')) return 'vendor-element'                            // Element Plus UI 组件库
            if (id.includes('vue-router') || id.includes('/pinia/')) return 'vendor-router'     // Vue Router + Pinia 状态管理
            if (id.includes('axios')) return 'vendor-axios'                                    // Axios HTTP 客户端
          }                                                      // node_modules 判断结束
        }                                                        // manualChunks 函数结束
      }                                                          // output 配置结束
    }                                                            // rollupOptions 配置结束
  }                                                              // build 配置结束
})                                                               // defineConfig 调用结束