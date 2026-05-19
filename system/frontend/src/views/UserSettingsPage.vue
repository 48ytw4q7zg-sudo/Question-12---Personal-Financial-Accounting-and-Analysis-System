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
    <el-card shadow="hover">
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
          <el-input v-model="formData.oldPassword" type="password" placeholder="请输入原密码" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="formData.newPassword" type="password" placeholder="请输入新密码" show-password />
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
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
// → 调用 api/user.js 的 changePassword()（修改密码接口）
import { changePassword } from '../api/user'
// → 调用 stores/user.js 的 username（显示当前用户名）
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const submitting = ref(false)    // 提交 loading
const formRef = ref(null)        // 表单引用

// 修改密码表单数据
const formData = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 表单校验规则
const formRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      // 自定义校验器：确认密码必须与新密码一致
      validator: (rule, value, callback) => {
        if (value !== formData.newPassword) {
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
 * 提交修改密码
 * → 调用 api/user.js 的 changePassword({ oldPassword, newPassword })
 * 成功后清空表单
 */
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await changePassword({
      oldPassword: formData.oldPassword,
      newPassword: formData.newPassword
    })
    ElMessage.success('密码修改成功')
    // 清空表单
    formData.oldPassword = ''
    formData.newPassword = ''
    formData.confirmPassword = ''
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.settings-page h2 {
  margin-bottom: 20px;
  color: #303133;
}

.password-card {
  margin-top: 20px;
}
</style>
