/**
 * axios 实例配置文件
 * 职责：创建统一的 HTTP 客户端，配置请求/响应拦截器
 *
 * 所有业务 API 模块（user.js / account.js / ...）都导入此实例发起请求
 * → 调用关系：api/user.js, api/account.js, api/category.js, api/transaction.js,
 *             api/budget.js, api/recurring-bill.js, api/statistics.js 全部 import 本文件
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 创建 axios 实例，统一配置 baseURL 和超时时间
const request = axios.create({
  baseURL: '/api',      // 与 vite.config.js 的 proxy 代理配置对齐，所有请求自动加 /api 前缀
  timeout: 10000        // 10 秒超时
})

/**
 * 请求拦截器
 * 功能：从 localStorage 读取 JWT token，自动附加到 Authorization Header
 * 这样所有经过此实例的请求都会自动携带 token，无需业务代码手动处理
 */
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
}, error => {
  return Promise.reject(error)
})

/**
 * 响应拦截器（三段处理，对齐 CLAUDE.md §一·三 全栈接口契约）
 * 后端统一返回 Result<T> 结构：{ code, message, data }
 *
 * 处理逻辑：
 *   code === 200  → 成功，直接返回 data（业务层拿到的就是纯数据）
 *   code === 401  → 未登录/过期，清 token + 跳 /login + 提示
 *   其他 code     → 业务错误，ElMessage 提示 + reject（组件层可通过 catch 捕获）
 */
request.interceptors.response.use(res => {
  // 可选链保护：防止后端返回非 Result<T> 结构时解构报错
  const { code, message, data } = res.data || {}
  if (code === 200) {
    // 成功 → 返回 data，所有 API 函数拿到的返回值就是 data 部分
    return data
  } else if (code === 401) {
    // token 过期或未登录 → 清除本地 token，跳转登录页并保留当前路径用于登录后回跳
    localStorage.removeItem('token')
    router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
    ElMessage.error('未登录')
    return Promise.reject(new Error(message))
  } else {
    // 业务异常（如用户名重复、余额不足等）→ 弹错误提示
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  }
}, error => {
  // 网络层异常（超时、断网、服务器 500 等）
  ElMessage.error(error.message || '网络异常')
  return Promise.reject(error)
})

export default request
