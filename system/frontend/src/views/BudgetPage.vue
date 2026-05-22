<!--
  预算管理页面
  路由：/budget
  对应 PRD 功能：P1 预算管理（月预算按分类设置 + 超支标记）+ P2-2 预算预警

  功能说明：
    - 顶部月份选择器切换查看不同月份的预算
    - 预算进度表格：分类 / 预算金额 / 已支出 / 进度条 / 状态
    - 进度条颜色：绿色(<80%) / 橙色(80-100%) / 红色(>=100% 超支)
    - P2-2 预警标签：OVERSPENT(红) / MONTHLY_WARN(橙) / DAILY_WARN(黄) / NORMAL(绿)
    - 设置/编辑/删除预算弹窗

  调用关系：
    → 调用 api/budget.js 的 getBudgetProgress()（加载预算执行进度）
    → 调用 api/budget.js 的 getBudgetAlert()（加载预警级别 · P2-2）
    → 调用 api/budget.js 的 saveBudget()（设置/更新预算）
    → 调用 api/budget.js 的 deleteBudget()（删除预算）
    → 调用 api/category.js 的 getCategoryList()（加载支出分类下拉选项）
-->
<template>
  <div class="budget-page">
    <div class="page-header">
      <h2>预算管理</h2>
      <div class="header-actions">
        <!-- 月份选择器：切换月份后自动刷新预算进度 -->
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

    <!-- 预算进度表格 -->
    <el-card shadow="hover">
      <el-table :data="budgetProgress" v-loading="loading" stripe aria-label="预算进度表">
        <template #empty><el-empty description="暂未设置预算" /></template>
        <el-table-column prop="categoryName" label="分类" min-width="120" />
        <el-table-column prop="budgetAmount" label="预算金额" width="120">
          <template #default="{ row }">
            ¥ {{ formatAmount(row.budgetAmount) }}
          </template>
        </el-table-column>
        <el-table-column prop="spentAmount" label="已支出" width="120">
          <template #default="{ row }">
            ¥ {{ formatAmount(row.spentAmount) }}
          </template>
        </el-table-column>
        <!-- 进度条：根据已支出/预算金额计算百分比，颜色随百分比变化 -->
        <el-table-column label="进度" min-width="200">
          <template #default="{ row }">
            <el-progress
              :percentage="getProgress(row)"
              :color="getProgressColor(row)"
              :stroke-width="18"
            />
          </template>
        </el-table-column>
        <!-- 状态标签：P2-2 四级预警颜色映射 -->
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.alertLevel === ALERT_LEVEL_OVERSPENT" type="danger" size="small">已超支</el-tag>
            <el-tag v-else-if="row.alertLevel === ALERT_LEVEL_MONTHLY_WARN" type="warning" size="small">月预警</el-tag>
            <el-tag v-else-if="row.alertLevel === ALERT_LEVEL_DAILY_WARN" type="warning" size="small">日预警</el-tag>
            <el-tag v-else type="success" size="small">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDeleteBudget(row)" :disabled="deletingId === row.id">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 设置/编辑预算弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑预算' : '设置预算'" width="420px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="分类" prop="categoryId">
          <!-- 编辑时禁用分类选择（防止修改已有预算的分类） -->
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
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
// → 调用 api/budget.js 的 getBudgetProgress()、getBudgetAlert()、saveBudget()、deleteBudget()
import { getBudgetProgress, getBudgetAlert, saveBudget, deleteBudget } from '../api/budget'
import { formatAmount } from '../utils/format'
import { getCategoryList } from '../api/category'
import { ALERT_LEVEL_OVERSPENT, ALERT_LEVEL_MONTHLY_WARN, ALERT_LEVEL_DAILY_WARN, CATEGORY_TYPE_EXPENSE } from '../constants/finance'

const loading = ref(false)
const submitting = ref(false)
const deletingId = ref(null)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

// 当前选中月份（默认当前月，格式 "YYYY-MM" · 使用本地时间而非 UTC）
const now = new Date()
const selectedMonth = ref(`${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`)
const budgetProgress = ref([])       // 预算进度列表
const cssVarsCache = ref(null)      // CSS 变量缓存（避免每行 getComputedStyle）
const expenseCategories = ref([])    // 支出分类列表（下拉选项）

// 新增/编辑表单数据
const formData = reactive({
  categoryId: null,
  amount: null
})

// 表单校验规则
const formRules = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  amount: [
    { required: true, message: '请输入预算金额', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '预算金额必须大于0', trigger: 'blur' }
  ]
}

/**
 * 计算预算进度百分比
 * @returns {Number} 0-100 的整数
 */
function getProgress(row) {
  if (!row.budgetAmount || row.budgetAmount <= 0) return 0
  const pct = Math.round((row.spentAmount / row.budgetAmount) * 100)
  return pct  // 允许超过100%显示超支程度，el-progress会自动渲染
}

/**
 * 根据进度百分比返回进度条颜色
 *   >=100% → 红色（超支）
 *   >=80%  → 橙色（接近超支）
 *   <80%   → 绿色（正常）
 */
function getProgressColor(row) {
  const pct = getProgress(row)
  // 缓存 CSS 变量值（避免每行渲染都调用 getComputedStyle 强制样式重算）
  if (!cssVarsCache.value) {
    const rootStyles = getComputedStyle(document.documentElement)
    cssVarsCache.value = {
      expense: rootStyles.getPropertyValue('--color-expense').trim(),
      warning: rootStyles.getPropertyValue('--color-warning').trim(),
      income: rootStyles.getPropertyValue('--color-income').trim()
    }
  }
  const colors = cssVarsCache.value
  if (pct >= 100) return colors.expense
  if (pct >= 80) return colors.warning
  return colors.income
}

/**
 * 加载预算进度 + 预警数据（P2-2）
 * → 调用 api/budget.js 的 getBudgetProgress({ year, month })（进度数据）
 * → 调用 api/budget.js 的 getBudgetAlert({ year, month })（预警级别 · P2-2）
 * 合并逻辑：按 categoryId 匹配，将 alertLevel 注入到进度行中
 */
async function loadData() {
  loading.value = true
  try {
    const [year, month] = selectedMonth.value.split('-')
    const params = { year: Number(year), month: Number(month) }

    // 并行加载进度和预警数据
    const [progress, alerts] = await Promise.all([
      getBudgetProgress(params),
      getBudgetAlert(params)
    ])

    // 构建 categoryId → alertLevel 映射
    const alertMap = {}
    if (alerts) {
      alerts.forEach(a => { alertMap[a.categoryId] = a.alertLevel })
    }

    // 将 alertLevel 注入到进度数据中
    budgetProgress.value = (progress || []).map(item => ({
      ...item,
      alertLevel: alertMap[item.categoryId] || null
    }))
  } finally {
    loading.value = false
  }
}

/**
 * 加载支出分类列表（筛选 type=1 的支出分类）
 * → 调用 api/category.js 的 getCategoryList()
 */
async function loadCategories() {
  try {
    const data = await getCategoryList()
    expenseCategories.value = (data || []).filter(item => item.type === CATEGORY_TYPE_EXPENSE)
  } catch (e) {
    if (import.meta.env.DEV) console.warn('加载分类列表失败:', e)
    ElMessage.warning('分类选项加载失败，请刷新重试')
    // axios 拦截器已统一处理业务错误消息
  }
}

/**
 * 打开设置/编辑弹窗
 * @param {Object|null} row - 传入行数据为编辑模式，不传为新增
 */
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

/**
 * 提交预算设置/更新
 * → 调用 api/budget.js 的 saveBudget({ categoryId, amount, month })
 */
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await saveBudget({
      categoryId: formData.categoryId,
      amount: formData.amount,
      month: selectedMonth.value  // "YYYY-MM" 格式，对齐后端 BudgetRequest @Pattern 校验
    })
    ElMessage.success(isEdit.value ? '更新成功' : '设置成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

/**
 * 删除预算
 * → 调用 api/budget.js 的 deleteBudget(id)
 */
async function handleDeleteBudget(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.categoryName}」的预算吗？`, '确认删除', {
      type: 'warning'
    })
    deletingId.value = row.id
    await deleteBudget(row.id)
    ElMessage.success('预算已删除')
    loadData()
  } catch (e) {
    if (e === 'cancel') {
      // 用户取消，无需处理
    } else {
      // 其他错误由 axios 拦截器统一处理，此处记录日志便于排查
      console.error('删除预算失败:', e)
    }
  } finally {
    deletingId.value = null
  }
}

// 页面挂载时加载分类选项和预算进度
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
  color: var(--color-title);
}

.header-actions {
  display: flex;
  gap: 12px;
}
</style>
