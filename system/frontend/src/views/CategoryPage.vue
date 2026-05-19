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
        <el-date-picker
          v-model="selectedMonth"
          type="month"
          placeholder="选择月份"
          format="YYYY-MM"
          value-format="YYYY-MM"
          @change="loadSummary"
        />
      </div>

      <!-- 支出/收入 Tab 切换 -->
      <el-tabs v-model="activeTab">
        <!-- 支出分类 Tab（type=1 的分类） -->
        <el-tab-pane label="支出分类" name="expense">
          <el-table :data="expenseCategoriesWithAmount" v-loading="loading" stripe>
            <el-table-column prop="name" label="分类名称" min-width="150" />
            <el-table-column prop="type" label="类型" width="100">
              <template #default>
                <el-tag type="danger">支出</el-tag>
              </template>
            </el-table-column>
            <!-- P0-6: 各分类本月消费金额 -->
            <el-table-column prop="monthAmount" label="本月消费" width="150" align="right">
              <template #default="{ row }">
                <span class="amount-text expense">
                  ¥{{ row.monthAmount != null ? row.monthAmount.toFixed(2) : '0.00' }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <!-- 收入分类 Tab（type=2 的分类） -->
        <el-tab-pane label="收入分类" name="income">
          <el-table :data="incomeCategoriesWithAmount" v-loading="loading" stripe>
            <el-table-column prop="name" label="分类名称" min-width="150" />
            <el-table-column prop="type" label="类型" width="100">
              <template #default>
                <el-tag type="success">收入</el-tag>
              </template>
            </el-table-column>
            <!-- P0-6: 各分类本月收入金额 -->
            <el-table-column prop="monthAmount" label="本月收入" width="150" align="right">
              <template #default="{ row }">
                <span class="amount-text income">
                  ¥{{ row.monthAmount != null ? row.monthAmount.toFixed(2) : '0.00' }}
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
import { ref, computed, onMounted } from 'vue'
// → 调用 api/category.js 的 getCategoryList()
import { getCategoryList } from '../api/category'
// → 调用 api/statistics.js 的 getCategorySummary()（P0-6 本月消费金额）
import { getCategorySummary } from '../api/statistics'

const loading = ref(false)
const activeTab = ref('expense')     // 当前激活的 Tab：'expense' 或 'income'
const categories = ref([])           // 全部分类数据（后端返回的完整列表）
const summaryData = ref([])          // 本月各分类消费金额汇总（category-summary API 返回）
const selectedMonth = ref('')        // 选中的月份（YYYY-MM 格式）

// 计算属性：筛选支出分类（type=1）并关联本月消费金额
const expenseCategoriesWithAmount = computed(() =>
  categories.value
    .filter(item => item.type === 1)
    .map(cat => {
      const summary = summaryData.value.find(s => s.categoryId === cat.id)
      return { ...cat, monthAmount: summary ? summary.totalAmount : 0 }
    })
)

// 计算属性：筛选收入分类（type=2）并关联本月收入金额
const incomeCategoriesWithAmount = computed(() =>
  categories.value
    .filter(item => item.type === 2)
    .map(cat => {
      const summary = summaryData.value.find(s => s.categoryId === cat.id)
      return { ...cat, monthAmount: summary ? summary.totalAmount : 0 }
    })
)

/**
 * 加载分类列表
 * → 调用 api/category.js 的 getCategoryList()
 * 返回的数组包含收入和支出两种类型，前端用 computed 分别筛选
 */
async function loadCategories() {
  loading.value = true
  try {
    const data = await getCategoryList()
    categories.value = data || []
  } finally {
    loading.value = false
  }
}

/**
 * 加载本月各分类消费金额汇总（P0-6 核心功能）
 * → 调用 api/statistics.js 的 getCategorySummary()
 * 参数：year, month（从 selectedMonth 解析）, type（为空时返回全部）
 * 返回：[{ categoryId, categoryName, totalAmount, transactionCount }, ...]
 */
async function loadSummary() {
  if (!selectedMonth.value) return
  const [year, month] = selectedMonth.value.split('-')
  try {
    // 不传 type 参数，同时获取支出和收入的汇总数据
    const data = await getCategorySummary({ year: parseInt(year), month: parseInt(month) })
    summaryData.value = data || []
  } catch (e) {
    // 无数据时 summaryData 保持空数组，页面显示 0.00
    summaryData.value = []
  }
}

// 页面挂载时加载分类列表 + 本月汇总
onMounted(() => {
  // 初始化默认当月
  const now = new Date()
  selectedMonth.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  loadCategories()
  loadSummary()
})
</script>

<style scoped>
.category-page h2 {
  margin-bottom: 20px;
  color: #303133;
}
.month-selector {
  margin-bottom: 16px;
}
.amount-text {
  font-weight: 600;
  font-family: 'Courier New', monospace;
}
.amount-text.expense {
  color: #f56c6c;
}
.amount-text.income {
  color: #67c23a;
}
</style>
