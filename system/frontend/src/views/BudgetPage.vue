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
        <!-- el-date-picker type="month"：Element Plus 月份选择器，切换月份后 @change → loadData() -->
        <el-date-picker
          v-model="selectedMonth"          <!-- 绑定 YYYY-MM 格式字符串 -->
          type="month"                     <!-- 月份选择模式 -->
          placeholder="选择月份"
          value-format="YYYY-MM"          <!-- 值格式 -->
          @change="loadData"              <!-- 月份变更 → 加载预算进度 + 预警数据 -->
        />
        <el-button type="primary" @click="openDialog()">
          <el-icon><Plus /></el-icon>设置预算  <!-- @element-plus/icons-vue Plus 图标 -->
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
            ¥ {{ formatAmount(row.budgetAmount) }}  <!-- formatAmount → utils/format.js -->
          </template>
        </el-table-column>
        <el-table-column prop="spentAmount" label="已支出" width="120">
          <template #default="{ row }">
            ¥ {{ formatAmount(row.spentAmount) }}
          </template>
        </el-table-column>
        <!-- 进度条：el-progress Element Plus 进度条组件，根据已支出/预算金额计算百分比，颜色随百分比变化 -->
        <el-table-column label="进度" min-width="200">
          <template #default="{ row }">
            <el-progress
              :percentage="getProgress(row)"       <!-- 百分比：spentAmount / budgetAmount * 100 -->
              :color="getProgressColor(row)"       <!-- 颜色：<80%绿 / 80-100%橙 / >=100%红 -->
              :stroke-width="18"                   <!-- 进度条高度 18px -->
            />
          </template>
        </el-table-column>
        <!-- 状态标签：P2-2 四级预警颜色映射 -->
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <!-- el-tag：Element Plus 标签，按 alertLevel 显示不同颜色 -->
            <el-tag v-if="row.alertLevel === ALERT_LEVEL_OVERSPENT" type="danger" size="small">已超支</el-tag>      <!-- 红色 -->
            <el-tag v-else-if="row.alertLevel === ALERT_LEVEL_MONTHLY_WARN" type="warning" size="small">月预警</el-tag> <!-- 橙色 -->
            <el-tag v-else-if="row.alertLevel === ALERT_LEVEL_DAILY_WARN" type="warning" size="small">日预警</el-tag>  <!-- 橙色 -->
            <el-tag v-else type="success" size="small">正常</el-tag>                                                  <!-- 绿色 -->
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(row)">编辑</el-button>   <!-- openDialog(row) 回填表单 -->
            <el-button type="danger" link @click="handleDeleteBudget(row)" :disabled="deletingId === row.id">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- el-dialog：Element Plus 弹窗，设置/编辑预算 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑预算' : '设置预算'" width="420px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="分类" prop="categoryId">
          <!-- el-select：Element Plus 下拉选择器，编辑时禁用分类选择（:disabled="isEdit"），防止修改已有预算的分类 -->
          <el-select v-model="formData.categoryId" placeholder="请选择支出分类" style="width: 100%" :disabled="isEdit">
            <el-option
              v-for="cat in expenseCategories"   <!-- 仅展示支出分类（category.type === 1） -->
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="预算金额" prop="amount">
          <!-- el-input-number：Element Plus 数字输入框 -->
          <el-input-number v-model="formData.amount" :precision="2" :min="MIN_TRANSACTION_AMOUNT" :step="AMOUNT_STEP_ROUGH" style="width: 100%" />
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
import { ref, reactive, onMounted } from 'vue'               // 导入Vue组合式API
import { ElMessage, ElMessageBox } from 'element-plus'      // 导入消息和确认框
// → 调用 api/budget.js 的 getBudgetProgress()、getBudgetAlert()、saveBudget()、deleteBudget()
import { getBudgetProgress, getBudgetAlert, saveBudget, deleteBudget } from '../api/budget' // 导入预算API
import { formatAmount } from '../utils/format'               // 导入金额格式化
import { getCategoryList } from '../api/category'             // 导入分类列表API
import { ALERT_LEVEL_OVERSPENT, ALERT_LEVEL_MONTHLY_WARN, ALERT_LEVEL_DAILY_WARN, CATEGORY_TYPE_EXPENSE, MIN_TRANSACTION_AMOUNT, AMOUNT_STEP_ROUGH } from '../constants/finance' // 导入常量
import { logger } from '../utils/logger'                    // 导入统一日志工具

const log = logger('BudgetPage')                            // 创建日志实例
const loading = ref(false)                                  // 页面loading
const submitting = ref(false)                               // 提交loading
const deletingId = ref(null)                                // 正在删除的预算ID
const dialogVisible = ref(false)                            // 弹窗显隐
const isEdit = ref(false)                                   // 是否编辑模式
const formRef = ref(null)                                   // 表单引用

// 当前选中月份（默认当前月，格式 "YYYY-MM" · 使用 getCurrentMonth() 确保每次挂载时获取实时日期）
function getCurrentMonth() {                                   // 获取当前月份字符串（挂载时调用，避免模块级捕获过期时间）
  const now = new Date()                                       // 挂载时获取实时时间
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}` // 格式: YYYY-MM
}
const selectedMonth = ref(getCurrentMonth())                   // 默认当前月（挂载时计算）
const budgetProgress = ref([])       // 预算进度列表
// cssVarsCache 已移除 — getProgressColor() 改为实时读取 CSS 变量，支持暗色模式等主题切换
const expenseCategories = ref([])    // 支出分类列表（下拉选项）

// 新增/编辑表单数据
const formData = reactive({
  categoryId: null,                                         // 分类ID
  amount: null                                              // 预算金额
})

// 表单校验规则
const formRules = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }], // 分类必选
  amount: [                                                 // 金额校验
    { required: true, message: '请输入预算金额', trigger: 'blur' },  // 必填
    { type: 'number', min: MIN_TRANSACTION_AMOUNT, message: '预算金额必须大于0', trigger: 'blur' } // 使用常量 constants/finance.js
  ]
}

/**
 * 计算预算进度百分比
 * @returns {Number} 0-100 的整数
 */
function getProgress(row) {
  if (!row.budgetAmount || row.budgetAmount <= 0) return 0  // 无预算返回0
  const pct = Math.round((row.spentAmount / row.budgetAmount) * 100) // 计算百分比
  return pct  // 允许超过100%显示超支程度，el-progress会自动渲染
}

/**
 * 根据进度百分比返回进度条颜色
 *   >=100% → 红色（超支）
 *   >=80%  → 橙色（接近超支）
 *   <80%   → 绿色（正常）
 */
function getProgressColor(row) {
  const pct = getProgress(row)                              // 获取进度百分比
  // 移除 CSS 变量缓存：缓存导致主题切换（如暗色模式）后颜色不更新
  // getComputedStyle 在表格单元格渲染路径的性能影响可忽略（每行仅调用一次）
  const rootStyles = getComputedStyle(document.documentElement) // 实时获取根元素样式
  const colors = {                                           // 实时提取颜色变量
    expense: rootStyles.getPropertyValue('--color-expense').trim(), // 红色（超支）
    warning: rootStyles.getPropertyValue('--color-warning').trim(), // 橙色（接近超支）
    income: rootStyles.getPropertyValue('--color-income').trim()    // 绿色（正常）
  }
  if (pct >= 100) return colors.expense                     // 超支红色
  if (pct >= 80) return colors.warning                      // 接近超支橙色
  return colors.income                                      // 正常绿色
}

/**
 * 加载预算进度 + 预警数据（P2-2）
 * → 调用 api/budget.js 的 getBudgetProgress({ year, month })（进度数据）
 * → 调用 api/budget.js 的 getBudgetAlert({ year, month })（预警级别 · P2-2）
 * 合并逻辑：按 categoryId 匹配，将 alertLevel 注入到进度行中
 */
async function loadData() {
  loading.value = true                                      // 开启loading
  try {
    const [year, month] = selectedMonth.value.split('-')    // 解析年月
    const params = { year: Number(year), month: Number(month) } // 构建请求参数

    // 并行加载进度和预警数据
    const [progress, alerts] = await Promise.all([          // 并行请求
      getBudgetProgress(params),                            // 获取进度数据
      getBudgetAlert(params)                                // 获取预警数据
    ])

    // 构建 categoryId → alertLevel 映射
    const alertMap = {}                                     // 预警映射对象
    if (alerts) {
      alerts.forEach(a => { alertMap[a.categoryId] = a.alertLevel }) // 遍历构建映射
    }

    // 将 alertLevel 注入到进度数据中
    budgetProgress.value = (progress || []).map(item => ({  // 合并进度和预警
      ...item,                                              // 保留原字段
      alertLevel: alertMap[item.categoryId] || null         // 注入预警级别
    }))
  } catch (e) {
    log.warn('加载预算进度失败:', e) /* 开发环境日志 */
    ElMessage.error('预算数据加载失败，请刷新重试')            // 用户级错误提示
    budgetProgress.value = []                                 // 清空数据
  } finally {
    loading.value = false                                   // 关闭loading
  }
}

/**
 * 加载支出分类列表（筛选 type=1 的支出分类）
 * → 调用 api/category.js 的 getCategoryList()
 */
async function loadCategories() {
  try {
    const data = await getCategoryList()                     // 调用分类列表API
    expenseCategories.value = (data || []).filter(item => item.type === CATEGORY_TYPE_EXPENSE) // 筛选支出分类
  } catch (e) {
    log.warn('加载分类列表失败:', e) // 开发环境日志
    ElMessage.warning('分类选项加载失败，请刷新重试')          // 降级提示
    // axios 拦截器已统一处理业务错误消息
  }
}

/**
 * 打开设置/编辑弹窗
 * @param {Object|null} row - 传入行数据为编辑模式，不传为新增
 */
function openDialog(row) {
  isEdit.value = !!row                                      // 判断是否编辑模式
  if (row) {
    formData.categoryId = row.categoryId                    // 回填分类ID
    formData.amount = Number(row.budgetAmount || 0)         // 回填预算金额
  } else {
    formData.categoryId = null                              // 新增时清空分类
    formData.amount = null                                  // 新增时清空金额
  }
  dialogVisible.value = true                                // 显示弹窗
}

/**
 * 提交预算设置/更新
 * → 调用 api/budget.js 的 saveBudget({ categoryId, amount, month })
 */
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false) // 触发校验
  if (!valid) return                                        // 校验不通过不提交

  submitting.value = true                                   // 开启提交loading
  try {
    await saveBudget({                                      // 调用保存预算API
      categoryId: formData.categoryId,                      // 分类ID参数
      amount: formData.amount,                              // 预算金额参数
      month: selectedMonth.value  // "YYYY-MM" 格式，对齐后端 BudgetRequest @Pattern 校验
    })
    ElMessage.success(isEdit.value ? '更新成功' : '设置成功') // 成功提示
    dialogVisible.value = false                             // 关闭弹窗
    await loadData()                                        // 刷新进度数据（await 确保异常可追踪）
  } catch (e) {
    log.warn('保存预算失败:', e) /* 开发环境日志 */
    if (e.code === 'ERR_NETWORK' || e.code === 'ECONNABORTED') {  // 网络或超时错误
      ElMessage.error('网络异常，预算保存失败')              // 网络级错误提示
    }
  } finally {
    submitting.value = false                                // 关闭提交loading
  }
}

/**
 * 删除预算
 * → 调用 api/budget.js 的 deleteBudget(id)
 */
async function handleDeleteBudget(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.categoryName}」的预算吗？`, '确认删除', { // 删除确认
      type: 'warning'                                       // 警告类型
    })
    deletingId.value = row.id                               // 标记正在删除的行
    await deleteBudget(row.id)                              // 调用删除API
    ElMessage.success('预算已删除')                          // 成功提示
    await loadData()                                        // 刷新进度数据（await 确保异常可追踪）
  } catch (e) {
    // P1-9 修复(Q-CR Loop2):反转判断逻辑,只在非用户取消场景才记录错误日志
    // 用户取消(cancel/close)是正常交互流程,不应作为错误处理 — 写日志会污染监控信号
    if (e !== 'cancel' && e !== 'close') {                       // 非用户取消(真实业务/网络错误)
      // 业务错误已被 axios 拦截器(api/request.js)统一弹窗,此处仅记日志便于排查
      log.error('删除预算失败:', e)                                // 记录真实错误日志
    }
  } finally {
    deletingId.value = null                                 // 重置删除标记
  }
}

// 页面挂载时加载分类选项和预算进度（async+Promise.all 并行加载）
onMounted(async () => {
  try {
    await Promise.all([                                        // 并行加载分类选项+预算进度
      loadCategories(),                                        // 加载分类选项
      loadData()                                               // 加载预算进度
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
