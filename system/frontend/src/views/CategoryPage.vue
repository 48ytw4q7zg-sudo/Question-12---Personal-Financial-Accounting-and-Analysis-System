<!--
  分类浏览页面
  路由：/category
  对应 PRD 功能：P0 分类 GET 列表（种子数据，不做增改删）

  功能说明：
    - Tab 切换支出/收入分类
    - 两个表格分别展示支出分类和收入分类
    - 分类数据由后端种子数据提供，前端仅做展示

  调用关系：
    → 调用 api/category.js 的 getCategoryList()（加载全部分类列表）
-->
<template>
  <div class="category-page">
    <h2>分类浏览</h2>

    <el-card shadow="hover">
      <!-- 支出/收入 Tab 切换 -->
      <el-tabs v-model="activeTab">
        <!-- 支出分类 Tab（type=1 的分类） -->
        <el-tab-pane label="支出分类" name="expense">
          <el-table :data="expenseCategories" v-loading="loading" stripe>
            <el-table-column prop="name" label="分类名称" min-width="150" />
            <el-table-column prop="type" label="类型" width="100">
              <template #default>
                <el-tag type="danger">支出</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <!-- 收入分类 Tab（type=2 的分类） -->
        <el-tab-pane label="收入分类" name="income">
          <el-table :data="incomeCategories" v-loading="loading" stripe>
            <el-table-column prop="name" label="分类名称" min-width="150" />
            <el-table-column prop="type" label="类型" width="100">
              <template #default>
                <el-tag type="success">收入</el-tag>
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

const loading = ref(false)
const activeTab = ref('expense')     // 当前激活的 Tab：'expense' 或 'income'
const categories = ref([])           // 全部分类数据（后端返回的完整列表）

// 计算属性：筛选支出分类（type=1）
const expenseCategories = computed(() =>
  categories.value.filter(item => item.type === 1)
)

// 计算属性：筛选收入分类（type=2）
const incomeCategories = computed(() =>
  categories.value.filter(item => item.type === 2)
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

// 页面挂载时加载分类列表
onMounted(() => {
  loadCategories()
})
</script>

<style scoped>
.category-page h2 {
  margin-bottom: 20px;
  color: #303133;
}
</style>
