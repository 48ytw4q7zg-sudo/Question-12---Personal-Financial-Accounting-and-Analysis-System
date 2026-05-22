<!--
  转账页面
  路由：/transfer
  对应 PRD 功能：P1 转账（在两个账户间转移资金）

  功能说明：
    - 转账表单：选择转出账户、转入账户（互斥，不能选同一个）、输入金额、备注
    - 提交后后端自动创建一对关联的收支记录（转出方支出 + 转入方收入）

  调用关系：
    → 调用 api/transaction.js 的 transfer()（转账接口）
    → 调用 api/account.js 的 getAccountList()（加载账户下拉选项）
-->
<template>
  <div class="transfer-page">
    <h2>转账</h2>

    <el-card shadow="hover" class="transfer-card" v-loading="loading" aria-label="转账表单">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px" style="max-width: 500px; margin: 0 auto;">
        <el-form-item label="转出账户" prop="fromAccountId">
          <el-select v-model="formData.fromAccountId" placeholder="请选择转出账户" style="width: 100%">
            <el-option
              v-for="acc in accountList"
              :key="acc.id"
              :label="acc.name"
              :value="acc.id"
              :disabled="acc.id === formData.toAccountId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="转入账户" prop="toAccountId">
          <el-select v-model="formData.toAccountId" placeholder="请选择转入账户" style="width: 100%">
            <el-option
              v-for="acc in accountList"
              :key="acc.id"
              :label="acc.name"
              :value="acc.id"
              :disabled="acc.id === formData.fromAccountId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="formData.amount" :precision="2" :min="0.01" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="note">
          <el-input v-model="formData.note" type="textarea" placeholder="备注（可选）" :rows="3" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit" style="width: 100%;">确认转账</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { transfer } from '../api/transaction'
import { getAccountList } from '../api/account'

const loading = ref(false)          // 初始加载状态
const submitting = ref(false)       // 提交 loading
const formRef = ref(null)
const accountList = ref([])

const formData = reactive({
  fromAccountId: null,
  toAccountId: null,
  amount: null,
  note: ''
})

const formRules = {
  fromAccountId: [{ required: true, message: '请选择转出账户', trigger: 'change' }],
  toAccountId: [
    { required: true, message: '请选择转入账户', trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        if (value && value === formData.fromAccountId) {
          callback(new Error('转入账户不能与转出账户相同'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  amount: [
    { required: true, message: '请输入转账金额', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '转账金额必须大于0', trigger: 'blur' }
  ],
  note: [{ max: 200, message: '备注长度不能超过200', trigger: 'blur' }]
}

async function loadAccounts() {
  loading.value = true
  try {
    const data = await getAccountList()
    accountList.value = data || []
  } catch (e) {
    // axios 拦截器已统一处理业务错误消息，此处额外做本地降级提示（账户下拉为空时用户需手动刷新）
    if (import.meta.env.DEV) console.warn('加载账户列表失败:', e)
    ElMessage.warning('加载账户列表失败，请刷新重试')
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await transfer(formData)
    ElMessage.success('转账成功')
    formRef.value.resetFields()
    // 转账后刷新账户列表（余额可能已变化）
    await loadAccounts()
  } catch (e) {
    // axios 拦截器已统一处理业务错误消息（如余额不足、账户禁用等），此处仅记录非业务异常
    if (e && e.message && !e.message.includes('业务')) console.error('转账异常:', e)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadAccounts()
})
</script>

<style scoped>
.transfer-page h2 {
  margin-bottom: 20px;
  color: var(--color-title);
}

.transfer-card {
  max-width: 600px;
  margin: 0 auto;
}
</style>
