// ============================================================
// §1.4 数据流 节点 ④ 请求拦截器（注入 token）+ 节点 ⑮ 响应拦截器（剥壳 + 401 跳转 + 重试）
// §2.2 逐文件讲解 ⑨/⑩ — request.js（axios 实例 · 前端所有请求的"总闸"）
//
// 这个文件做什么：创建 axios 实例（baseURL=/api/v1, timeout=10s）
//                 10 个 API 模块都导入这个实例发请求
//                 请求拦截器：自动从 localStorage 读 token → 注入 Authorization 头
//                 响应拦截器：三段处理 code 200/401/其他 + 指数退避重试
//
// 答辩讲什么（4 个点）：
//   1. axios.create — baseURL 为什么是 /api/v1（对齐 Vite Proxy）
//   2. 请求拦截器 — 为什么统一注入 token（10 个模块不会遗漏）
//   3. 响应拦截器成功分支 — 三段处理（200 剥壳 / 401 跳转 / 其他提示）
//   4. 响应拦截器错误分支 — 指数退避重试（1s→2s→4s + 随机抖动防惊群效应）
//
// ▶ 逐文件讲解下一个（Ctrl+P）：
//   system/frontend/src/views/TransactionListPage.vue
//   （§2.2 逐文件讲解 ★ ⑩/⑩ — 自选前端页面 · 3 个核心函数）
// ============================================================
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

// 创建 axios 实例：所有 API 模块（user.js / account.js 等 10+ 个文件）均导入此实例发请求，统一管理 baseURL 和超时。
const request = axios.create({
  // 【做什么】每个请求 URL 前自动拼接 /api/v1。如 user.js 写 request.post('/user/login') → 实际发出 POST /api/v1/user/login。
  // 【为什么 '/api/v1' 而不是 '/' 或其他】① 对齐 vite.config.js 的 proxy——Vite Dev Server 将 /api 前缀的请求转发到后端 localhost:8080，浏览器认为请求发给了同源 Vite Server 从而绕过跨域限制；② v1 是 API 版本号——后端可同时部署 /api/v1 和 /api/v2 两套接口，前端只需改此处 baseURL 即可全局切换，调用方零改动；③ 支持 VITE_API_BASE_URL 覆盖——生产环境 Nginx 反代时可配成完整域名如 https://api.example.com/api/v1。
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  // 【做什么】10 秒超时——超过 10 秒未收到数据，axios 自动中断并进入错误分支（async error 回调）。
  // 【为什么 10 秒】① 体验平衡——10 秒是"用户愿等"与"该提示了"的合理分界线，超时立即提示而非让用户干等；② 配合指数退避重试（最多 3 次，间隔 1s/2s/4s，等待约 7s）——单请求最大总耗时 = 10s+1s+10s+2s+10s+4s ≈ 37s，仍在浏览器 HTTP 超时容忍范围内；③ VITE_API_TIMEOUT 环境变量可按部署网络条件灵活调整（内网更短、公网可更长）。
  timeout: Number(import.meta.env.VITE_API_TIMEOUT) || 10000
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
  // ── 请求拦截器：每个请求发出前自动执行 ──
  // 职责：① 从 localStorage 读 token 注入 Authorization 头 ② 初始化重试计数器
  // 为什么用拦截器而非在每个 API 模块手动加？10 个 API 模块、几十个接口——拦截器一次性保证所有请求统一携带 token
  const token = localStorage.getItem('token')               // 【做什么】从浏览器 localStorage 按 'token' 键读取 JWT /【为什么】JWT 无状态，不需要 cookie——键名 'token' 与 stores/user.js 写入方和 api/request.js 请求拦截器读取方保持一致，三个读写点闭环
  if (token) {
    config.headers.Authorization = `Bearer ${token}`        // 【做什么】将 token 拼装为 "Bearer <token>" 格式，写入请求头的 Authorization 字段 /【为什么】后端 LoginInterceptor 按 "Bearer " 前缀解析 JWT——格式对齐 RFC 6750 标准；10 个 API 模块共用一个拦截器注入，任一遗漏都会 401，统一拦截保证一致性
  }
  config.retryCount = config.retryCount || 0                // 【做什么】初始化当前请求的重试计数器为 0 /【为什么】axios 可能跨请求复用 config 对象——|| 0 确保计数器始终从数值起步；每个请求独立计数，防止请求 A 的重试次数被请求 B 继承而错误跳过重试
  return config                                             // 【做什么】返回修改后的 config 对象 /【为什么】axios 拦截器强制要求 return config——不返回则请求被静默取消：既不发送 HTTP 请求，也不 reject，调用方的 await 永远不 resolve
}, error => {
  return Promise.reject(error)                              // 【做什么】将请求配置阶段的错误包装为 rejected Promise 向下传递 /【为什么】Promise.reject 确保错误能被响应拦截器的错误分支和调用方 catch 同时捕获——直接 throw 在异步上下文中不会自动变成 rejection
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
  // ── 响应拦截器：三段处理（对齐 CLAUDE.md §一·三 全栈接口契约）──
  // 后端统一返回 Result<T> = { code, message, data }，此拦截器按 code 值分三路：
  // ① code===200 → 剥壳返回 data  ② code===401 → 清 token + 跳 /login  ③ 其他 → 弹错误提示 + reject
  const { code, message, data } = res.data || {}           // 【做什么】从后端响应中解构 code/message/data 三个字段 /【为什么】后端返回 {code, message, data} 的 Result<T> 结构——|| {} 是防御性兜底：如果后端因 bug 返回非标准结构（如 res.data 为 undefined），不解构直接点属性会抛 TypeError 导致白屏
  if (code === 200) {
    return data                                             // 【做什么】仅返回 data 部分，丢弃外层的 code 和 message /【为什么】code===200 代表成功——code 和 message 对业务代码无意义；直接返回 data 让所有调用方（如 await loginUser(form)）拿到的直接是 {token, userId, role}，无需额外写 .then(res=>res.data.data)
  } else if (code === 401) {
    if (!isRedirecting) {
      isRedirecting = true                                  // 【做什么】立即上锁——将模块级去重标志置为 true /【为什么】Dashboard 首页可能同时有 3 个统计 API 并行调用，如果它们同时返回 401，不加锁会导致 router.push 被调用 3 次——Vue Router 控制台报 NavigationDuplicated 警告，且用户看到 3 次过期提示弹出
      const userStore = useUserStore()                      // 【做什么】动态获取 Pinia user store 实例 /【为什么】必须在拦截器回调内部调用 useUserStore()——模块顶层调用时 Pinia 尚未激活（Vue 实例还未创建），会抛出 getActivePinia() 错误
      userStore.clearUser()                                 // 【做什么】一次性清除 Pinia 中的用户状态（role、userId 等字段）+ localStorage 中的 token /【为什么】只清 localStorage 不够——Pinia store 中的 isLoggedIn 仍为 true，路由守卫会读到旧状态误判用户已登录然后放行；必须两处同时清除
      ElMessage.error('登录已过期，请重新登录')               // 【做什么】弹出红色错误提示告诉用户被踢到登录页的原因 /【为什么】必须让用户知道是"过期"而非"系统故障"——否则用户困惑"为什么突然跳到登录页了？"
      const currentPath = router.currentRoute.value.fullPath // 【做什么】获取 401 发生时的当前路由完整路径（含 query 参数）/【为什么】需要判断当前是否已在 /login——如果已在 /login 还要 redirect=/login，登录后会产生死循环：登录成功 → router.push('/login') → 路由守卫再次踢
      const redirectPath = currentPath.startsWith('/login') ? '/' : currentPath // 【做什么】防循环：已在 /login 则 redirect=/（首页），否则 redirect=原页面 /【为什么】redirect 参数会在登录成功后用于 router.push(redirect)——必须保证 redirect 值本身不是 /login
      const redirectTimeoutId = setTimeout(() => { isRedirecting = false }, 2000) // 【做什么】启动 2 秒超时看门狗——到期自动解锁 isRedirecting /【为什么】router.push 的 .finally() 只在导航成功时触发——如果路由守卫因某 bug 调了 next(false) 取消导航，.finally 永不执行，isRedirecting 永久为 true，后续所有 401 都被吞掉。看门狗是最后的兜底保证
      router.push({ path: '/login', query: { redirect: redirectPath } }) // 【做什么】编程式导航跳转到 /login，query 参数携带 redirect 目标路径 /【为什么】Vue Router 的编程式导航——不是 window.location.href 的整页刷新跳转；redirect 参数供 LoginPage 登录成功后调用 router.push(redirect) 自动回到被打断的页面
        .finally(() => {
          clearTimeout(redirectTimeoutId)                   // 【做什么】清除 2 秒超时看门狗定时器 /【为什么】导航已成功完成（.finally 被执行），无需再等 2 秒触发看门狗——clearTimeout 防止过期回调误执行和内存泄漏
          isRedirecting = false                             // 【做什么】解锁去重标志 /【为什么】导航已结束——此后的 401 需要能再次触发跳转流程，不能永久锁住
        })
    }
    const err401 = new Error(message)                       // 【做什么】用后端返回的 message 构造标准 Error 对象 /【为什么】需要创建一个能被调用方 catch 捕获的标准 Error——包含后端响应中的具体错误描述（如"token已过期"）
    err401.noRetry = true                                   // 【做什么】给 Error 对象打上 noRetry=true 标记 /【为什么】401 是认证失效而非网络抖动——重试 3 次（1s+2s+4s=7s 总等待）也还是 401，纯属浪费；noRetry 标记让 isRetryable() 函数能识别并跳过重试
    return Promise.reject(err401)                           // 【做什么】以 rejected Promise 向下传递 401 错误 /【为什么】Promise.reject 让调用方在 await 处抛异常进入 catch 块——调用方可据此显示"请重新登录"或重置当前页面状态
  } else {
    ElMessage.error(message || '请求失败')                  // 【做什么】弹出 Element Plus 红色错误消息，内容用后端返回的 message /【为什么】大多数业务错误（如"余额不足"、"用户名重复"）只需提示用户即可——||'请求失败' 是 message 为 null/undefined 时的兜底文案
    const errBiz = new Error(message)                       // 【做什么】创建业务错误 Error 对象 /【为什么】虽然已在 ElMessage 阶段提示了用户，仍需 reject——某些组件可能需要 try-catch 后做额外处理（如"用户名重复"时在输入框下方标红，而非仅弹一个消息）
    errBiz.noRetry = true                                   // 【做什么】标记业务错误不可重试 /【为什么】业务错误（余额不足、用户名重复）是操作不合法而非服务不可用——重试 100 次也不会改变结果，标记 noRetry 让指数退避重试逻辑跳过这些错误
    return Promise.reject(errBiz)                           // 【做什么】将业务错误以 rejected Promise 传给调用方 /【为什么】组件层 catch 可按 message 内容做针对性 UI 处理——如注册页 catch 到"用户名已被注册"后在用户名输入框下方显示红色提示
  }
}, async error => {
  const config = error.config

  // ── 指数退避重试策略 ──
  // 为什么用指数退避而非固定间隔？大量客户端同时重试 = "惊群效应"——服务器刚恢复就被瞬间涌入的请求再次打垮
  // 指数退避（1s→2s→4s）+ 随机抖动（±10-30%）把重试时间分散到不同时刻，给服务器喘息空间
  if (config && isRetryable(error) && config.retryCount < RETRY_CONFIG.maxRetries) {
    config.retryCount += 1                                   // 【做什么】重试计数器自增 1 /【为什么】每次重试都要记录次数——retryCount < maxRetries(3) 的上限判断依赖此值；每个请求的 config 对象独立，计数不会跨请求污染
    const jitter = Math.random() * 0.2 + 0.1                  // 【做什么】生成 0.1-0.3 的随机抖动因子 /【为什么】如果所有客户端在第 1 次重试时恰好同时等待 1000ms，它们会在同一毫秒同时发出重试请求——jitter 把重试分散到 1100-1300ms 区间，消除"同步重试冲击波"
    const baseDelayTime = RETRY_CONFIG.baseDelay * Math.pow(2, config.retryCount - 1) // 【做什么】按 2 的幂次计算基础延迟——第 1/2/3 次 = 1000ms/2000ms/4000ms /【为什么】次数越多间隔越长——给服务器逐步恢复的时间，避免在服务仍不健康时密集重试
    const delayTime = Math.min(
      Math.floor(baseDelayTime * (1 + jitter)),               // 【做什么】基础延迟乘以抖动系数后向下取整——得到最终等待毫秒数 /【为什么】Math.floor 保证 setTimeout 收到整数；乘以 (1+jitter) 把随机分散注入延迟
      RETRY_CONFIG.maxDelay                                   // 【做什么】用 Math.min 将延迟上限卡在 maxDelay(10s) /【为什么】指数退避在第 5+ 次重试时延迟会超过 16s——10 秒上限保证用户不会等到不耐烦才看到错误提示
    )
    await delay(delayTime)                                   // 【做什么】用 Promise+setTimeout 异步等待 delayTime 毫秒 /【为什么】await 让出事件循环主线程——浏览器在此期间仍可响应用户点击、滚动、输入，不会卡死 UI
    return request(config)                                   // 【做什么】用原 config 重新发起 HTTP 请求，走 request 实例而非 axios /【为什么】递归调用 request(config) 而非 axios(config)——复用 axios 实例的完整拦截器链（请求拦截器重新注入最新 token、响应拦截器再次三段处理），保证重试请求与首次请求行为完全一致
  }

  // ===== 不可重试或已达重试上限 → 错误提示 =====
  // 【为什么 4xx 不在 RETRY_CONFIG.retryableStatuses 列表、不会被重试】
  // 4xx 是客户端错误——请求本身有问题，服务器状态不会自行改变，重试结果永远相同：
  //   400 Bad Request → 参数格式错误，不修改参数重试 100 次也是 400
  //   403 Forbidden → 权限不足，不提升用户角色重试是徒劳的
  //   404 Not Found → 资源不存在（已删/ID错误），重试不会让资源凭空出现
  //   429 Too Many Requests → 已被限流，继续重试只会让限流更严厉
  // 只有 5xx（服务端临时故障，可能自愈）和网络层异常（断网/超时可能恢复）才值得重试。
  // 状态码判断顺序：先精确匹配(404/403) → 再范围匹配(>=500) → 最后兜底——确保不遗漏不误判。
  if (error.response) {                                     // 【做什么】有响应对象 = 服务器已收到请求并返回了 HTTP 状态码（4xx/5xx）/【为什么】以此区分"服务器回应了"和"根本没联系上服务器"——前者给精准提示，后者给通用网络提示
    const status = error.response.status                    // 【做什么】取 HTTP 状态码（200/400/401/403/404/500...）/【为什么】按状态码分层提示——让用户知道是"资源不存在"还是"服务器崩了"，便于自行判断下一步：重试 vs 放弃 vs 联系管理员
    if (status === 404) {
      ElMessage.error('请求的资源不存在')                    // 【做什么】404 专用提示 /【为什么】404 通常是用户操作了已被删除的数据（如点击已删账户的链接）——清晰告知便于用户理解"不是系统坏了，是那个东西不在了"
    } else if (status === 403) {
      ElMessage.error('无权限访问')                          // 【做什么】403 禁止访问 /【为什么】403 通常是角色权限不足（普通用户访问 /admin 接口）或 CORS/CSRF 校验失败——提示"无权限"而非"网络异常"让用户知道是权限问题
    } else if (status >= 500) {
      ElMessage.error('服务器内部错误，请稍后重试')           // 【做什么】5xx 服务器错误提示 /【为什么】走到此处说明已经过了 3 次重试仍 5xx——服务端持续不可用，"请稍后重试"暗示用户这是临时性问题而非永久故障
    } else {
      ElMessage.error(error.message || '网络异常')           // 【做什么】400/405/429 等未单独分类的 HTTP 错误兜底 /【为什么】这些状态码出现概率极低（正常业务的错误走响应拦截器成功分支的 code!=200 逻辑，不会走到这里）——用服务器返回的 message 或通用兜底文案保持用户体验完整
    }
  } else if (error.code === 'ECONNABORTED') {
    // 【做什么】axios 自身超时信号提示 /【为什么】error.response 不存在 + code='ECONNABORTED' 是 axios 在 timeout 到期时触发的——区别"超时"（请求出去了但服务器太慢）和"断网"（请求没出去），给用户更精准的操作指引："检查网络连接"而非"刷新页面"
    ElMessage.error('请求超时，请检查网络连接')
  } else {
    // 【做什么】完全无响应的兜底提示 /【为什么】error.response 不存在 + 非超时 = 真正的网络断开/DNS失败/CORS拦截/服务器宕机——最外层兜底，覆盖所有前面判断没命中的未知网络异常场景
    ElMessage.error('网络连接异常，请检查网络')
  }
  return Promise.reject(error)                              // 【做什么】最终 reject——将错误继续向下传递给调用方的 catch 块 /【为什么】拦截器已向用户弹了提示，但调用方可能仍需做额外恢复操作（如关闭 loading、重置表单状态）——reject 让调用方有机会执行这些清理逻辑
})

export default request                                      // 导出axios实例