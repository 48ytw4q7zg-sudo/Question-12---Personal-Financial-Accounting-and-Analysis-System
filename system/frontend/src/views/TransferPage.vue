<template>
  <div class="transfer-page">
    <h2>转账</h2>

    <el-card shadow="hover" class="transfer-card">
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

const submitting = ref(false)
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
  toAccountId: [{ required: true, message: '请选择转入账户', trigger: 'change' }],
  amount: [{ required: true, message: '请输入转账金额', trigger: 'blur' }]
}

async function loadAccounts() {
  try {
    const data = await getAccountList()
    accountList.value = data || []
  } catch {
    // 静默处理
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await transfer(formData)
    ElMessage.success('转账成功')
    formData.fromAccountId = null
    formData.toAccountId = null
    formData.amount = null
    formData.note = ''
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
  color: #303133;
}

.transfer-card {
  max-width: 600px;
  margin: 0 auto;
}
</style>
