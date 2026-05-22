<!--
  统计分析页面
  路由：/analytics
  对应 PRD 功能：P2-1 ECharts 多图联动 + drill-down

  功能说明：
    - 顶部年份/月份选择器
    - 左侧柱状图：月度收支对比（选定年份的 12 个月收入 vs 支出柱状图）
    - 右侧饼图：支出分类分布（选定月份的支出分类占比）
    - 底部折线图：收支趋势（选定年份的收入/支出趋势曲线）
    - P2-1 drill-down：点击图表元素跳转到 TransactionListPage 带筛选参数

  调用关系：
    → 调用 api/statistics.js 的 getTrend()（月度趋势数据 → 柱状图 + 折线图）
    → 调用 api/statistics.js 的 getCategorySummary()（分类汇总数据 → 饼图）
    → 点击柱状图月份/饼图分类/趋势图月份 → router.push('/transaction?...')（drill-down）
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
          <div ref="barChartRef" class="chart-container" role="img" aria-label="月度收支对比柱状图"></div>
        </el-card>
      </el-col>
      <!-- 支出分类分布饼图 -->
      <el-col :xs="24" :lg="10">
        <el-card shadow="hover">
          <template #header>支出分类分布（{{ selectedMonth }}）</template>
          <div ref="pieChartRef" class="chart-container" role="img" aria-label="支出分类分布饼图"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 下排图表：趋势折线图 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>收支趋势（{{ selectedYear }}年）</template>
          <div ref="trendChartRef" class="chart-container" role="img" aria-label="收支趋势折线图"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
// ECharts 按需加载（echarts/core + 所需组件，减小 vendor-echarts chunk 体积 ~1MB → ~350KB）
import { loadEcharts } from '../utils/echarts-lazy'
// → 调用 api/statistics.js 的 getTrend() 和 getCategorySummary()
import { getTrend, getCategorySummary } from '../api/statistics'
import { CHART_COLORS } from '../constants/finance'

// Vue Router 实例（用于 P2-1 drill-down 跳转到 TransactionListPage）
const router = useRouter()

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
const analyticsNow = new Date()
const selectedMonth = ref(`${analyticsNow.getFullYear()}-${String(analyticsNow.getMonth() + 1).padStart(2, '0')}`)

/**
 * P2-1 drill-down 处理：点击柱状图某月 → 跳转 TransactionListPage 带时间筛选
 * @param {Object} params - ECharts click event params
 */
function handleBarClick(params) {
  if (params.componentType === 'series') {
    // 从月份字符串提取月份数字（如 "5月" 或 "2026-05月"），年份使用当前选中年份
    const monthStr = params.name.replace('月', '')
    const parts = monthStr.split('-')
    let year, month
    if (parts.length === 2) {
      // 格式 "2026-05" → year=2026, month=05
      year = parts[0]
      month = parts[1]
    } else {
      // 格式 "5" → 使用 selectedYear 作为年份
      year = selectedYear.value
      month = monthStr
    }
    const startDate = `${year}-${String(month).padStart(2, '0')}-01`
    const endDay = new Date(Number(year), Number(month), 0)
    const endDate = `${year}-${String(month).padStart(2, '0')}-${String(endDay.getDate()).padStart(2, '0')}`
    router.push({ path: '/transaction', query: { startDate, endDate } })
  }
}

/**
 * 加载月度收支对比柱状图
 * → 调用 api/statistics.js 的 getTrend({ year })
 * → P2-1: 绑定 click 事件支持 drill-down
 */
async function loadBarChart() {
  try {
    const data = await getTrend({ year: Number(selectedYear.value) })
    const echartsModule = await loadEcharts()
    if (!barChart && barChartRef.value) {
      barChart = echartsModule.init(barChartRef.value)
    }
    if (!barChart) return
    if (data && data.length > 0) {
      barChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['收入', '支出'] },
        xAxis: { type: 'category', data: data.map(item => item.month + '月') },
        yAxis: { type: 'value' },
        series: [
          { name: '收入', type: 'bar', data: data.map(item => item.totalIncome), itemStyle: { color: CHART_COLORS.income } },
          { name: '支出', type: 'bar', data: data.map(item => item.totalExpense), itemStyle: { color: CHART_COLORS.expense } }
        ]
      }, { notMerge: true })
    } else {
      barChart.setOption({
        title: { text: '暂无月度数据', left: 'center', top: 'center', textStyle: { color: '#999', fontSize: 14 } },
        xAxis: { type: 'category', data: [] },
        yAxis: { type: 'value' },
        series: []
      }, true)
    }
    // P2-1 drill-down: 点击柱状图某月 → 跳转交易列表带时间筛选
    barChart.off('click')
    barChart.on('click', handleBarClick)
  } catch (e) {
    if (import.meta.env.DEV) console.warn('加载柱状图失败:', e)
    ElMessage.warning('月度收支对比图加载失败')
  }
}

/**
 * P2-1 drill-down 处理：点击饼图某分类 → 跳转 TransactionListPage 带分类筛选
 * @param {Object} params - ECharts click event params
 */
function handlePieClick(params) {
  if (params.componentType === 'series') {
    // 从饼图数据中获取 categoryId（需要在 series data 中保留）
    const categoryId = params.data?.categoryId
    if (categoryId) {
      // drill-down 跳转到交易列表页，带分类筛选参数
      const [year, month] = selectedMonth.value.split('-')
      const startDate = `${year}-${month}-01`
      const endDay = new Date(Number(year), Number(month), 0)
      const endDate = `${year}-${month}-${String(endDay.getDate()).padStart(2, '0')}`
      router.push({ path: '/transaction', query: { categoryId, startDate, endDate } })
    }
  }
}

/**
 * 加载支出分类分布饼图
 * → 调用 api/statistics.js 的 getCategorySummary({ year, month, type: 2 })
 * type=2 只查支出分类（1=收入, 2=支出，对齐 TransactionType 枚举）
 * → P2-1: 绑定 click 事件支持 drill-down（点击分类 → 跳转交易列表）
 */
async function loadCategoryChart() {
  try {
    const [year, month] = selectedMonth.value.split('-')
    const data = await getCategorySummary({ year: Number(year), month: Number(month), type: 2 })
    const echartsModule = await loadEcharts()
    if (!pieChart && pieChartRef.value) {
      pieChart = echartsModule.init(pieChartRef.value)
    }
    if (!pieChart) return
    if (data && data.length > 0) {
      pieChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
        legend: { orient: 'vertical', left: 'left' },
        series: [{
          type: 'pie',
          radius: '60%',
          data: data.map(item => ({ name: item.categoryName, value: item.totalAmount, categoryId: item.categoryId }))
        }]
      }, { notMerge: true })
    } else {
      pieChart.setOption({
        title: { text: '暂无分类数据', left: 'center', top: 'center', textStyle: { color: '#999', fontSize: 14 } },
        series: []
      }, true)
    }
    // P2-1 drill-down: 点击饼图某分类 → 跳转交易列表带分类筛选
    pieChart.off('click')
    pieChart.on('click', handlePieClick)
  } catch (e) {
    if (import.meta.env.DEV) console.warn('加载饼图失败:', e)
    ElMessage.warning('分类分布图加载失败')
  }
}

/**
 * 加载收支趋势折线图
 * → 调用 api/statistics.js 的 getTrend({ year })
 * → P2-1: 绑定 click 事件支持 drill-down（点击月份 → 跳转交易列表）
 */
async function loadTrendChart() {
  try {
    const data = await getTrend({ year: Number(selectedYear.value) })
    const echartsModule = await loadEcharts()
    if (!trendChart && trendChartRef.value) {
      trendChart = echartsModule.init(trendChartRef.value)
    }
    if (!trendChart) return
    if (data && data.length > 0) {
      trendChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['收入', '支出'] },
        xAxis: { type: 'category', data: data.map(item => item.month + '月') },
        yAxis: { type: 'value' },
        series: [
          { name: '收入', type: 'line', data: data.map(item => item.totalIncome), smooth: true, itemStyle: { color: CHART_COLORS.income } },
          { name: '支出', type: 'line', data: data.map(item => item.totalExpense), smooth: true, itemStyle: { color: CHART_COLORS.expense } }
        ]
      }, { notMerge: true })
    } else {
      trendChart.setOption({
        title: { text: '暂无趋势数据', left: 'center', top: 'center', textStyle: { color: '#999', fontSize: 14 } },
        xAxis: { type: 'category', data: [] },
        yAxis: { type: 'value' },
        series: []
      }, true)
    }
    // P2-1 drill-down: 点击趋势图某月 → 跳转交易列表带时间筛选
    trendChart.off('click')
    trendChart.on('click', handleBarClick)
  } catch (e) {
    if (import.meta.env.DEV) console.warn('加载趋势图失败:', e)
    ElMessage.warning('趋势图加载失败')
  }
}

/** 切换年份时同时刷新柱状图和折线图（await 确保错误能正确传播） */
async function loadAllCharts() {
  await Promise.allSettled([loadBarChart(), loadTrendChart()])
}

/** 窗口 resize 时重绘所有图表 */
function handleResize() {
  barChart?.resize()
  pieChart?.resize()
  trendChart?.resize()
}

// 页面挂载时并行加载 3 个图表
onMounted(async () => {
  await Promise.allSettled([loadBarChart(), loadCategoryChart(), loadTrendChart()])
  loading.value = false
  window.addEventListener('resize', handleResize)
})

// 页面卸载时移除 resize 监听 + 销毁所有 ECharts 实例（先移除监听再 dispose，避免 race condition）
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
  barChart = null
  pieChart?.dispose()
  pieChart = null
  trendChart?.dispose()
  trendChart = null
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

.chart-container {
  height: 350px;
}
</style>
