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
    <el-card class="login-card">
      <h2 class="login-title">个人财务记账与分析系统</h2>

      <!-- 登录/注册 Tab 切换 -->
      <el-tabs v-model="activeTab">
        <!-- 登录 Tab -->
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-width="0" @submit.prevent="handleLogin">
            <el-form-item prop="username" label="用户名" class="hidden-label">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password" label="密码" class="hidden-label">
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item>
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
  const valid = await loginFormRef.value.validate().catch(() => false) // 触发表单校验
  if (!valid) return                                        // 校验不通过不提交

  loginLoading.value = true                                 // 开启登录loading
  try {
    // → 调用 api/user.js 的 login()
    const data = await login(loginForm)                      // 调用登录API
    // → 调用 stores/user.js 的 setUser()：集中存储 token + 用户信息（token 写入由 store 统一管理）
    userStore.setUser({ userId: data.userId, username: data.username || loginForm.username, role: data.role || 0, token: data.token }) // 存储用户信息
    ElMessage.success('登录成功')                             // 成功提示
    // 跳转：优先跳 redirect 参数指定的页面（路由守卫带过来的），否则跳首页
    // 安全校验：防止开放重定向攻击，只允许站内相对路径（以 / 开头且不含 :// 和 //）
    const redirect = route.query.redirect || '/'             // 获取重定向路径
    const safeRedirect = (typeof redirect === 'string' && redirect.startsWith('/') && !redirect.includes('://') && !redirect.startsWith('//')) ? redirect : '/' // 安全校验重定向
    router.push(safeRedirect)                                // 跳转到目标页面
  } catch (e) {
    log.warn('登录失败:', e) /* 开发环境日志 */
    // axios 拦截器（api/request.js）已统一处理业务错误消息（密码错误/用户不存在），此处处理网络异常
    if (e.code === 'ERR_NETWORK' || e.code === 'ECONNABORTED') {  // 网络错误或超时
      ElMessage.error('网络异常，登录失败')                    // 网络级错误提示
    }
  } finally {
    loginLoading.value = false                               // 关闭登录loading
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
