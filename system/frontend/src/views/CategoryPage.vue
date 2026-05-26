<!--
  分类浏览页面
  路由：/category
  对应 PRD 功能：P0-6 分类浏览页（展示种子分类列表 + 各分类本月消费金额）

  功能说明：
    - Tab 切换支出/收入分类
    - 两个表格分别展示支出分类和收入分类
    - 分类数据由后端种子数据提供，前端仅做展示
    - 各分类展示本月消费金额（调用 getCategorySummary API）

  调用关系：
    → 调用 api/category.js 的 getCategoryList()（加载全部分类列表）
    → 调用 api/statistics.js 的 getCategorySummary()（加载各分类本月消费金额）
-->
<template>
  <div class="category-page">
    <h2>分类浏览</h2>

    <el-card shadow="hover">
      <!-- 月份选择（默认当月） -->
      <div class="month-selector">
        <!-- el-date-picker：Element Plus 日期选择器，type="month" 月份选择模式 -->
        <el-date-picker
          v-model="selectedMonth"        <!-- 绑定选中月份（YYYY-MM 格式字符串） -->
          type="month"                   <!-- 月选择器类型 -->
          placeholder="选择月份"
          format="YYYY-MM"               <!-- 显示格式 -->
          value-format="YYYY-MM"         <!-- 值格式（绑定到 v-model） -->
          @change="loadSummary"          <!-- 月份变更 → 重新加载汇总数据（→ api/statistics.js） -->
        />
      </div>

      <!-- el-tabs：Element Plus 标签页，切换支出/收入分类 -->
      <el-tabs v-model="activeTab" aria-label="分类类型切换">
        <!-- 支出分类 Tab（展示 category.type === 1 的分类 + 本月消费金额） -->
        <el-tab-pane label="支出分类" name="expense">
          <!-- el-table：Element Plus 表格，数据源=computed 属性 expenseCategoriesWithAmount -->
          <el-table :data="expenseCategoriesWithAmount" v-loading="loading" stripe>
            <template #empty><el-empty description="暂无支出分类" /></template>
            <el-table-column prop="name" label="分类名称" min-width="150" />
            <el-table-column prop="type" label="类型" width="100">
              <template #default>
                <!-- el-tag：Element Plus 标签，type="danger" 红色支出标签 -->
                <el-tag type="danger">支出</el-tag>
              </template>
            </el-table-column>
            <!-- P0-6: 各分类本月消费金额 -->
            <el-table-column prop="monthAmount" label="本月消费" width="150" align="right">
              <template #default="{ row }">
                <span class="amount-text expense">  <!-- expense CSS 类显示红色金额 -->
                  ¥{{ formatAmount(row.monthAmount) }}  <!-- formatAmount → utils/format.js -->
                </span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <!-- 收入分类 Tab（展示 category.type === 2 的分类 + 本月收入金额） -->
        <el-tab-pane label="收入分类" name="income">
          <el-table :data="incomeCategoriesWithAmount" v-loading="loading" stripe>
            <template #empty><el-empty description="暂无收入分类" /></template>
            <el-table-column prop="name" label="分类名称" min-width="150" />
            <el-table-column prop="type" label="类型" width="100">
              <template #default>
                <!-- el-tag：Element Plus 标签，type="success" 绿色收入标签 -->
                <el-tag type="success">收入</el-tag>
              </template>
            </el-table-column>
            <!-- P0-6: 各分类本月收入金额 -->
            <el-table-column prop="monthAmount" label="本月收入" width="150" align="right">
              <template #default="{ row }">
                <span class="amount-text income">  <!-- income CSS 类显示绿色金额 -->
                  ¥{{ formatAmount(row.monthAmount) }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
// ===== Vue 核心库导入 =====
import { ref, computed, onMounted } from 'vue'              // 导入Vue组合式API（ref响应式、computed计算属性、onMounted生命周期）
import { ElMessage } from 'element-plus'                    // 导入Element Plus消息提示组件（用于错误提示）

// ===== 业务API模块导入 =====
// → 调用 api/category.js 的 getCategoryList()（获取全部分类列表）
import { getCategoryList } from '../api/category'             // 导入分类列表API（后端种子数据）
// → 调用 api/statistics.js 的 getCategorySummary()（P0-6 本月各分类消费金额汇总）
import { getCategorySummary } from '../api/statistics'        // 导入分类汇总API（按分类统计金额）

// ===== 工具函数和常量导入 =====
import { formatAmount } from '../utils/format'               // 导入金额格式化工具（分转元、千分位）
import { CATEGORY_TYPE_EXPENSE, CATEGORY_TYPE_INCOME } from '../constants/finance'  // 导入分类类型常量（1=支出，2=收入）
import { logger } from '../utils/logger'                    // 导入统一日志工具

const log = logger('CategoryPage')                          // 创建日志实例
const loading = ref(false)                                  // 页面loading
const activeTab = ref('expense')     // 当前激活的 Tab：'expense' 或 'income'
const categories = ref([])           // 全部分类数据（后端返回的完整列表）
const summaryData = ref([])          // 本月各分类消费金额汇总（category-summary API 返回）
const selectedMonth = ref('')        // 选中的月份（YYYY-MM 格式）

// 预构建 summaryMap（categoryId → totalAmount），避免 O(n*m) 嵌套 find
const summaryMap = computed(() => {
  const map = new Map()                                     // 创建Map映射
  for (const s of summaryData.value) {                      // 遍历汇总数据
    map.set(s.categoryId, s.totalAmount)                    // 分类ID映射到金额
  }
  return map                                                // 返回映射表
})

// 计算属性：筛选支出分类（type=1）并关联本月消费金额（O(n+m) Map查找）
const expenseCategoriesWithAmount = computed(() =>
  categories.value
    .filter(item => item.type === CATEGORY_TYPE_EXPENSE)                        // 筛选支出分类
    .map(cat => ({ ...cat, monthAmount: summaryMap.value.get(cat.id) || 0 })) // 关联本月金额
)

// 计算属性：筛选收入分类（type=2）并关联本月收入金额（O(n+m) Map查找）
const incomeCategoriesWithAmount = computed(() =>
  categories.value
    .filter(item => item.type === CATEGORY_TYPE_INCOME)                        // 筛选收入分类
    .map(cat => ({ ...cat, monthAmount: summaryMap.value.get(cat.id) || 0 })) // 关联本月金额
)

/**
 * 加载分类列表
 * → 调用 api/category.js 的 getCategoryList()
 * 返回的数组包含收入和支出两种类型，前端用 computed 分别筛选
 */
async function loadCategories() {
  try {
    const data = await getCategoryList()                     // 调用分类列表API
    categories.value = data || []                            // 设置分类数据
  } catch (e) {
    log.error('加载分类列表失败:', e)
    throw e                                                  // 向上传播给 onMounted 统一处理 loading 状态
  }
}

/**
 * 加载本月各分类消费金额汇总（P0-6 核心功能）
 * → 调用 api/statistics.js 的 getCategorySummary()
 * 参数：year, month（从 selectedMonth 解析）, type（为空时返回全部）
 * 返回：[{ categoryId, categoryName, totalAmount, transactionCount }, ...]
 */
async function loadSummary() {
  if (!selectedMonth.value) return                           // 未选月份不加载
  const [year, month] = selectedMonth.value.split('-')      // 解析年月
  try {
    // 不传 type 参数，同时获取支出和收入的汇总数据
    const data = await getCategorySummary({ year: parseInt(year), month: parseInt(month) }) // 调用汇总API
    summaryData.value = data || []                           // 设置汇总数据
  } catch (e) {
    log.warn('加载汇总数据失败:', e) // 开发环境日志
    ElMessage.warning('加载汇总数据失败')                     // 降级提示
    summaryData.value = []                                   // 清空汇总数据
  }
}

// 页面挂载时加载分类列表 + 本月汇总（async+Promise.all 并行加载，统一管理 loading 状态）
onMounted(async () => {
  // 初始化默认当月
  const now = new Date()                                     // 获取当前日期
  selectedMonth.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}` // 格式化为YYYY-MM
  loading.value = true                                       // 开启动态 loading（修复：统一管理，避免子函数独立控制导致竞态）
  try {
    await Promise.all([                                        // 并行加载分类列表+汇总数据
      loadCategories(),                                        // 加载分类列表
      loadSummary()                                            // 加载汇总数据
    ])
  } catch (e) {
    log.error('页面初始化加载失败:', e)                          // 记录错误日志
  } finally {
    loading.value = false                                      // 所有数据加载完成后关闭 loading
  }
})
</script>

<style scoped>
.category-page h2 {
  margin-bottom: 20px;
  color: var(--color-title);
}
.month-selector {
  margin-bottom: 16px;
}
.amount-text {
  font-weight: 600;
  font-family: 'Courier New', monospace;
}
.amount-text.expense {
  color: var(--color-expense);
}
.amount-text.income {
  color: var(--color-income);
}
</style>
