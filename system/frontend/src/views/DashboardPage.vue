<template>
  <div class="dashboard-page" v-loading="loading">
    <h2>首页概览</h2>

    <!-- 月度统计卡片 -->
    <el-row :gutter="20" class="summary-cards">
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="income-card">
          <div class="card-content">
            <div class="card-label">月收入</div>
            <div class="card-value income">¥ {{ formatAmount(monthlySummary.income) }}</div>
          </div>
          <el-icon class="card-icon"><TrendCharts /></el-icon>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="expense-card">
          <div class="card-content">
            <div class="card-label">月支出</div>
            <div class="card-value expense">¥ {{ formatAmount(monthlySummary.expense) }}</div>
          </div>
          <el-icon class="card-icon"><Money /></el-icon>
        </el-card>
      </el-col>
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

    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :lg="10">
        <el-card shadow="hover">
          <template #header>支出分类分布</template>
          <div ref="pieChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
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
import { getMonthlySummary, getCategorySummary, getTrend } from '../api/statistics'

const pieChartRef = ref(null)
const lineChartRef = ref(null)
const loading = ref(true)
let pieChart = null
let lineChart = null

const monthlySummary = reactive({
  income: 0,
  expense: 0,
  balance: 0
})

function formatAmount(val) {
  return Number(val || 0).toFixed(2)
}

function getCurrentYearMonth() {
  const now = new Date()
  return {
    year: now.getFullYear(),
    month: now.getMonth() + 1
  }
}

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

async function loadCategoryChart() {
  const { year, month } = getCurrentYearMonth()
  try {
    const data = await getCategorySummary({ year, month, type: 1 })
    if (data && data.length > 0) {
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

async function loadTrendChart() {
  const { year } = getCurrentYearMonth()
  try {
    const data = await getTrend({ year })
    if (data && data.length > 0) {
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

function handleResize() {
  pieChart?.resize()
  lineChart?.resize()
}

onMounted(async () => {
  await Promise.all([loadMonthlySummary(), loadCategoryChart(), loadTrendChart()])
  loading.value = false
  window.addEventListener('resize', handleResize)
})

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
