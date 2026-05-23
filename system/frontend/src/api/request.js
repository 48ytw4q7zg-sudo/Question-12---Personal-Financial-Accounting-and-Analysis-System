/**
 * axios 实例配置文件
 * 职责：创建统一的 HTTP 客户端，配置请求/响应拦截器
 *
 * 所有业务 API 模块（user.js / account.js / ...）都导入此实例发起请求
 * → 调用关系：api/user.js, api/account.js, api/category.js, api/transaction.js,
 *             api/budget.js, api/recurring-bill.js, api/statistics.js 全部 import 本文件
 */
import axios from 'axios'                                   // 导入axios库
import { ElMessage } from 'element-plus'                    // 导入消息提示组件
import router from '../router'                              // 导入路由实例（用于401跳转）
import { useUserStore } from '../stores/user'               // 导入用户状态store

// 创建 axios 实例，统一配置 baseURL 和超时时间（支持环境变量覆盖）
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',     // API基础路径，与vite proxy对齐
  timeout: Number(import.meta.env.VITE_API_TIMEOUT) || 10000 // 10秒超时
})

// 401 跳转去重标志（防止多个并发 401 请求重复跳转 /login）
let isRedirecting = false                                   // 401跳转去重标志

/**
 * 请求拦截器
 * 功能：从 localStorage 读取 JWT token，自动附加到 Authorization Header
 * 这样所有经过此实例的请求都会自动携带 token，无需业务代码手动处理
 */
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')               // 从本地存储读取token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`        // 携带JWT到请求头
  }
  return config                                             // 返回修改后的配置
}, error => {
  return Promise.reject(error)                              // 请求配置错误直接reject
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
  const { code, message, data } = res.data || {}           // 解构响应数据
  if (code === 200) {
    // 成功 → 返回 data，所有 API 函数拿到的返回值就是 data 部分
    return data                                             // 返回业务数据
  } else if (code === 401) {
    // token 过期或未登录 → 跳转登录页（去重防止并发 401 重复跳转）
    if (!isRedirecting) {
      isRedirecting = true                                  // 设置跳转标志防止重复
      // 清除 token + Pinia store 状态：统一清除 localStorage 和 Pinia store，避免 isLoggedIn 保持 true
      const userStore = useUserStore()                      // 获取用户store实例
      userStore.clearUser()                                 // 清除用户状态和token
      ElMessage.error('登录已过期，请重新登录')               // 提示过期
      // 防止 redirect 到 /login 造成循环跳转
      const currentPath = router.currentRoute.value.fullPath // 当前路由路径
      const redirectPath = currentPath.startsWith('/login') ? '/' : currentPath // 防循环跳转
      router.push({ path: '/login', query: { redirect: redirectPath } }) // 跳转登录页
        .finally(() => { isRedirecting = false })           // 跳转完成重置标志
    }
    return Promise.reject(new Error(message))               // reject业务错误
  } else {
    // 业务异常（如用户名重复、余额不足等）→ 弹错误提示
    ElMessage.error(message || '请求失败')                  // 弹出错误提示
    return Promise.reject(new Error(message))               // reject业务错误
  }
}, error => {
  // 网络层异常（超时、断网、服务器 5xx 等） — 区分状态码提供更精准提示
  // 状态码判断顺序：先精确匹配(404/403) → 再范围匹配(>=500) → 最后兜底
  if (error.response) {                                     // 有响应对象
    const status = error.response.status                    // 获取HTTP状态码
    if (status === 404) {
      ElMessage.error('请求的资源不存在')                    // 404资源不存在
    } else if (status === 403) {
      ElMessage.error('无权限访问')                          // 403禁止访问
    } else if (status >= 500) {
      ElMessage.error('服务器内部错误，请稍后重试')           // 5xx服务器错误
    } else {
      ElMessage.error(error.message || '网络异常')           // 其他网络异常
    }
  } else if (error.code === 'ECONNABORTED') {
    ElMessage.error('请求超时，请检查网络连接')               // 超时提示
  } else {
    ElMessage.error('网络连接异常，请检查网络')               // 通用网络异常
  }
  return Promise.reject(error)                              // reject网络错误
})

export default request                                      // 导出axios实例