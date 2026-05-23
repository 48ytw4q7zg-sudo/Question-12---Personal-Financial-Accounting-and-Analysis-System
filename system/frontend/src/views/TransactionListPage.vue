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
    <el-card shadow="hover" class="filter-card" role="search" aria-label="筛选条件">
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
        <template #empty><el-empty description="暂无收支记录" /></template>
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
            <el-tag :type="row.type === TRANSACTION_TYPE_INCOME ? 'success' : 'danger'" size="small">
              {{ TRANSACTION_TYPE_MAP[row.type]?.label || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 金额：收入显示+，支出显示- -->
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">
            <span :class="row.type === TRANSACTION_TYPE_INCOME ? 'amount-income' : 'amount-expense'">
              {{ TRANSACTION_TYPE_MAP[row.type]?.sign || '' }}¥ {{ formatAmount(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="note" label="备注" min-width="150" show-overflow-tooltip />
        <!-- 转账标识：有 transferId 的记录标记为「转账」 -->
        <el-table-column label="转账标识" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.transferId && row.type === TRANSACTION_TYPE_EXPENSE" type="warning" size="small">(转出)</el-tag>
            <el-tag v-if="row.transferId && row.type === TRANSACTION_TYPE_INCOME" type="success" size="small">(转入)</el-tag>
            <!-- 兜底条件：type 只能是 1(收入) 或 2(支出)，此分支理论上不会触发，保留以防御未来枚举扩展 -->
            <el-tag v-if="row.transferId && row.type !== TRANSACTION_TYPE_EXPENSE && row.type !== TRANSACTION_TYPE_INCOME" type="warning" size="small">转账</el-tag>
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
              :disabled="deletingId === row.id"
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
            <el-radio :value="TRANSACTION_TYPE_EXPENSE">支出</el-radio>
            <el-radio :value="TRANSACTION_TYPE_INCOME">收入</el-radio>
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
import { ref, reactive, computed, onMounted, watch } from 'vue' // 导入Vue组合式API
import { useRoute, useRouter } from 'vue-router'            // 导入路由
import { ElMessage, ElMessageBox } from 'element-plus'      // 导入消息和确认框
// → 调用 api/transaction.js 的 4 个接口
import { getTransactionList, createTransaction, updateTransaction, deleteTransaction } from '../api/transaction' // 导入交易API
// → 调用 api/account.js 的 getAccountList()（下拉选项）
import { getAccountList } from '../api/account'              // 导入账户API
// → 调用 api/category.js 的 getCategoryList()（下拉选项）
import { getCategoryList } from '../api/category'             // 导入分类API
import { formatTime, formatDateTime, formatAmount } from '../utils/format' // 导入格式化工具
import { TRANSACTION_TYPE_MAP, TRANSACTION_TYPE_INCOME, TRANSACTION_TYPE_EXPENSE, CATEGORY_TYPE_EXPENSE, CATEGORY_TYPE_INCOME } from '../constants/finance' // 导入常量
import { logger } from '../utils/logger' // 导入日志工具

const log = logger('TransactionListPage') // 创建日志实例

const route = useRoute()                                    // 当前路由信息
const router = useRouter()                                  // 路由实例

const loading = ref(false)                                  // 页面loading
const submitting = ref(false)                               // 提交loading
const deletingId = ref(null)                                // 正在删除的记录ID
const dialogVisible = ref(false)                            // 弹窗显隐
const isEdit = ref(false)           // 是否编辑模式
const editId = ref(null)            // 编辑的记录 ID
const isTransfer = ref(false)       // 当前编辑的记录是否为转账记录（转账记录禁用部分字段修改）
const formRef = ref(null)                                    // 表单引用

const transactionList = ref([])     // 交易记录列表
const accountList = ref([])         // 账户下拉选项
const categoryList = ref([])        // 分类下拉选项

// 筛选条件（PRD P1-1 多条件筛选）
const TIME_START_OF_DAY = ' 00:00:00'
const TIME_END_OF_DAY = ' 23:59:59'

const filters = reactive({
  dateRange: null,      // 日期范围 [startDate, endDate]
  accountId: null,      // 按账户筛选
  categoryId: null,     // 按分类筛选
  keyword: ''           // 关键词搜索（备注）
})

// 分页参数
const pagination = reactive({
  pageNum: 1,                                               // 当前页码
  pageSize: 10,                                             // 每页条数
  total: 0                                                  // 总条数
})

// 新增/编辑表单数据
const formData = reactive({
  accountId: null,                                          // 账户ID
  categoryId: null,                                         // 分类ID
  type: TRANSACTION_TYPE_EXPENSE,        // 默认支出
  amount: null,                                             // 金额
  note: '',                                                 // 备注
  time: ''                                                  // 时间
})

// 表单校验规则
const formRules = {
  accountId: [{ required: true, message: '请选择账户', trigger: 'change' }], // 账户必选
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }], // 分类必选
  type: [{ required: true, message: '请选择类型', trigger: 'change' }], // 类型必选
  amount: [                                                 // 金额校验
    { required: true, message: '请输入金额', trigger: 'blur' }, // 必填
    { type: 'number', min: 0.01, message: '金额必须大于0', trigger: 'blur' } // 最小值
  ],
  time: [{ required: true, message: '请选择时间', trigger: 'change' }], // 时间必选
  note: [{ max: 200, message: '备注长度不能超过200', trigger: 'blur' }] // 备注长度
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
  const categoryType = formData.type === TRANSACTION_TYPE_EXPENSE ? CATEGORY_TYPE_EXPENSE : CATEGORY_TYPE_INCOME // 反转映射类型值
  return categoryList.value.filter(item => item.type === categoryType) // 筛选匹配分类
})

// 切换类型时重置分类选择→ 避免选中不匹配的分类
watch(() => formData.type, () => {
  formData.categoryId = null                                // 重置分类选择
})

/** 分页 pageSize 变化处理（重置到第1页并加载） */
function handleSizeChange(size) {
  pagination.pageSize = size                                // 更新每页条数
  pagination.pageNum = 1                                    // 重置到第1页
  loadTransactions()                                        // 重新加载列表
}

/** 分页页码变化处理 */
function handlePageChange(page) {
  pagination.pageNum = page                                 // 更新页码
  loadTransactions()                                        // 重新加载列表
}

/**
 * 加载交易记录列表（支持筛选 + 分页）
 * → 调用 api/transaction.js 的 getTransactionList(params)
 */
async function loadTransactions() {
  loading.value = true                                      // 开启loading
  try {
    const params = {                                        // 构建请求参数
      pageNum: pagination.pageNum,                          // 页码参数
      pageSize: pagination.pageSize                         // 每页条数参数
    }
    // 拼接筛选参数
    if (filters.dateRange) {                                // 有日期范围筛选
      params.startTime = filters.dateRange[0] + TIME_START_OF_DAY // 开始时间
      params.endTime = filters.dateRange[1] + TIME_END_OF_DAY   // 结束时间
    }
    if (filters.accountId != null) params.accountId = filters.accountId // 账户筛选
    if (filters.categoryId != null) params.categoryId = filters.categoryId // 分类筛选
    if (filters.keyword) params.keyword = filters.keyword  // 关键词筛选

    // → 调用 api/transaction.js 的 getTransactionList(params)
    const data = await getTransactionList(params)            // 调用列表API
    // 兼容后端分页返回结构（records/list/直接数组）
    transactionList.value = data?.records || data?.list || data || [] // 兼容多种返回结构
    pagination.total = data?.total || 0                     // 更新总条数
  } finally {
    loading.value = false                                   // 关闭loading
  }
}

/**
 * 加载下拉选项数据（账户 + 分类）
 * → 调用 api/account.js 的 getAccountList() + api/category.js 的 getCategoryList()
 * 并行请求提高加载速度
 */
// → 调用 api/account.js 的 getAccountList() + api/category.js 的 getCategoryList()
async function loadOptions() {
  try {
    const [accounts, categories] = await Promise.all([getAccountList(), getCategoryList()]) // 并行加载
    accountList.value = accounts || []                       // 设置账户选项
    categoryList.value = categories || []                    // 设置分类选项
  } catch (e) {
    log.warn('加载选项数据失败:', e) // 开发环境日志
    ElMessage.error('账户/分类选项加载失败，下拉框可能为空，请刷新页面重试') // 明确错误提示（含影响说明）
  }
}

/**
 * 同步筛选条件到 URL query params（PRD P1-1）
 * 支持浏览器前进后退保留筛选状态
 */
function syncFiltersToUrl() {
  const q = {}                                              // 构建query对象
  if (filters.dateRange) { q.startDate = filters.dateRange[0]; q.endDate = filters.dateRange[1] } // 日期范围
  if (filters.accountId) q.accountId = filters.accountId   // 账户筛选
  if (filters.categoryId) q.categoryId = filters.categoryId // 分类筛选
  if (filters.keyword) q.keyword = filters.keyword         // 关键词
  if (pagination.pageNum > 1) q.page = pagination.pageNum  // 页码
  router.replace({ query: q })                              // 替换URL query参数
}

/** 从 URL query params 读取筛选条件（页面初始化时调用） */
function readFiltersFromUrl() {
  const q = route.query                                     // 获取URL query参数
  if (q.startDate && q.endDate) filters.dateRange = [q.startDate, q.endDate] // 读取日期范围
  if (q.accountId) filters.accountId = Number(q.accountId) // 读取账户筛选
  if (q.categoryId) filters.categoryId = Number(q.categoryId) // 读取分类筛选
  if (q.keyword) filters.keyword = q.keyword               // 读取关键词
  if (q.page) pagination.pageNum = Number(q.page)          // 读取页码
}

/** 搜索：重置页码到第 1 页 + 同步 URL + 重新加载 */
function handleSearch() {
  pagination.pageNum = 1                                    // 重置页码
  syncFiltersToUrl()                                        // 同步到URL
  loadTransactions()                                        // 重新加载
}

/** 重置筛选条件 + 清空 URL query params */
function resetFilters() {
  filters.dateRange = null                                  // 清空日期范围
  filters.accountId = null                                  // 清空账户筛选
  filters.categoryId = null                                 // 清空分类筛选
  filters.keyword = ''                                      // 清空关键词
  router.replace({ query: {} })                             // 清空URL参数
  handleSearch()                                            // 重置后搜索
}

/**
 * 打开新增/编辑弹窗
 * @param {Object|null} row - 传入行数据为编辑模式，不传为「记一笔」新增模式
 */
function openDialog(row) {
  isEdit.value = !!row                                      // 判断是否编辑模式
  editId.value = row?.id || null                            // 获取编辑记录ID
  isTransfer.value = !!row?.transferId    // 判断是否为转账记录
  if (row) {
    // 编辑模式：回填表单
    formData.accountId = row.accountId                      // 回填账户ID
    formData.categoryId = row.categoryId                    // 回填分类ID
    formData.type = row.type                                // 回填类型
    formData.amount = Number(row.amount || 0)               // 回填金额
    formData.note = row.note || ''                           // 回填备注
    formData.time = row.time || ''                           // 回填时间
  } else {
    // 新增模式：重置表单，默认时间设为当前
    formData.accountId = null                               // 清空账户
    formData.categoryId = null                              // 清空分类
    formData.type = TRANSACTION_TYPE_EXPENSE                 // 默认支出
    formData.amount = null                                  // 清空金额
    formData.note = ''                                      // 清空备注
    formData.time = formatDateTime(new Date())              // 默认当前时间
  }
  dialogVisible.value = true                                // 显示弹窗
}

/**
 * 删除交易记录
 * → 调用 api/transaction.js 的 deleteTransaction(id)
 * 转账记录不显示删除按钮（已在模板中 v-if="!row.transferId" 隐藏）
 */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除这条${TRANSACTION_TYPE_MAP[row.type]?.label || '未知'}记录吗？`, '确认删除', { // 删除确认
      type: 'warning'                                       // 警告类型
    })
    deletingId.value = row.id                               // 标记正在删除
    await deleteTransaction(row.id)                          // 调用删除API
    ElMessage.success('记录已删除')                          // 成功提示
    await loadTransactions()                                 // 刷新列表（添加await确保列表更新完成）
  } catch (e) {
    if (e !== 'cancel') {
      // axios 拦截器已统一处理业务错误，此处记录日志便于排查非业务异常
      log.error('删除记录失败:', e)                    // 记录错误日志
    }
  } finally {
    deletingId.value = null                                 // 重置删除标记
  }
}

/**
 * 提交表单（记一笔或编辑）
 * →  调用 api/transaction.js 的 createTransaction() 或 updateTransaction()
 */
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false) // 触发校验
  if (!valid) return                                        // 校验不通过不提交

  submitting.value = true                                   // 开启提交loading
  try {
    if (isEdit.value) {
      // → 调用 api/transaction.js 的 updateTransaction(id, data)
      await updateTransaction(editId.value, formData)       // 调用更新API
      ElMessage.success('更新成功')                          // 成功提示
    } else {
      // → 调用 api/transaction.js 的 createTransaction(data)
      await createTransaction(formData)                     // 调用创建API
      ElMessage.success('记账成功')                          // 成功提示
    }
    dialogVisible.value = false                             // 关闭弹窗
    await loadTransactions()                                // 刷新列表（添加await确保列表更新完成）
  } finally {
    submitting.value = false                                // 关闭提交loading
  }
}

// 页面挂载时：从 URL 读取筛选条件 → 加载下拉选项 → 加载交易列表
onMounted(async () => {                                     // 改为async以支持await
  readFiltersFromUrl();                                     // 从URL读取筛选条件（同步函数无需await）
  await Promise.all([                                       // 并行加载下拉选项+交易列表（Promise.all更高效）
    loadOptions(),                                          // 加载下拉选项
    loadTransactions()                                      // 加载交易列表
  ])
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
  color: var(--color-title);
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
  color: var(--color-income);
  font-weight: bold;
}

.amount-expense {
  color: var(--color-expense);
  font-weight: bold;
}
</style>
