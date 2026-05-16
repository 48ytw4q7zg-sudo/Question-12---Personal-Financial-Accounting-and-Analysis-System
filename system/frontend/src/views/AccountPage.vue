<template>
  <div class="account-page">
    <div class="page-header">
      <h2>账户管理</h2>
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon>新增账户
      </el-button>
    </div>

    <el-card shadow="hover">
      <el-table :data="accountList" v-loading="loading" stripe>
        <el-table-column prop="name" label="账户名称" min-width="120" />
        <el-table-column prop="type" label="账户类型" width="100">
          <template #default="{ row }">
            {{ accountTypeMap[row.type] || row.type }}
          </template>
        </el-table-column>
        <el-table-column prop="initialBalance" label="初始余额" width="120">
          <template #default="{ row }">
            ¥ {{ Number(row.initialBalance || 0).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="currency" label="币种" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 余额汇总 -->
    <el-card shadow="hover" class="balance-card" v-if="balanceList.length > 0">
      <template #header>账户余额汇总</template>
      <el-row :gutter="16">
        <el-col v-for="item in balanceList" :key="item.accountId" :xs="12" :sm="6">
          <div class="balance-item">
            <div class="balance-name">{{ item.accountName }}</div>
            <div class="balance-amount">¥ {{ Number(item.currentBalance || 0).toFixed(2) }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑账户' : '新增账户'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="账户名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入账户名称" />
        </el-form-item>
        <el-form-item label="账户类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择账户类型" style="width: 100%">
            <el-option label="现金" :value="1" />
            <el-option label="银行卡" :value="2" />
            <el-option label="支付宝" :value="3" />
            <el-option label="微信" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="初始余额" prop="initialBalance">
          <el-input-number v-model="formData.initialBalance" :precision="2" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="币种" prop="currency">
          <el-input v-model="formData.currency" placeholder="CNY" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAccountList, createAccount, updateAccount, deleteAccount, getAccountBalance } from '../api/account'

const accountTypeMap = { 1: '现金', 2: '银行卡', 3: '支付宝', 4: '微信' }

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)

const accountList = ref([])
const balanceList = ref([])

const formData = reactive({
  name: '',
  type: null,
  initialBalance: 0,
  currency: 'CNY'
})

const formRules = {
  name: [{ required: true, message: '请输入账户名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择账户类型', trigger: 'change' }]
}

function formatTime(time) {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 19)
}

async function loadAccounts() {
  loading.value = true
  try {
    const data = await getAccountList()
    accountList.value = data || []
  } finally {
    loading.value = false
  }
}

async function loadBalance() {
  try {
    const data = await getAccountBalance()
    balanceList.value = data || []
  } catch {
    balanceList.value = []
  }
}

function openDialog(row) {
  isEdit.value = !!row
  editId.value = row?.id || null
  if (row) {
    formData.name = row.name
    formData.type = row.type
    formData.initialBalance = Number(row.initialBalance || 0)
    formData.currency = row.currency || 'CNY'
  } else {
    formData.name = ''
    formData.type = null
    formData.initialBalance = 0
    formData.currency = 'CNY'
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateAccount(editId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await createAccount(formData)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadAccounts()
    loadBalance()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除该账户吗？', '提示', { type: 'warning' })
  await deleteAccount(row.id)
  ElMessage.success('删除成功')
  loadAccounts()
  loadBalance()
}

onMounted(() => {
  loadAccounts()
  loadBalance()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  color: #303133;
}

.balance-card {
  margin-top: 20px;
}

.balance-item {
  text-align: center;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.balance-name {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.balance-amount {
  font-size: 18px;
  font-weight: bold;
  color: #409eff;
}
</style>
