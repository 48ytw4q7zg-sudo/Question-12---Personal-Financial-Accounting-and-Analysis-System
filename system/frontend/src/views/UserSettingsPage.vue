<!--
  个人设置页面
  路由：/settings
  对应 PRD 功能：P1 个人设置（查看用户信息 + 修改密码）

  功能说明：
    - 用户信息展示（用户名）
    - 修改密码表单（原密码 + 新密码 + 确认密码）

  调用关系：
    → 调用 api/user.js 的 changePassword()（修改密码接口）
    → 调用 stores/user.js 的 username（读取当前用户名显示）
-->
<template>
  <div class="settings-page">
    <h2>设置</h2>

    <!-- 用户信息卡片：展示当前登录用户的基本信息 -->
    <el-card shadow="hover" aria-label="用户信息">
      <template #header>用户信息</template>
      <el-descriptions :column="1" border>
        <!-- → 调用 stores/user.js 的 username -->
        <el-descriptions-item label="用户名">{{ userStore.username }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 修改密码卡片 -->
    <el-card shadow="hover" class="password-card">
      <template #header>修改密码</template>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px" style="max-width: 400px;">
        <el-form-item label="原密码" prop="oldPassword">
          <!-- Q-CR修复: @change触发newPassword重校验，防止旧密码变更后新密码校验状态残留 -->
          <el-input v-model="formData.oldPassword" type="password" placeholder="请输入原密码" show-password @change="() => formRef.validateField('newPassword').catch(() => {})" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <!-- Q-CR修复: @change触发confirmPassword重校验，防止新密码变更后确认密码校验状态残留 -->
          <el-input v-model="formData.newPassword" type="password" placeholder="请输入新密码" show-password @change="() => formRef.validateField('confirmPassword').catch(() => {})" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="formData.confirmPassword" type="password" placeholder="请确认新密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'                         // 导入Vue组合式API
import { useRouter } from 'vue-router'                      // 导入路由
import { ElMessage } from 'element-plus'                     // 导入消息提示
import { PASSWORD_MIN_LENGTH, PASSWORD_MAX_LENGTH } from '../constants/finance' // 导入密码常量
import { logger } from '../utils/logger'                    // 导入日志工具（utils/logger.js）
// → 调用 api/user.js 的 changePassword()（修改密码接口）
import { changePassword } from '../api/user'                 // 导入修改密码API（api/user.js）
// → 调用 stores/user.js 的 username（显示当前用户名）
import { useUserStore } from '../stores/user'                // 导入用户store

const log = logger('UserSettingsPage')                      // 创建日志实例

const router = useRouter()                                  // 路由实例

const userStore = useUserStore()                            // 用户状态store
const submitting = ref(false)    // 提交 loading
const formRef = ref(null)        // 表单引用

// 修改密码表单数据
const formData = reactive({
  oldPassword: '',                                          // 原密码
  newPassword: '',                                          // 新密码
  confirmPassword: ''                                       // 确认新密码
})

// 表单校验规则
const formRules = {
  oldPassword: [                                            // 原密码校验
    { required: true, message: '请输入原密码', trigger: 'blur' } // 必填校验
  ],
  newPassword: [                                            // 新密码校验
    { required: true, message: '请输入新密码', trigger: 'blur' },  // 必填校验
    { min: PASSWORD_MIN_LENGTH, max: PASSWORD_MAX_LENGTH, message: `密码长度为 ${PASSWORD_MIN_LENGTH}-${PASSWORD_MAX_LENGTH} 个字符`, trigger: 'blur' }, // 使用常量 constants/finance.js
    {
      validator: (rule, value, callback) => {               // 自定义校验器
        if (value === formData.oldPassword) {               // 与原密码相同
          callback(new Error('新密码不能与原密码相同'))      // 校验失败
        } else {
          callback()                                        // 校验通过
        }
      },
      trigger: 'blur'                                       // 失焦触发
    }
  ],
  confirmPassword: [                                        // 确认密码校验
    { required: true, message: '请确认新密码', trigger: 'blur' },  // 必填校验
    {
      // 自定义校验器：确认密码必须与新密码一致
      validator: (rule, value, callback) => {               // 自定义校验器
        if (value !== formData.newPassword) {               // 与新密码不一致
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
 * 提交修改密码
 * → 调用 api/user.js 的 changePassword({ oldPassword, newPassword })
 * 成功后清空表单
 */
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false) // 触发表单校验
  if (!valid) return                                        // 校验不通过不提交

  submitting.value = true                                   // 开启提交loading
  try {
    await changePassword({                                  // 调用修改密码API（api/user.js changePassword）
      oldPassword: formData.oldPassword,                    // 原密码参数
      newPassword: formData.newPassword                     // 新密码参数
    })
    ElMessage.success('密码修改成功，请重新登录')             // 成功提示
    // 仅在成功后清空表单（修复：之前 finally 中清空导致失败时也清空表单的 bug）
    formData.oldPassword = ''                                // 清空原密码
    formData.newPassword = ''                                // 清空新密码
    formData.confirmPassword = ''                            // 清空确认密码
  } catch (e) {
    // axios 拦截器已统一处理业务错误消息（如旧密码错误、新旧密码相同等）
    log.warn('修改密码失败:', e) // 开发环境记录日志
    if (e.code === 'ERR_NETWORK' || e.code === 'ECONNABORTED') {  // 网络错误或超时
      ElMessage.error('网络异常，密码修改失败')              // 网络级错误提示
    }
    return;  // Q-CR修复：密码修改失败时直接返回，不执行跳转
  } finally {
    submitting.value = false                                 // 关闭提交loading
  }
  // Q-CR修复：将导航操作移到try-catch外部，避免router.push异常被误报告为"修改密码失败"
  userStore.clearUser()                                      // 清除用户状态（stores/user.js clearUser）
  router.push('/login').catch(() => {})                      // Q-CR修复：catch导航异常，防止未处理 rejection（router/index.js）
}
</script>

<style scoped>
.settings-page h2 {
  margin-bottom: 20px;
  color: var(--color-title);
}

.password-card {
  margin-top: 20px;
}
</style>
