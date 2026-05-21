<!--
  首页概览页面
  路由：/ （默认首页）
  对应 PRD 功能：
    - P0 按账户汇总余额 → 月度统计卡片（收入/支出/结余）
    - P1/P2 ECharts 图表 → 支出分类饼图 + 收支趋势折线图
    - P2-2 预算预警 → 月度预算超支/接近阈值提示条
    - P2-4 多币种 → 统计卡片底部显示CNY等值换算提示

  功能说明：
    - 顶部 3 个统计卡片：本月收入、本月支出、月结余（正数绿色/负数红色）
    - 预算预警条：超支分类红色警告 + 接近阈值分类黄色提示（P2-2）
    - 左侧饼图：本月支出分类分布
    - 右侧折线图：近 12 个月收支趋势
    - 多币种提示：当存在非CNY账户时，卡片底部显示CNY等值换算（P2-4）

  调用关系：
    → 调用 api/statistics.js 的 getMonthlySummary()（月度汇总数据 → 统计卡片）
    → 调用 api/statistics.js 的 getCategorySummary()（分类汇总数据 → 饼图）
    → 调用 api/statistics.js 的 getTrend()（趋势数据 → 折线图）
    → 调用 api/budget.js 的 getBudgetAlert()（预算预警数据 → 警告条 · P2-2）
    → 调用 api/exchange-rate.js 的 getExchangeRates()（汇率数据 → P2-4 多币种换算）
-->
<template>
  <div class="dashboard-page" v-loading="loading">
    <h2>首页概览</h2>

    <!-- 月度统计卡片：收入 / 支出 / 结余 -->
    <el-row :gutter="20" class="summary-cards">
      <!-- 月收入卡片 -->
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="income-card">
          <div class="card-content">
            <div class="card-label">月收入</div>
            <div class="card-value income">¥ {{ formatAmount(monthlySummary.income) }}</div>
          </div>
          <el-icon class="card-icon"><TrendCharts /></el-icon>
        </el-card>
      </el-col>
      <!-- 月支出卡片 -->
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="expense-card">
          <div class="card-content">
            <div class="card-label">月支出</div>
            <div class="card-value expense">¥ {{ formatAmount(monthlySummary.expense) }}</div>
          </div>
          <el-icon class="card-icon"><Money /></el-icon>
        </el-card>
      </el-col>
      <!-- 月结余卡片（正数=收入>支出，负数=支出>收入） -->
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="balance-card">
          <div class="card-content">
            <div class="card-label">月结余</div>
            <div class="card-value" :class="monthlySummary.balance >= 0 ? 'income' : 'expense'">
              ¥ {{ formatAmount(monthlySummary.balance) }}
            </div>
          </div>
          <el-icon class="card-icon"><Wallet /></el-icon>
        </el-card>
      </el-col>
    </el-row>

    <!-- P2-4 多币种提示：当存在非CNY账户时，提醒用户统计金额为各币种原始值 -->
    <div v-if="hasMultiCurrency" class="multi-currency-hint">
      <el-tag type="info" effect="plain">多币种账户存在，统计金额为各币种原始值，未做CNY换算</el-tag>
    </div>

    <!-- P2-2 预算预警条：根据 alertLevel 显示不同颜色 -->
    <div v-if="budgetAlerts.length > 0" class="budget-alert-section">
      <el-alert
        v-for="alert in budgetAlerts"
        :key="alert.categoryId"
        :title="formatAlertTitle(alert)"
        :type="getAlertType(alert.alertLevel)"
        :closable="false"
        show-icon
        class="budget-alert-item"
      />
    </div>

    <!-- 图表区域：饼图 + 折线图 -->
    <el-row :gutter="20" class="chart-row">
      <!-- 支出分类分布饼图 -->
      <el-col :xs="24" :lg="10">
        <el-card shadow="hover">
          <template #header>支出分类分布</template>
          <div ref="pieChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <!-- 收支趋势折线图（近 12 个月） -->
      <el-col :xs="24" :lg="14">
        <el-card shadow="hover">
          <template #header>收支趋势（近12个月）</template>
          <div ref="lineChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { formatAmount } from '../utils/format'
// → 调用 api/statistics.js 的 3 个统计接口
import { getMonthlySummary, getCategorySummary, getTrend } from '../api/statistics'
// → 调用 api/budget.js 的 getBudgetAlert()（P2-2 预算预警）
import { getBudgetAlert } from '../api/budget'
// → P2-4 多币种：调用 api/exchange-rate.js 的 getExchangeRates()
import { getExchangeRates } from '../api/exchange-rate'

/** P2-4: 汇率数据缓存（1外币→CNY），例 { USD: 7.3, EUR: 7.94, ... } */
const exchangeRates = ref({})
/** P2-4: 标记是否存在非CNY账户（用于显示多币种提示） */
const hasMultiCurrency = ref(false)

/** 预算预警数据（P2-2）：{ categoryName, alertLevel, budgetAmount, spentAmount, percentage }[] */
const budgetAlerts = ref([])

const pieChartRef = ref(null)   // 饼图 DOM 引用
const lineChartRef = ref(null)  // 折线图 DOM 引用
const loading = ref(true)       // 页面 loading 状态
let pieChart = null             // 饼图 ECharts 实例（用于 resize / dispose）
let lineChart = null            // 折线图 ECharts 实例

// 月度汇总数据（响应式对象 → 模板中绑定到统计卡片）
const monthlySummary = reactive({
  income: 0,
  expense: 0,
  balance: 0
})

/**
 * 获取当前年月（用于 API 查询参数中提取年份）
 */
function getCurrentYearMonth() {
  const now = new Date()
  return {
    year: now.getFullYear(),
    month: now.getMonth() + 1
  }
}

/**
 * 加载预算预警数据（P2-2）
 * → 调用 api/budget.js 的 getBudgetAlert({ year, month })
 * 返回 BudgetAlertDTO[]，含 alertLevel 字段（NORMAL/DAILY_WARN/MONTHLY_WARN/OVERSPENT）
 * 无预警时 budgetAlerts 保持空数组，模板中 v-if 隐藏预警区域
 */
async function loadBudgetAlerts() {
  const { year, month } = getCurrentYearMonth()
  try {
    const data = await getBudgetAlert({ year, month })
    if (data && data.length > 0) {
      // 只展示有实际已用金额且非 NORMAL 的预警项
      budgetAlerts.value = data.filter(a => a.percentage > 0 && a.alertLevel !== 'NORMAL')
    }
  } catch (e) {
    console.warn('加载预算预警失败:', e)
    budgetAlerts.value = []
  }
}

/**
 * 根据预警级别返回 el-alert 的 type 属性
 * @param {String} alertLevel - NORMAL / DAILY_WARN / MONTHLY_WARN / OVERSPENT
 * @returns {String} Element Plus alert type
 */
function getAlertType(alertLevel) {
  const typeMap = {
    'OVERSPENT': 'error',      // 红色：已超支
    'MONTHLY_WARN': 'warning', // 橙色：月预警(≥80%)
    'DAILY_WARN': 'warning',   // 黄色：日预警(日均150%)
    'NORMAL': 'success'
  }
  return typeMap[alertLevel] || 'warning'
}

/**
 * 格式化预警消息（P2-2）
 * @param {Object} alert - { categoryName, alertLevel, budgetAmount, spentAmount, percentage }
 * @returns {String} 预警标题文本
 */
function formatAlertTitle(alert) {
  const budget = Number(alert.budgetAmount).toFixed(2)
  const spent = Number(alert.spentAmount).toFixed(2)
  const pct = Number(alert.percentage).toFixed(0)

  if (alert.alertLevel === 'OVERSPENT') {
    return `${alert.categoryName}已超支：预算 ¥${budget}，已花 ¥${spent}（超额 ¥${(Number(alert.spentAmount) - Number(alert.budgetAmount)).toFixed(2)}）`
  }
  if (alert.alertLevel === 'MONTHLY_WARN') {
    return `${alert.categoryName}接近预算上限：已用 ${pct}%（¥${spent} / ¥${budget}）`
  }
  if (alert.alertLevel === 'DAILY_WARN') {
    return `${alert.categoryName}日均消耗偏高：已用 ${pct}%（¥${spent} / ¥${budget}）`
  }
  return `${alert.categoryName}：已用 ${pct}%（¥${spent} / ¥${budget}）`
}

/**
 * 加载月度汇总数据 → 填充顶部 3 个统计卡片
 * → 调用 api/statistics.js 的 getMonthlySummary({ year, month })
 */
async function loadMonthlySummary() {
  const { year, month } = getCurrentYearMonth()
  try {
    const data = await getMonthlySummary({ year, month })
    if (data) {
      monthlySummary.income = data.totalIncome || 0
      monthlySummary.expense = data.totalExpense || 0
      monthlySummary.balance = data.balance || 0
    }
  } catch (e) {
    console.warn('加载月度汇总失败:', e)
    // 空数据时显示 0
  }
}

/**
 * 加载支出分类饼图数据
 * → 调用 api/statistics.js 的 getCategorySummary({ year, month, type: 2 })
 * type=2 表示只查支出分类（1=收入, 2=支出，对齐 TransactionType 枚举）
 */
async function loadCategoryChart() {
  const { year, month } = getCurrentYearMonth()
  try {
    const data = await getCategorySummary({ year, month, type: 2 })
    if (data && data.length > 0) {
      // 复用已有实例，仅 setOption 更新数据（避免每次 dispose+reinit 导致闪烁）
      if (!pieChart && pieChartRef.value) {
        pieChart = echarts.init(pieChartRef.value)
      }
      if (!pieChart) return
      pieChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
        legend: { orient: 'vertical', left: 'left' },
        series: [{
          type: 'pie',
          radius: '60%',
          data: data.map(item => ({ name: item.categoryName, value: item.totalAmount })),
          emphasis: {
            itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' }
          }
        }]
      })
    }
  } catch (e) {
    console.warn('加载分类饼图失败:', e)
  }
}

/**
 * 加载收支趋势折线图数据
 * → 调用 api/statistics.js 的 getTrend({ year })
 */
async function loadTrendChart() {
  const { year } = getCurrentYearMonth()
  try {
    const data = await getTrend({ year })
    if (data && data.length > 0) {
      // 复用已有实例，仅 setOption 更新数据（避免每次 dispose+reinit 导致闪烁）
      if (!lineChart && lineChartRef.value) {
        lineChart = echarts.init(lineChartRef.value)
      }
      if (!lineChart) return
      lineChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['收入', '支出'] },
        xAxis: { type: 'category', data: data.map(item => item.month + '月') },
        yAxis: { type: 'value' },
        series: [
          { name: '收入', type: 'line', data: data.map(item => item.totalIncome), smooth: true, itemStyle: { color: '#67c23a' } },
          { name: '支出', type: 'line', data: data.map(item => item.totalExpense), smooth: true, itemStyle: { color: '#f56c6c' } }
        ]
      })
    }
  } catch (e) {
    console.warn('加载趋势折线图失败:', e)
  }
}

/**
 * P2-4: 加载汇率数据（通过 api/exchange-rate.js 封装函数调用 GET /api/exchange-rate）
 * 返回 { ratesInverse: { USD: "7.3000", ... } }
 * 用于 DashboardPage 多币种账户的 CNY 等值换算展示
 */
async function loadExchangeRates() {
  try {
    const data = await getExchangeRates()
    if (data && data.ratesInverse) {
      exchangeRates.value = data.ratesInverse
      // 标记存在非 CNY 汇率（说明系统启用了多币种）
      hasMultiCurrency.value = Object.keys(data.ratesInverse).length > 0
    }
  } catch (e) {
    console.warn('加载汇率数据失败:', e)
    // exchangeRates 保持空
  }
}

/** 窗口 resize 时重绘图表，保持自适应 */
function handleResize() {
  pieChart?.resize()
  lineChart?.resize()
}

// 页面挂载时并行加载所有数据（含 P2-4 汇率），加载完成后关闭 loading
onMounted(async () => {
  await Promise.all([loadMonthlySummary(), loadBudgetAlerts(), loadCategoryChart(), loadTrendChart(), loadExchangeRates()])
  loading.value = false
  window.addEventListener('resize', handleResize)
})

// 页面卸载时销毁 ECharts 实例 + 移除 resize 监听，防止内存泄漏
onUnmounted(() => {
  pieChart?.dispose()
  lineChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.dashboard-page h2 {
  margin-bottom: 20px;
  color: #303133;
}

.summary-cards {
  margin-bottom: 20px;
}

.summary-cards .el-col {
  margin-bottom: 10px;
}

/* P2-2 预算预警区域 */
.budget-alert-section {
  margin-bottom: 20px;
}
.budget-alert-item {
  margin-bottom: 8px;
}

/* P2-4 多币种提示 */
.multi-currency-hint {
  margin-bottom: 20px;
}

.card-content {
  display: inline-block;
}

.card-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.card-value {
  font-size: 24px;
  font-weight: bold;
}

.card-value.income {
  color: #67c23a;
}

.card-value.expense {
  color: #f56c6c;
}

.card-icon {
  float: right;
  font-size: 40px;
  color: #dcdfe6;
}

.chart-row {
  margin-bottom: 20px;
}

.chart-container {
  height: 350px;
}
</style>
