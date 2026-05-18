<template>
  <div class="transaction-page">
    <div class="page-header">
      <h2>收支记录</h2>
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon>记一笔
      </el-button>
    </div>

    <!-- 筛选栏 -->
    <el-card shadow="hover" class="filter-card">
      <el-form :inline="true" :model="filters">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item label="账户">
          <el-select v-model="filters.accountId" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="acc in accountList" :key="acc.id" :label="acc.name" :value="acc.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="filters.categoryId" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="cat in categoryList" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="备注搜索" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="hover">
      <el-table :data="transactionList" v-loading="loading" stripe>
        <el-table-column prop="time" label="时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.time) }}
          </template>
        </el-table-column>
        <el-table-column prop="accountName" label="账户" width="120" />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column prop="type" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === 1 ? 'success' : 'danger'" size="small">
              {{ row.type === 1 ? '收入' : '支出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">
            <span :class="row.type === 1 ? 'amount-income' : 'amount-expense'">
              {{ row.type === 1 ? '+' : '-' }}¥ {{ Number(row.amount || 0).toFixed(2) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="note" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column label="转账标识" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.transferId" type="warning" size="small">转账</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadTransactions"
          @current-change="loadTransactions"
        />
      </div>
    </el-card>

    <!-- 记一笔/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑记录' : '记一笔'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="账户" prop="accountId">
          <el-select v-model="formData.accountId" placeholder="请选择账户" style="width: 100%" :disabled="isEdit && isTransfer">
            <el-option v-for="acc in accountList" :key="acc.id" :label="acc.name" :value="acc.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="formData.categoryId" placeholder="请选择分类" style="width: 100%" :disabled="isEdit && isTransfer">
            <el-option v-for="cat in filteredCategories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="formData.type" :disabled="isEdit && isTransfer">
            <el-radio :value="2">支出</el-radio>
            <el-radio :value="1">收入</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="formData.amount" :precision="2" :min="0.01" :step="1" style="width: 100%" :disabled="isEdit && isTransfer" />
        </el-form-item>
        <el-form-item label="备注" prop="note">
          <el-input v-model="formData.note" type="textarea" placeholder="备注（可选）" />
        </el-form-item>
        <el-form-item label="时间" prop="time">
          <el-date-picker v-model="formData.time" type="datetime" placeholder="选择时间" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" :disabled="isEdit && isTransfer" />
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
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTransactionList, createTransaction, updateTransaction } from '../api/transaction'
import { getAccountList } from '../api/account'
import { getCategoryList } from '../api/category'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const isTransfer = ref(false)
const formRef = ref(null)

const transactionList = ref([])
const accountList = ref([])
const categoryList = ref([])

const filters = reactive({
  dateRange: null,
  accountId: null,
  categoryId: null,
  keyword: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const formData = reactive({
  accountId: null,
  categoryId: null,
  type: 2, // 1=收入, 2=支出
  amount: null,
  note: '',
  time: ''
})

const formRules = {
  accountId: [{ required: true, message: '请选择账户', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  time: [{ required: true, message: '请选择时间', trigger: 'change' }]
}

const filteredCategories = computed(() => {
  // category.type: 1=支出, 2=收入
  // transaction.type: 1=收入, 2=支出
  // 选支出(2) → 显示 category.type=1 的分类
  // 选收入(1) → 显示 category.type=2 的分类
  const categoryType = formData.type === 2 ? 1 : 2
  return categoryList.value.filter(item => item.type === categoryType)
})

function formatTime(time) {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 19)
}

function formatDateTime(date) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

async function loadTransactions() {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    }
    if (filters.dateRange) {
      params.startTime = filters.dateRange[0] + ' 00:00:00'
      params.endTime = filters.dateRange[1] + ' 23:59:59'
    }
    if (filters.accountId) params.accountId = filters.accountId
    if (filters.categoryId) params.categoryId = filters.categoryId
    if (filters.keyword) params.keyword = filters.keyword

    const data = await getTransactionList(params)
    transactionList.value = data?.records || data?.list || data || []
    pagination.total = data?.total || 0
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

// PRD P1-1: 筛选条件持久化到 URL query params
function syncFiltersToUrl() {
  const q = {}
  if (filters.dateRange) { q.startDate = filters.dateRange[0]; q.endDate = filters.dateRange[1] }
  if (filters.accountId) q.accountId = filters.accountId
  if (filters.categoryId) q.categoryId = filters.categoryId
  if (filters.keyword) q.keyword = filters.keyword
  if (pagination.pageNum > 1) q.page = pagination.pageNum
  router.replace({ query: q })
}

function readFiltersFromUrl() {
  const q = route.query
  if (q.startDate && q.endDate) filters.dateRange = [q.startDate, q.endDate]
  if (q.accountId) filters.accountId = Number(q.accountId)
  if (q.categoryId) filters.categoryId = Number(q.categoryId)
  if (q.keyword) filters.keyword = q.keyword
  if (q.page) pagination.pageNum = Number(q.page)
}

function handleSearch() {
  pagination.pageNum = 1
  syncFiltersToUrl()
  loadTransactions()
}

function resetFilters() {
  filters.dateRange = null
  filters.accountId = null
  filters.categoryId = null
  filters.keyword = ''
  router.replace({ query: {} })
  handleSearch()
}

function openDialog(row) {
  isEdit.value = !!row
  editId.value = row?.id || null
  isTransfer.value = !!row?.transferId
  if (row) {
    formData.accountId = row.accountId
    formData.categoryId = row.categoryId
    formData.type = row.type
    formData.amount = Number(row.amount || 0)
    formData.note = row.note || ''
    formData.time = row.time || ''
  } else {
    formData.accountId = null
    formData.categoryId = null
    formData.type = 2
    formData.amount = null
    formData.note = ''
    formData.time = formatDateTime(new Date())
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateTransaction(editId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await createTransaction(formData)
      ElMessage.success('记账成功')
    }
    dialogVisible.value = false
    loadTransactions()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  readFiltersFromUrl();
  loadOptions()
  loadTransactions()
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

.filter-card {
  margin-bottom: 20px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
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
