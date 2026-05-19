<!--
  统计分析页面
  路由：/analytics
  对应 PRD 功能：P1/P2 ECharts 图表（收支趋势图 + 分类饼图 + 月度对比）

  功能说明：
    - 顶部年份/月份选择器
    - 左侧柱状图：月度收支对比（选定年份的 12 个月收入 vs 支出柱状图）
    - 右侧饼图：支出分类分布（选定月份的支出分类占比）
    - 底部折线图：收支趋势（选定年份的收入/支出趋势曲线）

  调用关系：
    → 调用 api/statistics.js 的 getTrend()（月度趋势数据 → 柱状图 + 折线图）
    → 调用 api/statistics.js 的 getCategorySummary()（分类汇总数据 → 饼图）
-->
<template>
  <div class="analytics-page" v-loading="loading">
    <div class="page-header">
      <h2>统计分析</h2>
      <div class="header-actions">
        <!-- 年份选择器：切换年份后刷新柱状图和折线图 -->
        <el-date-picker
          v-model="selectedYear"
          type="year"
          placeholder="选择年份"
          value-format="YYYY"
          @change="loadAllCharts"
        />
        <!-- 月份选择器：切换月份后刷新分类饼图 -->
        <el-date-picker
          v-model="selectedMonth"
          type="month"
          placeholder="选择月份"
          value-format="YYYY-MM"
          @change="loadCategoryChart"
        />
      </div>
    </div>

    <!-- 上排图表：柱状图 + 饼图 -->
    <el-row :gutter="20">
      <!-- 月度收支对比柱状图 -->
      <el-col :xs="24" :lg="14">
        <el-card shadow="hover">
          <template #header>月度收支对比（{{ selectedYear }}年）</template>
          <div ref="barChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <!-- 支出分类分布饼图 -->
      <el-col :xs="24" :lg="10">
        <el-card shadow="hover">
          <template #header>支出分类分布（{{ selectedMonth }}）</template>
          <div ref="pieChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 下排图表：趋势折线图 -->
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
// → 调用 api/statistics.js 的 getTrend() 和 getCategorySummary()
import { getTrend, getCategorySummary } from '../api/statistics'

// 3 个图表的 DOM 引用
const barChartRef = ref(null)
const pieChartRef = ref(null)
const trendChartRef = ref(null)
const loading = ref(true)

// 3 个 ECharts 实例（用于 resize / dispose）
let barChart = null
let pieChart = null
let trendChart = null

// 当前选中的年份/月份（默认当前年月）
const selectedYear = ref(String(new Date().getFullYear()))
const selectedMonth = ref(new Date().toISOString().substring(0, 7))

/**
 * 加载月度收支对比柱状图
 * → 调用 api/statistics.js 的 getTrend({ year })
 */
async function loadBarChart() {
  try {
    const data = await getTrend({ year: Number(selectedYear.value) })
    if (!data || data.length === 0) return

    // 销毁旧实例再创建新实例（切换年份时需要重建）
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

/**
 * 加载支出分类分布饼图
 * → 调用 api/statistics.js 的 getCategorySummary({ year, month })
 */
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

/**
 * 加载收支趋势折线图
 * → 调用 api/statistics.js 的 getTrend({ year })
 */
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

/** 切换年份时同时刷新柱状图和折线图 */
function loadAllCharts() {
  loadBarChart()
  loadTrendChart()
}

/** 窗口 resize 时重绘所有图表 */
function handleResize() {
  barChart?.resize()
  pieChart?.resize()
  trendChart?.resize()
}

// 页面挂载时并行加载 3 个图表
onMounted(async () => {
  await Promise.all([loadBarChart(), loadCategoryChart(), loadTrendChart()])
  loading.value = false
  window.addEventListener('resize', handleResize)
})

// 页面卸载时销毁所有 ECharts 实例 + 移除 resize 监听
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
