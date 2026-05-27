<!--
  ╔══════════════════════════════════════════════════════════════════════╗
  ║  📋 答辩参考文件（非主讲）— 登录/注册页面                                 ║
  ║                                                                      ║
  ║  【文件整体实现什么】                                                    ║
  ║  LoginPage.vue — 登录/注册页面，路由 /login，含 handleLogin()/handleRegister() ║
  ║  handleLogin() 展示了全栈15节点链路（前端→网络→后端→数据库→响应），可作为备选参考   ║
  ║                                                                      ║
  ║  ⚠ 答辩主讲文件已变更为 TransactionListPage.vue（收支记录页面）             ║
  ║     → 路径：system/frontend/src/views/TransactionListPage.vue            ║
  ╚══════════════════════════════════════════════════════════════════════╝
-->
<!--
  登录/注册页面
  路由：/login（唯一不嵌套 AppLayout 的页面）
  对应 PRD 功能：P0 登录/JWT（用户注册登录 + JWT token 签发与校验）

  功能说明：
    - Tab 切换登录/注册两个表单
    - 登录成功后存储 token 到 localStorage + 写入 userStore → 跳转首页（或 redirect 参数指定的页面）
    - 注册成功后自动切到登录 Tab 并回填用户名

  调用关系：
    → 调用 api/user.js 的 login()（登录接口）
    → 调用 api/user.js 的 register()（注册接口）
    → 调用 stores/user.js 的 setUser()（存储用户信息）
-->
<template>
  <div class="login-container">
    <!-- el-card：Element Plus 卡片容器组件，作为登录/注册表单的外层包裹 -->
    <el-card class="login-card">
      <h2 class="login-title">个人财务记账与分析系统</h2>

      <!-- el-tabs：Element Plus 标签页组件，用于登录/注册表单切换 -->
      <el-tabs v-model="activeTab">       <!-- v-model 绑定当前激活 Tab（'login' 或 'register'） -->
        <!-- 登录 Tab -->
        <el-tab-pane label="登录" name="login">
          <!-- el-form：Element Plus 表单组件，ref 用于手动校验，:model 绑定数据，:rules 校验规则 -->
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-width="0" @submit.prevent="handleLogin">
            <!-- el-form-item：表单项，prop 关联校验规则的字段名 -->
            <el-form-item prop="username" label="用户名" class="hidden-label">
              <!-- el-input：Element Plus 输入框，:prefix-icon 使用 @element-plus/icons-vue 的 User 图标 -->
              <el-input v-model="loginForm.username" placeholder="请输入用户名" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password" label="密码" class="hidden-label">
              <!-- show-password 显示密码切换按钮，type="password" 密码类型输入 -->
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item>
              <!-- el-button：Element Plus 按钮，:loading 绑定 loading 状态防止重复点击 -->
              <el-button type="primary" :loading="loginLoading" class="submit-btn" @click="handleLogin">登录</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 注册 Tab -->
        <el-tab-pane label="注册" name="register">
          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="0" @submit.prevent="handleRegister">
            <el-form-item prop="username" label="用户名" class="hidden-label">
              <el-input v-model="registerForm.username" placeholder="请输入用户名" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password" label="密码" class="hidden-label">
              <!-- Q-CR修复: @change触发confirmPassword重新校验，防止密码变更后确认密码校验状态残留 -->
              <!-- .catch(() => {}) 防止 validateField rejection 导致未捕获的 Promise 错误 -->
              <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password @change="() => registerFormRef.validateField('confirmPassword').catch(() => {})" />
            </el-form-item>
            <el-form-item prop="confirmPassword" label="确认密码" class="hidden-label">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请确认密码" :prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="registerLoading" class="submit-btn" @click="handleRegister">注册</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'                         // 导入Vue组合式API
import { useRouter, useRoute } from 'vue-router'            // 导入路由组合式函数
import { ElMessage } from 'element-plus'                    // 导入消息提示
import { User, Lock } from '@element-plus/icons-vue'        // 导入Element Plus图标（prefix-icon需组件引用而非字符串）
// → 调用 api/user.js 的 login() 和 register()
import { login, register } from '../api/user'                // 导入用户API（api/user.js）
import { USERNAME_MIN_LENGTH, USERNAME_MAX_LENGTH, PASSWORD_MIN_LENGTH, PASSWORD_MAX_LENGTH, USERNAME_PATTERN, USERNAME_PATTERN_MSG } from '../constants/finance' // 导入验证常量
// → 调用 stores/user.js 的 setUser()
import { useUserStore } from '../stores/user'                // 导入用户状态store（stores/user.js）
import { logger } from '../utils/logger'                    // Q-CR修复：导入日志工具（utils/logger.js），防止 ReferenceError

const log = logger('LoginPage')                             // Q-CR修复：创建日志实例（之前缺失导致 log.warn() 抛出 ReferenceError）
const router = useRouter()                                  // 路由实例
const route = useRoute()                                    // 当前路由信息
const userStore = useUserStore()                            // 用户状态store

// 当前激活的 Tab：'login' 或 'register'
const activeTab = ref('login')                              // 当前Tab页
const loginLoading = ref(false)       // 登录按钮 loading 防重复提交
const registerLoading = ref(false)    // 注册按钮 loading 防重复提交
const loginFormRef = ref(null)        // 登录表单引用（用于手动触发表单校验）
const registerFormRef = ref(null)     // 注册表单引用

// 登录表单数据
const loginForm = reactive({
  username: '',                                             // 用户名
  password: ''                                              // 密码
})

// 注册表单数据（多一个确认密码字段）
const registerForm = reactive({
  username: '',                                             // 用户名
  password: '',                                             // 密码
  confirmPassword: ''                                       // 确认密码
})

// 登录表单校验规则（el-form rules · 对齐 PRD P0-1: 用户名3-20字符字母数字下划线）
const loginRules = {
  username: [                                               // 用户名校验规则（使用常量 constants/finance.js）
    { required: true, message: '请输入用户名', trigger: 'blur' }, // 必填
    { min: USERNAME_MIN_LENGTH, max: USERNAME_MAX_LENGTH, message: `用户名长度为 ${USERNAME_MIN_LENGTH}-${USERNAME_MAX_LENGTH} 个字符`, trigger: 'blur' }, // 长度限制
    { pattern: USERNAME_PATTERN, message: USERNAME_PATTERN_MSG, trigger: 'blur' } // 格式限制
  ],
  password: [                                               // 密码校验规则（使用常量 constants/finance.js）
    { required: true, message: '请输入密码', trigger: 'blur' },   // 必填
    { min: PASSWORD_MIN_LENGTH, max: PASSWORD_MAX_LENGTH, message: `密码长度为 ${PASSWORD_MIN_LENGTH}-${PASSWORD_MAX_LENGTH} 个字符`, trigger: 'blur' }   // 长度限制
  ]
}

// 注册表单校验规则（多一个确认密码的自定义校验器 + 用户名正则）
const registerRules = {                                 // 注册表单校验（使用常量 constants/finance.js）
  username: [                                               // 用户名校验规则
    { required: true, message: '请输入用户名', trigger: 'blur' }, // 必填
    { min: USERNAME_MIN_LENGTH, max: USERNAME_MAX_LENGTH, message: `用户名长度为 ${USERNAME_MIN_LENGTH}-${USERNAME_MAX_LENGTH} 个字符`, trigger: 'blur' }, // 长度限制
    { pattern: USERNAME_PATTERN, message: USERNAME_PATTERN_MSG, trigger: 'blur' } // 格式限制
  ],
  password: [                                               // 密码校验规则
    { required: true, message: '请输入密码', trigger: 'blur' },   // 必填
    { min: PASSWORD_MIN_LENGTH, max: PASSWORD_MAX_LENGTH, message: `密码长度为 ${PASSWORD_MIN_LENGTH}-${PASSWORD_MAX_LENGTH} 个字符`, trigger: 'blur' }   // 长度限制
  ],
  confirmPassword: [                                        // 确认密码校验规则
    { required: true, message: '请确认密码', trigger: 'blur' },   // 必填
    {
      validator: (rule, value, callback) => {               // 自定义校验器
        if (value !== registerForm.password) {              // 与密码不一致
          callback(new Error('两次输入的密码不一致'))        // 校验失败
        } else {
          callback()                                        // 校验通过
        }
      },
      trigger: 'blur'                                       // 失焦触发
    }
  ]
}

/**
 * 处理登录
 * → 调用 api/user.js 的 login()
 * 流程：表单校验 → 调用登录接口 → 存 token + 写 userStore → 跳转页面
 */
async function handleLogin() {
  // ★★【答辩第 196 行·第 1 步：前端表单校验】★★
  //  做什么：调 Element Plus 表单的 validate() 方法，检查所有 el-form-item 的 rules 校验规则
  //  .catch(() => false)：validate() 校验失败会 reject Promise，转成 false 避免未捕获异常
  //  await：等待异步校验完成——校验规则里可能有异步 validator
  //  校验规则在上面 loginRules（第 232-243 行）：
  //    username：必填 + 长度 3-20 字符 + 正则只允许字母数字下划线
  //    password：必填 + 长度 ≥6 字符
  //  为什么前端要校验？减少无效请求——用户输错不用等服务器返回，秒级反馈
  //   但安全校验仍以后端为准——前端校验可以被绕过（curl/Postman 直接调 API）
  const valid = await loginFormRef.value.validate().catch(() => false)
  // ★ 做什么：valid 是布尔值（true=校验通过，false=校验失败）
  //  为什么用 return：校验失败直接退出，不发 HTTP 请求——省带宽、省服务器资源
  if (!valid) return

  // ★★【答辩第 206 行】★★
  //  做什么：设置 loading 状态为 true
  //  为什么：el-button 绑定了 :loading="loginLoading"，设为 true 后：
  //    ① 按钮显示转圈动画（用户体验：知道正在处理中）
  //    ② 按钮 disabled 不可点击（防止用户狂点发出几十个重复请求——这叫"防重复提交"）
  loginLoading.value = true

  // ★★【答辩第 210 行】★★
  //  做什么：try 块包裹可能出错的操作
  //  为什么：await login() 有两种失败可能——业务异常（密码错误）和网络异常（断网）
  //    try/catch 统一捕获，防止未处理的 Promise rejection
  try {

    // ★★★【答辩第 231 行·第 2 步：调用登录 API —— 整个全栈链路从这里开始】★★★
    //  做什么：调用 api/user.js 的 login() 函数，发出 POST 请求到 /api/v1/user/login
    //  为什么 await：等待网络请求完成——JavaScript 异步非阻塞，不加 await 会拿到 Promise 对象而非数据
    //
    //  ★ 这一行背后发生的一整套事情（全栈链路 15 个节点）：
    //  【请求出去】① api/user.js login() → request.post('/user/login', data)
    //             ② api/request.js 请求拦截器 → 从 localStorage 读 token 注入 Authorization 头
    //             ③ axios 发送 HTTP POST → Vite Proxy 代理 /api → localhost:8080
    //             ④ 后端 CorsFilter 设置跨域头
    //             ⑤ LoginInterceptor 白名单放行 /api/v1/user/login（用户还没 token）
    //             ⑥ UserController.login() → @Valid 参数校验 → 调 Service
    //             ⑦ UserServiceImpl.login() → 限流→查库→BCrypt→JWT→返回
    //  【响应回来】⑧ Jackson 序列化为 JSON → HTTP 200
    //             ⑨ axios 响应拦截器 code===200 → return data（解出纯业务数据）
    //             ⑩ data = {token: "eyJ...", userId: 1, username: "admin", role: 1}
    //
    //  data 的类型是 {token, userId, username, role}——响应拦截器已经把 Result 外层剥掉了
    const data = await login(loginForm)

    // ★★【答辩第 242 行·第 3 步：存储用户信息】★★
    //  做什么：调用 stores/user.js 的 setUser() 方法
    //  为什么调 store 而不是直接写 localStorage？
    //    setUser() 同时做两件事：
    //    ① 写入 Pinia（Vue 响应式内存）→ 当前页面立刻显示用户名、侧栏菜单根据 role 切换
    //    ② 写入 localStorage（浏览器硬盘）→ 刷新页面后 token 还在，不用重新登录
    //    为什么两个都写？Pinia 管当前会话（响应式），localStorage 管持久化（刷新不丢）
    //  data.username || loginForm.username：优先用后端返回的 username，没有则用表单输入的
    //  data.role || 0：role 默认 0（普通用户），防御性编程防止后端返回 null
    userStore.setUser({ userId: data.userId, username: data.username || loginForm.username, role: data.role || 0, token: data.token })

    // ★ 做什么：Element Plus 绿色成功消息条
    //  为什么：给用户操作成功的即时反馈——符合 UX 设计规范
    ElMessage.success('登录成功')

    // ★★【答辩第 248-274 行·第 4 步：安全跳转】★★
    //  做什么：读取 URL 的 ?redirect= 参数，跳转到登录前访问的页面
    //  为什么有 redirect 参数：路由守卫（router/index.js 第 233 行）拦截未登录请求时，
    //    把目标路径存到 URL 参数里带过来——如 /login?redirect=/account
    //    登录成功后读这个参数跳回去——用户不会丢失原有的访问目标
    const redirect = route.query.redirect || '/'   // 没有 redirect 参数则默认跳首页

    // ★★ 防开放重定向攻击（Open Redirect Attack）★★
    //  做什么：校验 redirect 必须是站内相对路径
    //  为什么：攻击者可能发送 https://xxx.com/login?redirect=https://钓鱼网站.com
    //    用户登录后自动跳转到钓鱼网站 → 看到一模一样的登录页 → 输入密码被偷
    //  校验逻辑（4 个条件）：
    //    typeof === 'string' → 必须是字符串（防止注入对象/数组）
    //    startsWith('/') → 必须以 / 开头（只能是相对路径如 /account）
    //    !includes('://') → 不能包含完整 URL（防止 https://evil.com）
    //    !startsWith('//') → 不能以 // 开头（防止协议相对 URL 如 //evil.com，浏览器会补全为 https://evil.com）
    //  不通过则降级为 '/'（首页）——安全第一
    const safeRedirect = (
      typeof redirect === 'string' &&
      redirect.startsWith('/') &&
      !redirect.includes('://') &&
      !redirect.startsWith('//')
    ) ? redirect : '/'

    // ★ 做什么：Vue Router 编程式导航——跳转到目标页面
    //  为什么用 router.push 而不直接改 window.location：push 是 SPA 内部导航，不刷新页面
    router.push(safeRedirect)

  } catch (e) {
    // ★★【答辩第 279-290 行·异常处理】★★
    //  做什么：记录错误日志
    //  为什么这里只处理网络异常？
    //    业务异常（密码错误/用户不存在等）在 api/request.js 响应拦截器第 120 行
    //    已经用 ElMessage.error 弹过提示了——这里不需要再弹，否则用户看到两条错误
    //    只有网络层异常（ERR_NETWORK=网络断开、ECONNABORTED=请求超时）才需要在这里额外提示
    log.warn('登录失败:', e) /* 开发环境日志 */

    if (e.code === 'ERR_NETWORK' || e.code === 'ECONNABORTED') {
      ElMessage.error('网络异常，登录失败')
    }
  } finally {
    // ★★【答辩第 299 行·finally 块】★★
    //  做什么：无论成功还是失败，关闭 loading 恢复按钮可点击状态
    //  为什么放 finally 而不是在 try 和 catch 各写一遍？
    //    finally 保证一定会执行——即使 try 里 return 了、catch 里抛异常了、甚至有人写了错代码
    //    如果忘记恢复 loading → 按钮永远转圈，用户以为系统卡死（UX 灾难）
    loginLoading.value = false
  }
}

/**
 * 处理注册
 * → 调用 api/user.js 的 register()
 * 流程：表单校验 → 调用注册接口 → 成功后切到登录 Tab 并回填用户名
 */
async function handleRegister() {
  const valid = await registerFormRef.value.validate().catch(() => false) // 触发表单校验
  if (!valid) return                                        // 校验不通过不提交

  registerLoading.value = true                              // 开启注册loading
  try {
    // → 调用 api/user.js 的 register()
    await register({ username: registerForm.username, password: registerForm.password }) // 调用注册API
    ElMessage.success('注册成功，请登录')                     // 成功提示
    // 注册成功后自动切到登录 Tab，并回填刚注册的用户名
    activeTab.value = 'login'                               // 切换到登录Tab
    loginForm.username = registerForm.username               // 回填用户名
    // 清空注册表单
    registerForm.username = ''                               // 清空用户名
    registerForm.password = ''                               // 清空密码
    registerForm.confirmPassword = ''                        // 清空确认密码
  } catch (e) {
    log.warn('注册失败:', e) /* 开发环境日志 */
    if (e.code === 'ERR_NETWORK' || e.code === 'ECONNABORTED') {  // 网络错误或超时
      ElMessage.error('网络异常，注册失败')                    // 网络级错误提示
    }
  } finally {
    registerLoading.value = false                            // 关闭注册loading
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
}

.login-title {
  text-align: center;
  margin-bottom: 20px;
  color: var(--color-title);
  font-size: 20px;
}

.submit-btn {
  width: 100%;
}

/* 无障碍隐藏标签：视觉不显示但屏幕阅读器可识别 */
.hidden-label .el-form-item__label {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
}
</style>
