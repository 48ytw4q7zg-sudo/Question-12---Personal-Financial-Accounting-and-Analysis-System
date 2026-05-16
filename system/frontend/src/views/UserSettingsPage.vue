<template>
  <div class="settings-page">
    <h2>设置</h2>

    <el-card shadow="hover">
      <template #header>用户信息</template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户名">{{ userStore.username }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

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
import { changePassword } from '../api/user'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const submitting = ref(false)
const formRef = ref(null)

const formData = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

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
