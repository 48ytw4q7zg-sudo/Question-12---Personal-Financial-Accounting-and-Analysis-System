<template>
  <div class="recurring-bill-page">
    <div class="page-header">
      <h2>周期账单</h2>
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon>新增周期账单
      </el-button>
    </div>

    <el-card shadow="hover">
      <el-table :data="billList" v-loading="loading" stripe>
        <el-table-column prop="name" label="名称" min-width="120" />
        <el-table-column prop="accountName" label="账户" width="100" />
        <el-table-column prop="categoryName" label="分类" width="100" />
        <el-table-column prop="amount" label="金额" width="110">
          <template #default="{ row }">
            <span :class="row.type === 1 ? 'amount-income' : 'amount-expense'">
              {{ row.type === 1 ? '+' : '-' }}¥ {{ Number(row.amount || 0).toFixed(2) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === 1 ? 'success' : 'danger'" size="small">
              {{ row.type === 1 ? '收入' : '支出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="period" label="周期" width="100">
          <template #default="{ row }">
            {{ periodMap[row.period] || row.period }}
          </template>
        </el-table-column>
        <el-table-column prop="nextDueDate" label="下次到期" width="120">
          <template #default="{ row }">
            {{ formatDate(row.nextDueDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-button type="warning" link @click="handleDeactivate(row)">停用</el-button>
            <el-button type="success" link @click="handleGenerate(row)">生成</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑周期账单' : '新增周期账单'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" placeholder="如：房租、水电费" />
        </el-form-item>
        <el-form-item label="账户" prop="accountId">
          <el-select v-model="formData.accountId" placeholder="请选择账户" style="width: 100%">
            <el-option v-for="acc in accountList" :key="acc.id" :label="acc.name" :value="acc.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="formData.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="cat in categoryList" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="formData.amount" :precision="2" :min="0.01" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="formData.type">
            <el-radio :value="2">支出</el-radio>
            <el-radio :value="1">收入</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="周期" prop="period">
          <el-select v-model="formData.period" placeholder="请选择周期" style="width: 100%">
            <el-option label="每月" value="monthly" />
            <el-option label="每周" value="weekly" />
          </el-select>
        </el-form-item>
        <el-form-item label="下次到期" prop="nextDueDate">
          <el-date-picker v-model="formData.nextDueDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
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
import {
  getRecurringBillList,
  createRecurringBill,
  updateRecurringBill,
  deleteRecurringBill,
  generateRecurringBill
} from '../api/recurring-bill'
import { getAccountList } from '../api/account'
import { getCategoryList } from '../api/category'

const periodMap = { monthly: '每月', weekly: '每周' }

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)

const billList = ref([])
const accountList = ref([])
const categoryList = ref([])

const formData = reactive({
  name: '',
  accountId: null,
  categoryId: null,
  amount: null,
  type: 2,
  period: 'monthly',
  nextDueDate: ''
})

const formRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  accountId: [{ required: true, message: '请选择账户', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  period: [{ required: true, message: '请选择周期', trigger: 'change' }],
  nextDueDate: [{ required: true, message: '请选择下次到期日期', trigger: 'change' }]
}

function formatDate(date) {
  if (!date) return ''
  return date.substring(0, 10)
}

async function loadBills() {
  loading.value = true
  try {
    const data = await getRecurringBillList()
    billList.value = data || []
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  try {
    const [accounts, categories] = await Promise.all([getAccountList(), getCategoryList()])
    accountList.value = accounts || []
    categoryList.value = categories || []
  } catch {
    // 静默处理
  }
}

function openDialog(row) {
  isEdit.value = !!row
  editId.value = row?.id || null
  if (row) {
    formData.name = row.name
    formData.accountId = row.accountId
    formData.categoryId = row.categoryId
    formData.amount = Number(row.amount || 0)
    formData.type = row.type
    formData.period = row.period
    formData.nextDueDate = row.nextDueDate?.substring(0, 10) || ''
  } else {
    formData.name = ''
    formData.accountId = null
    formData.categoryId = null
    formData.amount = null
    formData.type = 2
    formData.period = 'monthly'
    formData.nextDueDate = ''
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateRecurringBill(editId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await createRecurringBill(formData)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadBills()
  } finally {
    submitting.value = false
  }
}

async function handleDeactivate(row) {
  await ElMessageBox.confirm('确定停用该周期账单吗？', '提示', { type: 'warning' }).catch(() => { return })
  await deleteRecurringBill(row.id)
  ElMessage.success('已停用')
  loadBills()
}

async function handleGenerate(row) {
  await ElMessageBox.confirm('确定生成该周期账单的交易记录吗？', '提示', { type: 'info' }).catch(() => { return })
  await generateRecurringBill(row.id)
  ElMessage.success('生成成功')
  loadBills()
}

onMounted(() => {
  loadOptions()
  loadBills()
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

.amount-income {
  color: #67c23a;
  font-weight: bold;
}

.amount-expense {
  color: #f56c6c;
  font-weight: bold;
}
</style>
