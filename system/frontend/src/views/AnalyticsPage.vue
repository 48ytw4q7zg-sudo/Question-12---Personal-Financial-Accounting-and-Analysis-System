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
        <!-- el-date-picker type="year"：Element Plus 年份选择器，切换年份后 @change → loadAllCharts() -->
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

    <!-- 上排图表：el-row 栅格布局，柱状图(lg=14) + 饼图(lg=10) -->
    <el-row :gutter="20">
      <!-- 月度收支对比柱状图 -->
      <el-col :xs="24" :lg="14">          <!-- xs=24 手机全宽，lg=14 大屏 14/24 -->
        <el-card shadow="hover">
          <template #header>月度收支对比（{{ selectedYear }}年）</template>
          <!-- ref="barChartRef" → echarts.init(barChartRef.value) 获取DOM容器 -->
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

    <!-- 下排图表：趋势折线图，el-col :span="24" 全宽 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">                 <!-- span=24 占满整行 -->
        <el-card shadow="hover">
          <template #header>收支趋势（{{ selectedYear }}年）</template>
          <div ref="trendChartRef" class="chart-container" role="img" aria-label="收支趋势折线图"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'           // 导入Vue组合式API
import { ElMessage } from 'element-plus'                    // 导入消息提示
import { useRouter } from 'vue-router'                      // 导入路由（用于drill-down跳转）
// ECharts 按需加载（echarts/core + 所需组件，减小 vendor-echarts chunk 体积 ~1MB → ~350KB）
import { loadEcharts } from '../utils/echarts-lazy'         // 导入ECharts懒加载
// → 调用 api/statistics.js 的 getTrend() 和 getCategorySummary()
import { getTrend, getCategorySummary } from '../api/statistics' // 导入统计API
import { CHART_COLORS, TRANSACTION_TYPE_EXPENSE } from '../constants/finance' // 导入图表颜色常量 + 支出类型枚举值(用于分类饼图筛选type=2)
import { logger } from '../utils/logger' // 导入日志工具

const log = logger('AnalyticsPage') // 创建日志实例

// Vue Router 实例（用于 P2-1 drill-down 跳转到 TransactionListPage）
const router = useRouter()                                  // 路由实例

// 3 个图表的 DOM 引用
const barChartRef = ref(null)                               // 柱状图DOM引用
const pieChartRef = ref(null)                               // 饼图DOM引用
const trendChartRef = ref(null)                             // 折线图DOM引用
const loading = ref(true)                                   // 页面loading

// 3 个 ECharts 实例（用于 resize / dispose）
let barChart = null                                         // 柱状图实例
let pieChart = null                                         // 饼图实例
let trendChart = null                                       // 折线图实例

// 当前选中的年份/月份（初始值在 onMounted 中设置，避免模块级日期捕获导致跨天后数据不一致）
const selectedYear = ref('')                                // 当前年份字符串（onMounted 初始化）
const selectedMonth = ref('')                               // 当前月份（onMounted 初始化）

/**
 * P2-1 drill-down 处理：点击柱状图某月 → 跳转 TransactionListPage 带时间筛选
 * @param {Object} params - ECharts click event params
 */
function handleBarClick(params) {
  if (params.componentType === 'series') {
    // 从月份字符串提取月份数字（如 "5月" 或 "2026-05月"），年份使用当前选中年份
    // 防御性解析：从月份字符串中提取数字（去除所有非数字和非连字符字符），兼容多种格式
    const monthStr = String(params.name || '').replace(/[^0-9-]/g, '') // 去除非数字字符（保留连字符）
    const parts = monthStr.split('-').filter(Boolean)                  // 按连字符分割并过滤空串
    let year, month
    if (parts.length === 2 && parts[0] && parts[1]) {
      // 格式 "2026-05" → year=2026, month=05
      year = parts[0]                                       // 解析年份
      month = parts[1]                                      // 解析月份
    } else if (parts.length === 1 && parts[0]) {
      // 格式 "5" → 使用 selectedYear 作为年份
      year = selectedYear.value                              // 使用选中年份
      month = parts[0]                                      // 月份字符串
    } else {
      // 无法解析，记录日志并跳过跳转
      log.warn('无法解析月份:', params.name)                  // 开发环境日志
      return                                                 // 中止跳转
    }
    // 校验年月有效性
    if (!year || !month || isNaN(Number(year)) || isNaN(Number(month))) {
      log.warn('无效的年月值:', year, month)                  // 开发环境日志
      return                                                 // 中止跳转
    }
    const startDate = `${year}-${String(month).padStart(2, '0')}-01` // 月初日期
    const endDay = new Date(Number(year), Number(month), 0) // 月末日期计算
    const endDate = `${year}-${String(month).padStart(2, '0')}-${String(endDay.getDate()).padStart(2, '0')}` // 月末日期
    router.push({ path: '/transaction', query: { startDate, endDate } }) // drill-down跳转
  }
}

/**
 * 渲染月度收支对比柱状图（基于已加载的趋势数据，不再独立请求数据）
 * @param {Array} data - getTrend() 返回的趋势数据 [{month, totalIncome, totalExpense}]
 * → P2-1: 绑定 click 事件支持 drill-down
 */
async function renderBarChart(data) {
  try {
    const echartsModule = await loadEcharts()                // 按需加载ECharts
    // 零维度保护：v-if 隐藏或 display:none 时容器尺寸为 0，init 会报错，跳过初始化
    if (!barChart && barChartRef.value && barChartRef.value.offsetWidth > 0 && barChartRef.value.offsetHeight > 0) {
      barChart = echartsModule.init(barChartRef.value)       // 初始化柱状图
    }
    if (!barChart) return                                    // 无DOM引用退出
    if (data && data.length > 0) {
      barChart.setOption({                                   // 设置柱状图配置
        tooltip: { trigger: 'axis' },                        // 提示框
        legend: { data: ['收入', '支出'] },                  // 图例
        xAxis: { type: 'category', data: data.map(item => item.month + '月') }, // X轴月份
        yAxis: { type: 'value' },                            // Y轴数值
        series: [
          { name: '收入', type: 'bar', data: data.map(item => item.totalIncome), itemStyle: { color: CHART_COLORS.income } }, // 收入柱
          { name: '支出', type: 'bar', data: data.map(item => item.totalExpense), itemStyle: { color: CHART_COLORS.expense } } // 支出柱
        ]
      }, { notMerge: true })                                 // 不合并配置
    } else {
      barChart.setOption({                                   // 空数据占位
        title: { text: '暂无月度数据', left: 'center', top: 'center', textStyle: { color: '#999', fontSize: 14 } }, // 空数据标题
        xAxis: { type: 'category', data: [] },               // 空X轴
        yAxis: { type: 'value' },                            // 空 Y轴
        series: []                                           // 空系列
      }, true)
    }
    // P2-1 drill-down: 点击柱状图某月 → 跳转交易列表带时间筛选
    barChart.off('click')                                    // 移除旧事件
    barChart.on('click', handleBarClick)                     // 绑定drill-down事件
  } catch (e) {
    log.warn('渲染柱状图失败:', e) /* 开发环境日志 */
    ElMessage.warning('柱状图渲染失败，请刷新页面重试')          // Q-CR修复：用户级错误提示
  }
}

/**
 * P2-1 drill-down 处理：点击饼图某分类 → 跳转 TransactionListPage 带分类筛选
 * @param {Object} params - ECharts click event params
 */
function handlePieClick(params) {
  if (params.componentType === 'series') {
    // 从饼图数据中获取 categoryId（需要在 series data 中保留）
    const categoryId = params.data?.categoryId               // 获取分类ID
    if (categoryId) {
      // drill-down 跳转到交易列表页，带分类筛选参数
      const [year, month] = selectedMonth.value.split('-')   // 解析年月
      const startDate = `${year}-${month}-01`                 // 月初日期
      const endDay = new Date(Number(year), Number(month), 0) // 月末日期
      const endDate = `${year}-${month}-${String(endDay.getDate()).padStart(2, '0')}` // 月末日期
      router.push({ path: '/transaction', query: { categoryId, startDate, endDate } }) // drill-down跳转
    }
  }
}

/**
 * 加载支出分类分布饼图
 * → 调用 api/statistics.js 的 getCategorySummary({ year, month, type: TRANSACTION_TYPE_EXPENSE })
 * type=2 只查支出分类（1=收入, 2=支出，对齐 TransactionType 枚举）
 * → P2-1: 绑定 click 事件支持 drill-down（点击分类 → 跳转交易列表）
 */
async function loadCategoryChart() {
  try {
    const [year, month] = selectedMonth.value.split('-')     // 解析年月
    const data = await getCategorySummary({ year: Number(year), month: Number(month), type: TRANSACTION_TYPE_EXPENSE }) // 调用分类汇总API
    const echartsModule = await loadEcharts()                // 按需加载ECharts
    // 零维度保护：v-if 隐藏或 display:none 时容器尺寸为 0，init 会报错，跳过初始化
    if (!pieChart && pieChartRef.value && pieChartRef.value.offsetWidth > 0 && pieChartRef.value.offsetHeight > 0) {
      pieChart = echartsModule.init(pieChartRef.value)       // 初始化饼图
    }
    if (!pieChart) return                                    // 无DOM引用退出
    if (data && data.length > 0) {
      pieChart.setOption({                                   // 设置饼图配置
        tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' }, // 提示框
        legend: { orient: 'vertical', left: 'left' },        // 图例
        series: [{
          type: 'pie',                                       // 饼图类型
          radius: '60%',                                     // 饼图半径
          data: data.map(item => ({ name: item.categoryName, value: item.totalAmount, categoryId: item.categoryId })) // 饼图数据含分类ID
        }]
      }, { notMerge: true })                                 // 不合并配置
    } else {
      pieChart.setOption({                                   // 空数据占位
        title: { text: '暂无分类数据', left: 'center', top: 'center', textStyle: { color: '#999', fontSize: 14 } }, // 空数据标题
        series: []                                           // 系列
      }, true)
    }
    // P2-1 drill-down: 点击饼图某分类 → 跳转交易列表带分类筛选
    pieChart.off('click')                                    // 移除旧事件
    pieChart.on('click', handlePieClick)                     // 绑定drill-down事件
  } catch (e) {
    log.warn('加载饼图失败:', e) // 开发环境日志
    ElMessage.warning('分类分布图加载失败')                   // 降级提示
  }
}

/**
 * 加载趋势数据并渲染柱状图+折线图（复用同一份 getTrend 数据，消除重复 API 调用）
 * 修复：原实现 loadBarChart + loadTrendChart 各调用一次 getTrend({year})，相同参数重复请求
 * → 调用 api/statistics.js 的 getTrend({ year })
 */
async function loadTrendCharts() {
  try {
    const data = await getTrend({ year: Number(selectedYear.value) }) // 一次性获取趋势数据，供柱状图和折线图共用
    await Promise.allSettled([renderBarChart(data), renderTrendChart(data)]) // 并行渲染两个图表（共享同一数据）
  } catch (e) {
    log.warn('加载趋势图失败:', e)
    ElMessage.warning('趋势图加载失败')
  }
}

/** 基于已有数据渲染趋势折线图（不再独立请求数据） */
async function renderTrendChart(data) {
  try {
    const echartsModule = await loadEcharts()                 // 按需加载ECharts
    // 零维度保护：v-if 隐藏或 display:none 时容器尺寸为 0，init 会报错，跳过初始化
    if (!trendChart && trendChartRef.value && trendChartRef.value.offsetWidth > 0 && trendChartRef.value.offsetHeight > 0) {
      trendChart = echartsModule.init(trendChartRef.value)    // 初始化折线图
    }
    if (!trendChart) return                                   // 无DOM引用退出
    if (data && data.length > 0) {
      trendChart.setOption({                                  // 设置折线图配置
        tooltip: { trigger: 'axis' },                         // 提示框
        legend: { data: ['收入', '支出'] },                   // 图例
        xAxis: { type: 'category', data: data.map(item => item.month + '月') }, // X轴月份
        yAxis: { type: 'value' },                             // Y轴数值
        series: [
          { name: '收入', type: 'line', data: data.map(item => item.totalIncome), smooth: true, itemStyle: { color: CHART_COLORS.income } }, // 收入折线
          { name: '支出', type: 'line', data: data.map(item => item.totalExpense), smooth: true, itemStyle: { color: CHART_COLORS.expense } } // 支出折线
        ]
      }, { notMerge: true })                                  // 不合并配置
    } else {
      trendChart.setOption({                                  // 空数据占位
        title: { text: '暂无趋势数据', left: 'center', top: 'center', textStyle: { color: '#999', fontSize: 14 } }, // 空数据标题
        xAxis: { type: 'category', data: [] },                // 空 X轴
        yAxis: { type: 'value' },                             // 空 Y轴
        series: []                                            // 空系列
      }, true)
    }
    // P2-1 drill-down: 点击趋势图某月 → 跳转交易列表带时间筛选
    trendChart.off('click')                                   // 移除旧事件
    trendChart.on('click', handleBarClick)                    // 绑定drill-down事件
  } catch (e) {
    log.warn('渲染趋势图失败:', e) /* 开发环境日志 */
    ElMessage.warning('趋势图渲染失败，请刷新页面重试')          // Q-CR修复：用户级错误提示
  }
}

/** 切换年份时刷新柱状图和折线图（共享一份 getTrend 数据，消除重复请求） */
async function loadAllCharts() {
  await loadTrendCharts() // 修复：合并 loadBarChart+loadTrendChart，消除对同一接口的重复请求
}

/** 窗口 resize 时重绘所有图表（防抖 150ms，避免频繁 resize 导致性能浪费） */
let resizeTimer = null
function handleResize() {
  clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => {
    barChart?.resize()
    pieChart?.resize()
    trendChart?.resize()
  }, 150)
}

// 页面挂载时初始化日期 + 并行加载图表
onMounted(async () => {
  // 修复：在 onMounted 中初始化日期（而非模块级 static 赋值），防止页面 keep-alive 跨天后日期不变
  const now = new Date()
  selectedYear.value = String(now.getFullYear())
  selectedMonth.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  await Promise.allSettled([loadTrendCharts(), loadCategoryChart()]) // 并行加载趋势图+分类饼图（趋势数据一次请求两图共用）
  loading.value = false                                      // 关闭loading
  window.addEventListener('resize', handleResize)            // 监听resize
})

// 页面卸载时移除 resize 监听 + 解绑 ECharts 事件 + 销毁实例（避免内存泄漏）
onUnmounted(() => {
  clearTimeout(resizeTimer)                                   // 清理防抖定时器
  window.removeEventListener('resize', handleResize)         // 移除resize监听

  // 先解绑 click 事件再 dispose，避免 dispose 后事件处理器仍持有 chart 引用导致内存泄漏
  barChart?.off('click')                                     // 解绑柱状图点击事件
  barChart?.dispose()                                        // 销毁柱状图
  barChart = null                                            // 清空柱状图引用

  pieChart?.off('click')                                     // 解绑饼图点击事件
  pieChart?.dispose()                                        // 销毁饼图
  pieChart = null                                            // 清空饼图引用

  trendChart?.off('click')                                   // 解绑折线图点击事件
  trendChart?.dispose()                                      // 销毁折线图
  trendChart = null                                          // 清空折线图引用
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
