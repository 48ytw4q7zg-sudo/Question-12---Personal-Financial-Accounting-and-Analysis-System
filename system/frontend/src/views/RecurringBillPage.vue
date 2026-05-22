<!--
  周期账单页面
  路由：/recurring-bill
  对应 PRD 功能：P1 周期性账单（周期性收支提醒）

  功能说明：
    - 周期账单列表表格（名称/账户/分类/金额/类型/周期/下次到期/状态/操作）
    - 新增/编辑周期账单弹窗
    - 停用操作（二次确认）
    - 手动生成交易记录操作

  调用关系：
    → 调用 api/recurring-bill.js 的 getRecurringBillList()（加载列表）
    → 调用 api/recurring-bill.js 的 createRecurringBill()（新增）
    → 调用 api/recurring-bill.js 的 updateRecurringBill()（编辑）
    → 调用 api/recurring-bill.js 的 deleteRecurringBill()（停用）
    → 调用 api/recurring-bill.js 的 generateRecurringBill()（生成交易记录）
    → 调用 api/account.js 的 getAccountList()（下拉选项）
    → 调用 api/category.js 的 getCategoryList()（下拉选项）
-->
<template>
  <div class="recurring-bill-page">
    <div class="page-header">
      <h2>周期账单</h2>
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon>新增周期账单
      </el-button>
    </div>

    <!-- 周期账单列表 -->
    <el-card shadow="hover">
      <el-table :data="billList" v-loading="loading" stripe aria-label="周期账单列表">
        <template #empty><el-empty description="暂无周期账单" /></template>
        <el-table-column prop="name" label="名称" min-width="120" />
        <el-table-column prop="accountName" label="账户" width="100" />
        <el-table-column prop="categoryName" label="分类" width="100" />
        <!-- 金额：收入+/支出- -->
        <el-table-column prop="amount" label="金额" width="110">
          <template #default="{ row }">
            <span :class="row.type === 1 ? 'amount-income' : 'amount-expense'">
              {{ typeMap[row.type]?.sign || '' }}¥ {{ formatAmount(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === 1 ? 'success' : 'danger'" size="small">
              {{ typeMap[row.type]?.label || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 周期：monthly=每月, weekly=每周 -->
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
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ statusMap[row.status] || '未知' }}
            </el-tag>
            <!-- PRD P1-4 业务规则③: 活跃账单关联账户被禁用 → 标记异常 -->
            <el-tag v-if="row.accountDisabled" type="danger" size="small" style="margin-left: 4px">账户异常</el-tag>
          </template>
        </el-table-column>
        <!-- 操作列：编辑 / 停用 / 生成交易记录 -->
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-button type="warning" link @click="handleDeactivate(row)" :disabled="deactivatingId === row.id">停用</el-button>
            <el-button type="success" link @click="handleGenerate(row)" :disabled="generatingId === row.id">生成</el-button>
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
            <el-option v-for="cat in filteredCategories" :key="cat.id" :label="cat.name" :value="cat.id" />
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
            <el-option v-for="(label, key) in periodMap" :key="key" :label="label" :value="key" />
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
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
// → 调用 api/recurring-bill.js 的 5 个接口函数
import {
  getRecurringBillList,
  createRecurringBill,
  updateRecurringBill,
  deleteRecurringBill,
  generateRecurringBill
} from '../api/recurring-bill'
// → 调用 api/account.js 的 getAccountList()（下拉选项）
import { getAccountList } from '../api/account'
// → 调用 api/category.js 的 getCategoryList()（下拉选项）
import { getCategoryList } from '../api/category'
import { formatAmount, formatDate } from '../utils/format'
import { PERIOD_MAP, TRANSACTION_TYPE_MAP, STATUS_MAP } from '../constants/finance'

// 周期类型映射（从常量文件统一管理，避免硬编码散布各页面）
const periodMap = PERIOD_MAP
const typeMap = TRANSACTION_TYPE_MAP
const statusMap = STATUS_MAP

const loading = ref(false)
const submitting = ref(false)
const deactivatingId = ref(null)
const generatingId = ref(null)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)

const billList = ref([])         // 周期账单列表
const accountList = ref([])      // 账户下拉选项
const categoryList = ref([])     // 分类下拉选项

// 根据类型筛选分类（交易 type 和分类 type 值相反：交易 1=收入/2=支出，分类 1=支出/2=收入）
const filteredCategories = computed(() => {
  if (!categoryList.value.length) return []
  // type=1(收入)对应 categoryType=2(收入分类)，type=2(支出)对应 categoryType=1(支出分类)
  const categoryType = formData.type === 2 ? 1 : 2
  return categoryList.value.filter(cat => cat.type === categoryType)
})

// 切换类型时重置分类选择，避免选中不匹配的分类
watch(() => formData.type, () => {
  formData.categoryId = null
})

// 新增/编辑表单数据
const formData = reactive({
  name: '',
  accountId: null,
  categoryId: null,
  amount: null,
  type: 2,              // 默认支出
  period: 'monthly',    // 默认每月
  nextDueDate: ''
})

// 表单校验规则（所有字段必填）
const formRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }, { max: 30, message: '名称长度不能超过30', trigger: 'blur' }],
  accountId: [{ required: true, message: '请选择账户', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  amount: [
    { required: true, message: '请输入金额', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '金额必须大于0', trigger: 'blur' }
  ],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  period: [{ required: true, message: '请选择周期', trigger: 'change' }],
  nextDueDate: [{ required: true, message: '请选择下次到期日期', trigger: 'change' }]
}

/**
 * 加载周期账单列表
 * → 调用 api/recurring-bill.js 的 getRecurringBillList()
 */
async function loadBills() {
  loading.value = true
  try {
    const data = await getRecurringBillList()
    billList.value = data || []
  } finally {
    loading.value = false
  }
}

/**
 * 加载下拉选项（账户 + 分类， 并行请求）
 * → 调用 api/account.js 的 getAccountList() + api/category.js 的 getCategoryList()
 */
async function loadOptions() {
  try {
    const [accounts, categories] = await Promise.all([getAccountList(), getCategoryList()])
    accountList.value = accounts || []
    categoryList.value = categories || []
  } catch (e) {
    if (import.meta.env.DEV) console.warn('加载选项数据失败:', e)
    ElMessage.warning('加载选项数据失败，请刷新重试')
  }
}

/**
 * 打开新增/编辑弹窗
 * @param {Object|null} row - 传入行数据为编辑模式
 */
function openDialog(row) {
  isEdit.value = !!row
  editId.value = row?.id || null
  if (row) {
    // 编辑模式：回填表单
    formData.name = row.name
    formData.accountId = row.accountId
    formData.categoryId = row.categoryId
    formData.amount = Number(row.amount || 0)
    formData.type = row.type
    formData.period = row.period
    formData.nextDueDate = row.nextDueDate?.substring(0, 10) || ''
  } else {
    // 新增模式：重置表单
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

/**
 * 提交表单（新增或编辑周期账单）
 * → 调用 api/recurring-bill.js 的 createRecurringBill() 或 updateRecurringBill()
 */
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

/**
 * 停用周期账单（二次确认后执行软删除）
 * → 调用 api/recurring-bill.js 的 deleteRecurringBill(id)
 */
async function handleDeactivate(row) {
  try {
    await ElMessageBox.confirm('确定停用该周期账单吗？', '提示', { type: 'warning' })
    deactivatingId.value = row.id
    await deleteRecurringBill(row.id)
    ElMessage.success('已停用')
    loadBills()
  } catch (e) {
    if (e === 'cancel') {
      // 用户取消，无需处理
    } else {
      // 其他错误由 axios 拦截器统一处理，此处记录日志便于排查
      console.error('停用账单失败:', e)
    }
  } finally {
    deactivatingId.value = null
  }
}

/**
 * 手动生成该周期账单对应的交易记录（二次确认后执行）
 * → 调用 api/recurring-bill.js 的 generateRecurringBill(id)
 */
async function handleGenerate(row) {
  try {
    await ElMessageBox.confirm('确定生成该周期账单的交易记录吗？', '提示', { type: 'info' })
    generatingId.value = row.id
    await generateRecurringBill(row.id)
    ElMessage.success('生成成功')
    loadBills()
  } catch (e) {
    if (e === 'cancel') {
      // 用户取消，无需处理
    } else {
      // 其他错误由 axios 拦截器统一处理，此处记录日志便于排查
      console.error('生成账单失败:', e)
    }
  } finally {
    generatingId.value = null
  }
}

// 页面挂载时加载下拉选项和周期账单列表
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
  color: var(--color-title);
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
