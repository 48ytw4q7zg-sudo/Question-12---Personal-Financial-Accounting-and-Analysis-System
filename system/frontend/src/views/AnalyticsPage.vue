<template>
  <div class="analytics-page" v-loading="loading">
    <div class="page-header">
      <h2>统计分析</h2>
      <div class="header-actions">
        <el-date-picker
          v-model="selectedYear"
          type="year"
          placeholder="选择年份"
          value-format="YYYY"
          @change="loadAllCharts"
        />
        <el-date-picker
          v-model="selectedMonth"
          type="month"
          placeholder="选择月份"
          value-format="YYYY-MM"
          @change="loadCategoryChart"
        />
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="14">
        <el-card shadow="hover">
          <template #header>月度收支对比（{{ selectedYear }}年）</template>
          <div ref="barChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="hover">
          <template #header>支出分类分布（{{ selectedMonth }}）</template>
          <div ref="pieChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>收支趋势（{{ selectedYear }}年）</template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getTrend, getCategorySummary } from '../api/statistics'

const barChartRef = ref(null)
const pieChartRef = ref(null)
const trendChartRef = ref(null)
const loading = ref(true)
let barChart = null
let pieChart = null
let trendChart = null

const selectedYear = ref(String(new Date().getFullYear()))
const selectedMonth = ref(new Date().toISOString().substring(0, 7))

async function loadBarChart() {
  try {
    const data = await getTrend({ year: Number(selectedYear.value) })
    if (!data || data.length === 0) return

    if (barChart) barChart.dispose()
    barChart = echarts.init(barChartRef.value)
    barChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['收入', '支出'] },
      xAxis: { type: 'category', data: data.map(item => item.month + '月') },
      yAxis: { type: 'value' },
      series: [
        { name: '收入', type: 'bar', data: data.map(item => item.totalIncome), itemStyle: { color: '#67c23a' } },
        { name: '支出', type: 'bar', data: data.map(item => item.totalExpense), itemStyle: { color: '#f56c6c' } }
      ]
    })
  } catch {
    // 静默处理
  }
}

async function loadCategoryChart() {
  try {
    const [year, month] = selectedMonth.value.split('-')
    const data = await getCategorySummary({ year: Number(year), month: Number(month) })
    if (!data || data.length === 0) return

    if (pieChart) pieChart.dispose()
    pieChart = echarts.init(pieChartRef.value)
    pieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
      legend: { orient: 'vertical', left: 'left' },
      series: [{
        type: 'pie',
        radius: '60%',
        data: data.map(item => ({ name: item.categoryName, value: item.totalAmount }))
      }]
    })
  } catch {
    // 静默处理
  }
}

async function loadTrendChart() {
  try {
    const data = await getTrend({ year: Number(selectedYear.value) })
    if (!data || data.length === 0) return

    if (trendChart) trendChart.dispose()
    trendChart = echarts.init(trendChartRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['收入', '支出'] },
      xAxis: { type: 'category', data: data.map(item => item.month + '月') },
      yAxis: { type: 'value' },
      series: [
        { name: '收入', type: 'line', data: data.map(item => item.totalIncome), smooth: true, itemStyle: { color: '#67c23a' } },
        { name: '支出', type: 'line', data: data.map(item => item.totalExpense), smooth: true, itemStyle: { color: '#f56c6c' } }
      ]
    })
  } catch {
    // 静默处理
  }
}

function loadAllCharts() {
  loadBarChart()
  loadTrendChart()
}

function handleResize() {
  barChart?.resize()
  pieChart?.resize()
  trendChart?.resize()
}

onMounted(async () => {
  await Promise.all([loadBarChart(), loadCategoryChart(), loadTrendChart()])
  loading.value = false
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  barChart?.dispose()
  pieChart?.dispose()
  trendChart?.dispose()
  window.removeEventListener('resize', handleResize)
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
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.chart-container {
  height: 350px;
}
</style>
