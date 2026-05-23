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
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',  // API基础路径（v1版本），与vite proxy对齐
  timeout: Number(import.meta.env.VITE_API_TIMEOUT) || 10000 // 10秒超时
})

// 401 跳转去重标志（防止多个并发 401 请求重复跳转 /login）
let isRedirecting = false                                   // 401跳转去重标志

// 请求重试配置
const RETRY_CONFIG = {
  maxRetries: 3,                                           // 最大重试次数
  baseDelay: 1000,                                         // 基础延迟（毫秒）
  maxDelay: 10000,                                         // 最大延迟（毫秒）
  retryableStatuses: [500, 502, 503, 504]                  // 可重试的HTTP状态码
}

/**
 * 延迟函数（用于指数退避）
 * @param {number} ms - 延迟毫秒数
 * @returns {Promise} 延迟Promise
 */
const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms))

/**
 * 判断错误是否可重试
 * 重试范围: ① 网络异常(无 response) ② 请求超时(ECONNABORTED) ③ 5xx 服务器错误
 * 不重试场景: ① 业务码非200(已被响应拦截器 reject 并标记 noRetry) ② 4xx 客户端错误 ③ 显式 noRetry 标志
 * @param {Error} error - axios错误对象 或 业务错误对象(含 noRetry 标志)
 * @returns {boolean} 是否可重试
 */
const isRetryable = (error) => {
  // P1-7 修复(Q-CR Loop1):业务错误(401/余额不足/参数非法等)显式标记 noRetry,直接跳过重试
  // 否则响应拦截器 reject 的 Error 对象因无 response 字段会被误判为网络错误而触发 3 次重试
  if (error && error.noRetry === true) {                     // 业务错误显式标记
    return false                                              // 不重试
  }
  // 网络错误或超时
  if (!error.response || error.code === 'ECONNABORTED') {
    return true
  }
  // 5xx服务器错误（排除4xx客户端错误）
  const status = error.response.status
  return RETRY_CONFIG.retryableStatuses.includes(status)
}

/**
 * 请求拦截器
 * 功能：从 localStorage 读取 JWT token，自动附加到 Authorization Header
 * 这样所有经过此实例的请求都会自动携带 token，无需业务代码手动处理
 * 同时初始化重试计数器
 */
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')               // 从本地存储读取token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`        // 携带JWT到请求头
  }
  // 初始化重试计数器（用于指数退避重试）
  config.retryCount = config.retryCount || 0                // 设置初始重试次数为0
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
      const userStore = useUserStore()                      // 获取用户store实例（stores/user.js）
      userStore.clearUser()                                 // 清除用户状态和token
      ElMessage.error('登录已过期，请重新登录')               // 提示过期
      // 防止 redirect 到 /login 造成循环跳转
      const currentPath = router.currentRoute.value.fullPath // 当前路由路径
      const redirectPath = currentPath.startsWith('/login') ? '/' : currentPath // 防循环跳转
      // P0-1 修复（Q-CR Loop1）：先声明 redirectTimeoutId 再使用,避免 TDZ ReferenceError
      // 旧代码在 .finally 闭包内引用了在其后 setTimeout 才赋值的 redirectTimeoutId,
      // 由于 const 的暂时性死区(TDZ),401 跳转流程会必抛 ReferenceError 导致 isRedirecting 永久卡死。
      const redirectTimeoutId = setTimeout(() => { isRedirecting = false }, 2000) // 先创建 2s 超时看门狗(导航失败时强制解锁)
      router.push({ path: '/login', query: { redirect: redirectPath } }) // 跳转登录页
        .finally(() => {
          clearTimeout(redirectTimeoutId)                   // 清除超时看门狗,防止内存泄漏和无效回调
          isRedirecting = false                             // 跳转完成重置标志
        })
    }
    // P1-7 修复(Q-CR Loop1):标记 noRetry,防止 axios error 拦截器把业务 401 当 5xx 重试
    const err401 = new Error(message)                       // 创建业务 401 错误对象
    err401.noRetry = true                                   // 标记不可重试(避免 isRetryable 误判)
    return Promise.reject(err401)                           // reject业务错误,中断后续 then 链
  } else {
    // 业务异常（如用户名重复、余额不足等）→ 弹错误提示
    ElMessage.error(message || '请求失败')                  // 弹出错误提示
    // P1-7 修复(Q-CR Loop1):标记 noRetry,业务异常(余额不足/重复用户名)不应自动重试
    const errBiz = new Error(message)                       // 创建业务错误对象
    errBiz.noRetry = true                                   // 标记不可重试
    return Promise.reject(errBiz)                           // reject业务错误
  }
}, async error => {
  const config = error.config

  // 请求重试逻辑：指数退避策略
  // 条件：有配置对象 && 错误可重试 && 未达最大重试次数
  if (config && isRetryable(error) && config.retryCount < RETRY_CONFIG.maxRetries) {
    config.retryCount += 1                                   // 重试次数+1
    // 计算延迟时间：指数退避 baseDelay * 2^(retryCount-1) + 随机抖动（10-30%），防止惊群效应
    const jitter = Math.random() * 0.2 + 0.1                  // 10-30%随机抖动因子
    const baseDelayTime = RETRY_CONFIG.baseDelay * Math.pow(2, config.retryCount - 1)  // 指数退避基础延迟
    const delayTime = Math.min(
      Math.floor(baseDelayTime * (1 + jitter)),               // 基础延迟 × (1 + 抖动)，向下取整
      RETRY_CONFIG.maxDelay                                   // 不超过最大延迟
    )
    await delay(delayTime)                                   // 等待延迟时间
    return request(config)                                   // 重新发起请求（递归调用）
  }

  // 不可重试或已达最大重试次数，执行原有错误处理逻辑
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