# Phase 5 R-06 前端代码审核报告 · 全页面 · 2026-05-16

## 审核元数据
- 审核日期:2026-05-16
- 审核切片:全页面(11 页面 + api(8) + stores(1) + router + layout)
- 使用模型:mimo-v2-pro
- 输入摘要:24 个前端源文件 · api(8) + views(11) + stores(1) + router(1) + layout(1) + main.js + App.vue

## 审核报告

### 维度 1:功能完整性
- **issue-1** [严重度:中]:缺少 getBudgetAlert API 函数
  - **位置**:api/budget.js
  - **修复建议**:后端 BudgetController 已实现 GET /api/budget/alert，前端 api/budget.js 缺少对应调用函数。添加 `export function getBudgetAlert(params) { return request.get('/budget/alert', { params }) }`

### 维度 2:安全性
无 v-html、无 innerHTML、token 存 localStorage ✅、无敏感信息硬编码 ✅、路由守卫生效 ✅

### 维度 3:响应式数据
ref/reactive 选用正确 ✅、onMounted 加载数据 ✅、onUnmounted 清理 ECharts ✅

### 维度 4:Element Plus 用法
- **issue-2** [严重度:中]:多个页面删除操作缺少 .catch(() => {}) 处理取消
  - **位置**:AccountPage.vue:178、TransactionListPage.vue(类似位置)
  - **修复建议**:ElMessageBox.confirm 用户点取消会 reject Promise，应加 `.catch(() => {})` 避免未捕获错误

### 维度 5:API 调用
前端 API 模块 URL 不带 /api 前缀 ✅、HTTP method 正确 ✅、import request 正确 ✅、无手动加 token ✅

### 维度 6:错误处理三态
loading 状态 7/11 页面有 ✅、拦截器统一处理错误 ✅、组件层不重复 ElMessage.error ✅

### 维度 7:用户体验
- **issue-3** [严重度:低]:路由缺少 meta.title 字段
  - **位置**:router/index.js:16-66
  - **修复建议**:各子路由未设置 meta.title，无法用于面包屑或页签标题。为每个路由添加 meta: { title: 'xxx' }

### 维度 8:依规范核对

#### api/ 子目核对
- **参考集**:import request from './request' / URL 不带 /api / HTTP method 正确 / 命名导出
- **被检集**:8 个 API 文件全部 import request from './request'，URL 不含 /api，method 正确，命名导出
- **差集 / 结论**:全部对齐。检查 5 项 · 全符合。

#### views/LoginPage.vue 核对
- **参考集**:<script setup> / 三步登录(token存+store写+router.push) / form rules / loading 防双击
- **被检集**:162 行 <script setup> ✅ / handleLogin 存 token(110)+setUser(111)+router.push(114) ✅ / loginRules + registerRules ✅ / :loading="loginLoading"(15) ✅
- **差集 / 结论**:全部对齐。检查 4 项 · 全符合。单用户角色无需 homeByRole。

#### stores/user.js 核对
- **参考集**:defineStore 组合式 / userId + username + setUser + clearUser + isLoggedIn
- **被检集**:defineStore('user', () => {...}) ✅ / userId + username refs ✅ / setUser ✅ / clearUser ✅ / isLoggedIn 检查 token ✅
- **差集 / 结论**:全部对齐。检查 4 项 · 全符合。

#### router/index.js 核对
- **参考集**:beforeEach 守卫 / requiresAuth / query.redirect / /login 公开
- **被检集**:beforeEach(73-83) ✅ / /login requiresAuth:false(10) ✅ / query.redirect(77) ✅ / 已登录跳 /(79) ✅
- **差集 / 结论**:缺少 meta.title 字段(issue-3)。检查 4 项 · 3 符合 1 缺失。

## 反例推演结果

### 推演 A · XSS 推演
无 v-html / innerHTML 使用。所有用户输入通过 {{ }} 插值(默认转义)。安全 ✅

### 推演 B · token 失效跳转推演
用户在 AccountPage 编辑 → token 过期 → axios 收 401 → localStorage 清 token + router.push('/login') → 用户重登 → route.query.redirect 带回 /account → 回到编辑页(表单丢失，但这是标准行为)。推演链完整 ✅

### 推演 C · 双击防重推演
AccountPage 点击"确定" → submitting=true 禁用按钮 → 请求完成 → submitting=false。无防双击缺失 ✅

### 推演 D · 拦截器 vs 组件层推演
接口返回 500 → 拦截器 ElMessage.error("服务器错误") → 组件层 catch 只 log 不重复弹。分工正确 ✅

## 修复行动建议
1. **中** issue-2:删除操作加 .catch(() => {}) 处理取消
2. **中** issue-1:api/budget.js 补 getBudgetAlert 函数
3. **低** issue-3:路由加 meta.title
