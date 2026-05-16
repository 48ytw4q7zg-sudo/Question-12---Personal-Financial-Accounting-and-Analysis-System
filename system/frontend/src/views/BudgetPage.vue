<template>
  <div class="budget-page">
    <div class="page-header">
      <h2>预算管理</h2>
      <div class="header-actions">
        <el-date-picker
          v-model="selectedMonth"
          type="month"
          placeholder="选择月份"
          value-format="YYYY-MM"
          @change="loadData"
        />
        <el-button type="primary" @click="openDialog()">
          <el-icon><Plus /></el-icon>设置预算
        </el-button>
      </div>
    </div>

    <el-card shadow="hover">
      <el-table :data="budgetProgress" v-loading="loading" stripe>
        <el-table-column prop="categoryName" label="分类" min-width="120" />
        <el-table-column prop="budgetAmount" label="预算金额" width="120">
          <template #default="{ row }">
            ¥ {{ Number(row.budgetAmount || 0).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="spentAmount" label="已支出" width="120">
          <template #default="{ row }">
            ¥ {{ Number(row.spentAmount || 0).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="进度" min-width="200">
          <template #default="{ row }">
            <el-progress
              :percentage="getProgress(row)"
              :color="getProgressColor(row)"
              :stroke-width="18"
            />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.spentAmount > row.budgetAmount" type="danger" size="small">超支</el-tag>
            <el-tag v-else type="success" size="small">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 设置/编辑预算弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑预算' : '设置预算'" width="420px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="formData.categoryId" placeholder="请选择支出分类" style="width: 100%" :disabled="isEdit">
            <el-option
              v-for="cat in expenseCategories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="预算金额" prop="amount">
          <el-input-number v-model="formData.amount" :precision="2" :min="0.01" :step="100" style="width: 100%" />
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
import { ElMessage } from 'element-plus'
import { getBudgetProgress, saveBudget } from '../api/budget'
import { getCategoryList } from '../api/category'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const selectedMonth = ref(new Date().toISOString().substring(0, 7))
const budgetProgress = ref([])
const expenseCategories = ref([])

const formData = reactive({
  categoryId: null,
  amount: null
})

const formRules = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  amount: [{ required: true, message: '请输入预算金额', trigger: 'blur' }]
}

function getProgress(row) {
  if (!row.budgetAmount || row.budgetAmount <= 0) return 0
  const pct = Math.round((row.spentAmount / row.budgetAmount) * 100)
  return Math.min(pct, 100)
}

function getProgressColor(row) {
  const pct = getProgress(row)
  if (pct >= 100) return '#f56c6c'
  if (pct >= 80) return '#e6a23c'
  return '#67c23a'
}

async function loadData() {
  loading.value = true
  try {
    const [year, month] = selectedMonth.value.split('-')
    const data = await getBudgetProgress({ year: Number(year), month: Number(month) })
    budgetProgress.value = data || []
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const data = await getCategoryList()
    expenseCategories.value = (data || []).filter(item => item.type === 1)
  } catch {
    // 静默处理
  }
}

function openDialog(row) {
  isEdit.value = !!row
  if (row) {
    formData.categoryId = row.categoryId
    formData.amount = Number(row.budgetAmount || 0)
  } else {
    formData.categoryId = null
    formData.amount = null
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await saveBudget({
      categoryId: formData.categoryId,
      amount: formData.amount,
      month: selectedMonth.value
    })
    ElMessage.success(isEdit.value ? '更新成功' : '设置成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadCategories()
  loadData()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.page-header h2 {
  margin: 0;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 12px;
}
</style>
