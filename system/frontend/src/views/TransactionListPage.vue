<!--
  收支记录页面
  路由：/transaction
  对应 PRD 功能：
    - P0 收支记录（记一笔 + 改 + 列表分页）
    - P1 多条件筛选（时间/账户/分类/关键词）

  功能说明：
    - 顶部筛选栏：日期范围 + 账户 + 分类 + 关键词搜索
    - 交易记录表格：时间/账户/分类/类型/金额/备注/转账标识/操作
    - 分页组件（pageNum + pageSize）
    - 「记一笔」新增弹窗 + 编辑弹窗
    - 筛选条件持久化到 URL query params（支持浏览器前进后退）

  调用关系：
    → 调用 api/transaction.js 的 getTransactionList()（加载交易列表，支持筛选+分页）
    → 调用 api/transaction.js 的 createTransaction()（记一笔）
    → 调用 api/transaction.js 的 updateTransaction()（编辑记录）
    → 调用 api/transaction.js 的 deleteTransaction()（删除记录，转账记录不显示删除按钮）
    → 调用 api/account.js 的 getAccountList()（加载账户下拉选项）
    → 调用 api/category.js 的 getCategoryList()（加载分类下拉选项）
-->
<template>
  <div class="transaction-page">
    <div class="page-header">
      <h2>收支记录</h2>
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon>记一笔
      </el-button>
    </div>

    <!-- 筛选栏（PRD P1-1: 多条件筛选） -->
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

    <!-- 交易记录列表 -->
    <el-card shadow="hover">
      <el-table :data="transactionList" v-loading="loading" stripe>
        <el-table-column prop="time" label="时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.time) }}
          </template>
        </el-table-column>
        <el-table-column prop="accountName" label="账户" width="120" />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <!-- 类型标签：1=收入(绿色), 2=支出(红色) -->
        <el-table-column prop="type" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === 1 ? 'success' : 'danger'" size="small">
              {{ row.type === 1 ? '收入' : '支出' }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 金额：收入显示+，支出显示- -->
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">
            <span :class="row.type === 1 ? 'amount-income' : 'amount-expense'">
              {{ row.type === 1 ? '+' : '-' }}¥ {{ formatAmount(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="note" label="备注" min-width="150" show-overflow-tooltip />
        <!-- 转账标识：有 transferId 的记录标记为「转账」 -->
        <el-table-column label="转账标识" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.transferId" type="warning" size="small">转账</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-button
              v-if="!row.transferId"
              type="danger"
              link
              @click="handleDelete(row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件（对齐 API_DESIGN.md §1 分页参数 pageNum + pageSize） -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 记一笔/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑记录' : '记一笔'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="账户" prop="accountId">
          <!-- 编辑转账记录时禁用账户/分类/类型/金额/时间，防止破坏转账关联 -->
          <el-select v-model="formData.accountId" placeholder="请选择账户" style="width: 100%" :disabled="isEdit && isTransfer">
            <el-option v-for="acc in accountList" :key="acc.id" :label="acc.name" :value="acc.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <!-- 根据当前选择的类型动态过滤可选分类 -->
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
import { ElMessage, ElMessageBox } from 'element-plus'
// → 调用 api/transaction.js 的 4 个接口
import { getTransactionList, createTransaction, updateTransaction, deleteTransaction } from '../api/transaction'
// → 调用 api/account.js 的 getAccountList()（下拉选项）
import { getAccountList } from '../api/account'
// → 调用 api/category.js 的 getCategoryList()（下拉选项）
import { getCategoryList } from '../api/category'
import { formatTime, formatDateTime, formatAmount } from '../utils/format'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)           // 是否编辑模式
const editId = ref(null)            // 编辑的记录 ID
const isTransfer = ref(false)       // 当前编辑的记录是否为转账记录（转账记录禁用部分字段修改）
const formRef = ref(null)

const transactionList = ref([])     // 交易记录列表
const accountList = ref([])         // 账户下拉选项
const categoryList = ref([])        // 分类下拉选项

// 筛选条件（PRD P1-1 多条件筛选）
const filters = reactive({
  dateRange: null,      // 日期范围 [startDate, endDate]
  accountId: null,      // 按账户筛选
  categoryId: null,     // 按分类筛选
  keyword: ''           // 关键词搜索（备注）
})

// 分页参数
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// 新增/编辑表单数据
const formData = reactive({
  accountId: null,
  categoryId: null,
  type: 2,        // 1=收入, 2=支出（默认支出）
  amount: null,
  note: '',
  time: ''
})

// 表单校验规则
const formRules = {
  accountId: [{ required: true, message: '请选择账户', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  time: [{ required: true, message: '请选择时间', trigger: 'change' }]
}

/**
 * 根据当前选择的类型动态过滤可选分类
 * 类型映射关系：
 *   category.type: 1=支出, 2=收入
 *   transaction.type: 1=收入, 2=支出
 *   选支出(2) → 显示 category.type=1 的分类
 *   选收入(1) → 显示 category.type=2 的分类
 */
const filteredCategories = computed(() => {
  const categoryType = formData.type === 2 ? 1 : 2
  return categoryList.value.filter(item => item.type === categoryType)
})

/** 分页 pageSize 变化处理（重置到第1页并加载） */
function handleSizeChange(size) {
  pagination.pageSize = size
  pagination.pageNum = 1
  loadTransactions()
}

/** 分页页码变化处理 */
function handlePageChange(page) {
  pagination.pageNum = page
  loadTransactions()
}

/**
 * 加载交易记录列表（支持筛选 + 分页）
 * → 调用 api/transaction.js 的 getTransactionList(params)
 */
async function loadTransactions() {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    }
    // 拼接筛选参数
    if (filters.dateRange) {
      params.startTime = filters.dateRange[0] + ' 00:00:00'
      params.endTime = filters.dateRange[1] + ' 23:59:59'
    }
    if (filters.accountId) params.accountId = filters.accountId
    if (filters.categoryId) params.categoryId = filters.categoryId
    if (filters.keyword) params.keyword = filters.keyword

    // → 调用 api/transaction.js 的 getTransactionList(params)
    const data = await getTransactionList(params)
    // 兼容后端分页返回结构（records/list/直接数组）
    transactionList.value = data?.records || data?.list || data || []
    pagination.total = data?.total || 0
  } finally {
    loading.value = false
  }
}

/**
 * 加载下拉选项数据（账户 + 分类）
 * → 调用 api/account.js 的 getAccountList() + api/category.js 的 getCategoryList()
 * 并行请求提高加载速度
 */
async function loadOptions() {
  try {
    const [accounts, categories] = await Promise.all([getAccountList(), getCategoryList()])
    accountList.value = accounts || []
    categoryList.value = categories || []
  } catch (e) {
    console.warn('加载选项数据失败:', e)
  }
}

/**
 * 同步筛选条件到 URL query params（PRD P1-1）
 * 支持浏览器前进后退保留筛选状态
 */
function syncFiltersToUrl() {
  const q = {}
  if (filters.dateRange) { q.startDate = filters.dateRange[0]; q.endDate = filters.dateRange[1] }
  if (filters.accountId) q.accountId = filters.accountId
  if (filters.categoryId) q.categoryId = filters.categoryId
  if (filters.keyword) q.keyword = filters.keyword
  if (pagination.pageNum > 1) q.page = pagination.pageNum
  router.replace({ query: q })
}

/** 从 URL query params 读取筛选条件（页面初始化时调用） */
function readFiltersFromUrl() {
  const q = route.query
  if (q.startDate && q.endDate) filters.dateRange = [q.startDate, q.endDate]
  if (q.accountId) filters.accountId = Number(q.accountId)
  if (q.categoryId) filters.categoryId = Number(q.categoryId)
  if (q.keyword) filters.keyword = q.keyword
  if (q.page) pagination.pageNum = Number(q.page)
}

/** 搜索：重置页码到第 1 页 + 同步 URL + 重新加载 */
function handleSearch() {
  pagination.pageNum = 1
  syncFiltersToUrl()
  loadTransactions()
}

/** 重置筛选条件 + 清空 URL query params */
function resetFilters() {
  filters.dateRange = null
  filters.accountId = null
  filters.categoryId = null
  filters.keyword = ''
  router.replace({ query: {} })
  handleSearch()
}

/**
 * 打开新增/编辑弹窗
 * @param {Object|null} row - 传入行数据为编辑模式，不传为「记一笔」新增模式
 */
function openDialog(row) {
  isEdit.value = !!row
  editId.value = row?.id || null
  isTransfer.value = !!row?.transferId    // 判断是否为转账记录
  if (row) {
    // 编辑模式：回填表单
    formData.accountId = row.accountId
    formData.categoryId = row.categoryId
    formData.type = row.type
    formData.amount = Number(row.amount || 0)
    formData.note = row.note || ''
    formData.time = row.time || ''
  } else {
    // 新增模式：重置表单，默认时间设为当前
    formData.accountId = null
    formData.categoryId = null
    formData.type = 2
    formData.amount = null
    formData.note = ''
    formData.time = formatDateTime(new Date())
  }
  dialogVisible.value = true
}

/**
 * 删除交易记录
 * → 调用 api/transaction.js 的 deleteTransaction(id)
 * 转账记录不显示删除按钮（已在模板中 v-if="!row.transferId" 隐藏）
 */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除这条${row.type === 1 ? '收入' : '支出'}记录吗？`, '确认删除', {
      type: 'warning'
    })
    await deleteTransaction(row.id)
    ElMessage.success('记录已删除')
    loadTransactions()
  } catch {
    // 用户取消删除，静默处理
  }
}

/**
 * 提交表单（记一笔或编辑）
 * → 调用 api/transaction.js 的 createTransaction() 或 updateTransaction()
 */
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      // → 调用 api/transaction.js 的 updateTransaction(id, data)
      await updateTransaction(editId.value, formData)
      ElMessage.success('更新成功')
    } else {
      // → 调用 api/transaction.js 的 createTransaction(data)
      await createTransaction(formData)
      ElMessage.success('记账成功')
    }
    dialogVisible.value = false
    loadTransactions()
  } finally {
    submitting.value = false
  }
}

// 页面挂载时：从 URL 读取筛选条件 → 加载下拉选项 → 加载交易列表
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
