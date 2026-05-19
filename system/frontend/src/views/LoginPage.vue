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
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" @submit.prevent="handleLogin">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loginLoading" class="submit-btn" @click="handleLogin">登录</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 注册 Tab -->
        <el-tab-pane label="注册" name="register">
          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" @submit.prevent="handleRegister">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="请输入用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请确认密码" prefix-icon="Lock" show-password />
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
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
// → 调用 api/user.js 的 login() 和 register()
import { login, register } from '../api/user'
// → 调用 stores/user.js 的 setUser()
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 当前激活的 Tab：'login' 或 'register'
const activeTab = ref('login')
const loginLoading = ref(false)       // 登录按钮 loading 防重复提交
const registerLoading = ref(false)    // 注册按钮 loading 防重复提交
const loginFormRef = ref(null)        // 登录表单引用（用于手动触发表单校验）
const registerFormRef = ref(null)     // 注册表单引用

// 登录表单数据
const loginForm = reactive({
  username: '',
  password: ''
})

// 注册表单数据（多一个确认密码字段）
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: ''
})

// 登录表单校验规则（el-form rules）
const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3-20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 个字符', trigger: 'blur' }
  ]
}

// 注册表单校验规则（多一个确认密码的自定义校验器）
const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3-20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

/**
 * 处理登录
 * → 调用 api/user.js 的 login()
 * 流程：表单校验 → 调用登录接口 → 存 token + 写 userStore → 跳转页面
 */
async function handleLogin() {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return

  loginLoading.value = true
  try {
    // → 调用 api/user.js 的 login()
    const data = await login(loginForm)
    // 登录成功：存储 JWT token 到 localStorage（→ api/request.js 请求拦截器读取此 token）
    localStorage.setItem('token', data.token)
    // → 调用 stores/user.js 的 setUser()：存储用户信息到 Pinia 状态
    userStore.setUser({ userId: data.userId, username: data.username || loginForm.username })
    ElMessage.success('登录成功')
    // 跳转：优先跳 redirect 参数指定的页面（路由守卫带过来的），否则跳首页
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } finally {
    loginLoading.value = false
  }
}

/**
 * 处理注册
 * → 调用 api/user.js 的 register()
 * 流程：表单校验 → 调用注册接口 → 成功后切到登录 Tab 并回填用户名
 */
async function handleRegister() {
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return

  registerLoading.value = true
  try {
    // → 调用 api/user.js 的 register()
    await register({ username: registerForm.username, password: registerForm.password })
    ElMessage.success('注册成功，请登录')
    // 注册成功后自动切到登录 Tab，并回填刚注册的用户名
    activeTab.value = 'login'
    loginForm.username = registerForm.username
    // 清空注册表单
    registerForm.username = ''
    registerForm.password = ''
    registerForm.confirmPassword = ''
  } finally {
    registerLoading.value = false
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
  color: #303133;
  font-size: 20px;
}

.submit-btn {
  width: 100%;
}
</style>
