<!--
  首页概览页面
  路由：/ （默认首页）
  对应 PRD 功能：
    - P0 按账户汇总余额 → 月度统计卡片（收入/支出/结余）
    - P1/P2 ECharts 图表 → 支出分类饼图 + 收支趋势折线图

  功能说明：
    - 顶部 3 个统计卡片：本月收入、本月支出、月结余（正数绿色/负数红色）
    - 左侧饼图：本月支出分类分布
    - 右侧折线图：近 12 个月收支趋势

  调用关系：
    → 调用 api/statistics.js 的 getMonthlySummary()（月度汇总数据 → 统计卡片）
    → 调用 api/statistics.js 的 getCategorySummary()（分类汇总数据 → 饼图）
    → 调用 api/statistics.js 的 getTrend()（趋势数据 → 折线图）
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
// → 调用 api/statistics.js 的 3 个统计接口
import { getMonthlySummary, getCategorySummary, getTrend } from '../api/statistics'

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

/** 格式化金额为两位小数 */
function formatAmount(val) {
  return Number(val || 0).toFixed(2)
}

/** 获取当前年月（用于 API 查询参数） */
function getCurrentYearMonth() {
  const now = new Date()
  return {
    year: now.getFullYear(),
    month: now.getMonth() + 1
  }
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
  } catch {
    // 静默处理，空数据时显示 0
  }
}

/**
 * 加载支出分类饼图数据
 * → 调用 api/statistics.js 的 getCategorySummary({ year, month, type: 1 })
 * type=1 表示只查支出分类
 */
async function loadCategoryChart() {
  const { year, month } = getCurrentYearMonth()
  try {
    const data = await getCategorySummary({ year, month, type: 1 })
    if (data && data.length > 0) {
      // 初始化 ECharts 饼图实例
      pieChart = echarts.init(pieChartRef.value)
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
  } catch {
    // 静默处理
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
      // 初始化 ECharts 折线图实例
      lineChart = echarts.init(lineChartRef.value)
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
  } catch {
    // 静默处理
  }
}

/** 窗口 resize 时重绘图表，保持自适应 */
function handleResize() {
  pieChart?.resize()
  lineChart?.resize()
}

// 页面挂载时并行加载所有数据，加载完成后关闭 loading
onMounted(async () => {
  await Promise.all([loadMonthlySummary(), loadCategoryChart(), loadTrendChart()])
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
