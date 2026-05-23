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
            <span :class="row.type === TRANSACTION_TYPE_INCOME ? 'amount-income' : 'amount-expense'">
              {{ typeMap[row.type]?.sign || '' }}¥ {{ formatAmount(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === TRANSACTION_TYPE_INCOME ? 'success' : 'danger'" size="small">
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
            <el-tag :type="row.status === STATUS_ACTIVE ? 'success' : 'info'" size="small">
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
          <el-input-number v-model="formData.amount" :precision="2" :min="MIN_TRANSACTION_AMOUNT" :step="AMOUNT_STEP_ROUGH" style="width: 100%" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="formData.type">
            <el-radio :value="TRANSACTION_TYPE_EXPENSE">支出</el-radio>
            <el-radio :value="TRANSACTION_TYPE_INCOME">收入</el-radio>
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
import { ref, reactive, computed, watch, onMounted } from 'vue' // 导入Vue组合式API
import { ElMessage, ElMessageBox } from 'element-plus'      // 导入消息和确认框
// → 调用 api/recurring-bill.js 的 5 个接口函数
import {
  getRecurringBillList,
  createRecurringBill,
  updateRecurringBill,
  deleteRecurringBill,
  generateRecurringBill
} from '../api/recurring-bill'                               // 导入周期账单API
// → 调用 api/account.js 的 getAccountList()（下拉选项）
import { getAccountList } from '../api/account'              // 导入账户列表API
// → 调用 api/category.js 的 getCategoryList()（下拉选项）
import { getCategoryList } from '../api/category'             // 导入分类列表API
import { formatAmount, formatDate } from '../utils/format'   // 导入格式化工具
import { PERIOD_MAP, TRANSACTION_TYPE_MAP, STATUS_MAP, TRANSACTION_TYPE_INCOME, TRANSACTION_TYPE_EXPENSE, CATEGORY_TYPE_EXPENSE, CATEGORY_TYPE_INCOME, STATUS_ACTIVE, MIN_TRANSACTION_AMOUNT, BILL_NAME_MAX_LENGTH, DEFAULT_RECURRING_PERIOD, AMOUNT_STEP_ROUGH } from '../constants/finance' // 导入常量
import { logger } from '../utils/logger'                     // 导入统一日志工具

// 创建日志实例（页面标识：RecurringBillPage）
const log = logger('RecurringBillPage')

// 周期类型映射（从常量文件统一管理，避免硬编码散布各页面）
const periodMap = PERIOD_MAP                                 // 周期映射
const typeMap = TRANSACTION_TYPE_MAP                         // 交易类型映射
const statusMap = STATUS_MAP                                 // 状态映射

const loading = ref(false)                                   // 页面loading
const submitting = ref(false)                                // 提交loading
const deactivatingId = ref(null)                             // 正在停用的账单ID
const generatingId = ref(null)                               // 正在生成的账单ID
const dialogVisible = ref(false)                             // 弹窗显隐
const isEdit = ref(false)                                    // 是否编辑模式
const editId = ref(null)                                     // 编辑的账单ID
const formRef = ref(null)                                    // 表单引用

const billList = ref([])         // 周期账单列表
const accountList = ref([])      // 账户下拉选项
const categoryList = ref([])     // 分类下拉选项

// 根据类型筛选分类（交易 type 和分类 type 值相反：交易 1=收入/2=支出，分类 1=支出/2=收入）
const filteredCategories = computed(() => {
  if (!categoryList.value.length) return []                  // 无分类数据返回空
  // type=1(收入)对应 categoryType=2(收入分类)，type=2(支出)对应 categoryType=1(支出分类)
  const categoryType = formData.type === TRANSACTION_TYPE_EXPENSE ? CATEGORY_TYPE_EXPENSE : CATEGORY_TYPE_INCOME          // 反转映射类型值
  return categoryList.value.filter(cat => cat.type === categoryType) // 筛选匹配分类
})

// 切换类型时重置分类选择，避免选中不匹配的分类
watch(() => formData.type, () => {
  formData.categoryId = null                                 // 重置分类选择
})

// 新增/编辑表单数据
const formData = reactive({
  name: '',                                                 // 账单名称
  accountId: null,                                          // 账户ID
  categoryId: null,                                         // 分类ID
  amount: null,                                             // 金额
  type: TRANSACTION_TYPE_EXPENSE,              // 默认支出
  period: DEFAULT_RECURRING_PERIOD,                          // 默认每月（使用常量 constants/finance.js）
  nextDueDate: ''                                           // 下次到期日期
})

// 表单校验规则（所有字段必填）
const formRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }, { max: BILL_NAME_MAX_LENGTH, message: `名称长度不能超过${BILL_NAME_MAX_LENGTH}`, trigger: 'blur' }], // 使用常量 constants/finance.js
  accountId: [{ required: true, message: '请选择账户', trigger: 'change' }], // 账户必选
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }], // 分类必选
  amount: [                                                 // 金额校验
    { required: true, message: '请输入金额', trigger: 'blur' }, // 必填
    { type: 'number', min: MIN_TRANSACTION_AMOUNT, message: '金额必须大于0', trigger: 'blur' } // 使用常量 constants/finance.js
  ],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }], // 类型必选
  period: [{ required: true, message: '请选择周期', trigger: 'change' }], // 周期必选
  nextDueDate: [{ required: true, message: '请选择下次到期日期', trigger: 'change' }] // 日期必选
}

/**
 * 加载周期账单列表
 * → 调用 api/recurring-bill.js 的 getRecurringBillList()
 */
async function loadBills() {
  loading.value = true                                      // 开启loading
  try {
    const data = await getRecurringBillList()                // 调用列表API
    billList.value = data || []                              // 设置账单数据
  } catch (e) {
    log.warn('加载周期账单列表失败:', e) /* 开发环境日志 */
    ElMessage.error('账单列表加载失败，请刷新重试')            // 用户级错误提示
    billList.value = []                                      // 清空数据
  } finally {
    loading.value = false                                    // 关闭loading
  }
}

/**
 * 加载下拉选项（账户 + 分类，并行请求）
 * → 调用 api/account.js 的 getAccountList() + api/category.js 的 getCategoryList()
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
 * 打开新增/编辑弹窗
 * @param {Object|null} row - 传入行数据为编辑模式
 */
function openDialog(row) {
  isEdit.value = !!row                                      // 判断是否编辑模式
  editId.value = row?.id || null                            // 获取编辑ID
  if (row) {
    // 编辑模式：回填表单
    formData.name = row.name                                // 回填名称
    formData.accountId = row.accountId                      // 回填账户
    formData.categoryId = row.categoryId                    // 回填分类
    formData.amount = Number(row.amount || 0)               // 回填金额
    formData.type = row.type                                // 回填类型
    formData.period = row.period                            // 回填周期
    formData.nextDueDate = row.nextDueDate?.substring(0, 10) || '' // 回填到期日期
  } else {
    // 新增模式：重置表单
    formData.name = ''                                      // 清空名称
    formData.accountId = null                               // 清空账户
    formData.categoryId = null                              // 清空分类
    formData.amount = null                                  // 清空金额
    formData.type = TRANSACTION_TYPE_EXPENSE                                       // 默认支出
    formData.period = DEFAULT_RECURRING_PERIOD               // 默认每月（使用常量 constants/finance.js）
    formData.nextDueDate = ''                               // 清空日期
  }
  dialogVisible.value = true                                // 显示弹窗
}

/**
 * 提交表单（新增或编辑周期账单）
 * → 调用 api/recurring-bill.js 的 createRecurringBill() 或 updateRecurringBill()
 */
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false) // 触发校验
  if (!valid) return                                        // 校验不通过不提交

  submitting.value = true                                   // 开启提交loading
  try {
    if (isEdit.value) {
      await updateRecurringBill(editId.value, formData)     // 调用更新API
      ElMessage.success('更新成功')                          // 成功提示
    } else {
      await createRecurringBill(formData)                   // 调用创建API
      ElMessage.success('新增成功')                          // 成功提示
    }
    dialogVisible.value = false                             // 关闭弹窗
    await loadBills()                                       // 刷新列表（修复：添加await确保列表刷新完成后再重置loading状态）
  } catch (e) {
    log.warn('保存周期账单失败:', e) /* 开发环境日志 */
    if (e.code === 'ERR_NETWORK' || e.code === 'ECONNABORTED') {  // 网络错误或超时
      ElMessage.error('网络异常，操作失败')                 // 网络级错误提示
    }
  } finally {
    submitting.value = false                                // 关闭提交loading
  }
}

/**
 * 停用周期账单（二次确认后执行软删除）
 * → 调用 api/recurring-bill.js 的 deleteRecurringBill(id)
 */
async function handleDeactivate(row) {
  try {
    await ElMessageBox.confirm('确定停用该周期账单吗？', '提示', { type: 'warning' }) // 停用确认
    deactivatingId.value = row.id                           // 标记正在停用
    await deleteRecurringBill(row.id)                       // 调用停用API
    ElMessage.success('已停用')                              // 成功提示
    await loadBills()                                       // 刷新列表（修复：添加await确保刷新完成后再重置停用状态）
  } catch (e) {
    if (e === 'cancel' || e === 'close') {                     // 用户取消（按钮）或关闭（X/遮罩）
      // 用户取消或关闭弹窗，无需处理
    } else {
      // 其他错误由 axios 拦截器统一处理，此处记录日志便于排查
      log.error('停用账单失败:', e)                    // 记录错误日志
    }
  } finally {
    deactivatingId.value = null                             // 重置停用标记
  }
}

/**
 * 手动生成该周期账单对应的交易记录（二次确认后执行）
 * → 调用 api/recurring-bill.js 的 generateRecurringBill(id)
 */
async function handleGenerate(row) {
  try {
    await ElMessageBox.confirm('确定生成该周期账单的交易记录吗？', '提示', { type: 'info' }) // 生成确认
    generatingId.value = row.id                             // 标记正在生成
    await generateRecurringBill(row.id)                     // 调用生成API
    ElMessage.success('生成成功')                            // 成功提示
    await loadBills()                                       // 刷新列表（修复：添加await确保刷新完成后再重置生成状态）
  } catch (e) {
    if (e === 'cancel' || e === 'close') {                     // 修复：添加 'close' 判断，覆盖 X 按钮/遮罩关闭场景
      // 用户取消或关闭弹窗，无需处理
    } else if (typeof e !== 'string') {                       // 修复：只有真正的异常才记录错误日志，ElMessageBox 取消返回 string 类型
      // 其他错误由 axios 拦截器统一处理，此处记录日志便于排查
      log.error('生成账单失败:', e)                    // 记录错误日志
    }
  } finally {
    generatingId.value = null                               // 重置生成标记
  }
}

// 页面挂载时加载下拉选项和周期账单列表（async+Promise.all 并行加载）
onMounted(async () => {
  try {
    await Promise.all([                                        // 并行加载下拉选项+账单列表
      loadOptions(),                                           // 加载下拉选项
      loadBills()                                              // 加载账单列表
    ])
  } catch (e) {
    log.error('页面初始化加载失败:', e)                          // 记录错误日志（各子函数已有独立错误处理）
  }
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
