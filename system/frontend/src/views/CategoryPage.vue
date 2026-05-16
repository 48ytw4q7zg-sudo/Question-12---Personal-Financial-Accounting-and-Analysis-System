<template>
  <div class="category-page">
    <h2>分类浏览</h2>

    <el-card shadow="hover">
      <el-tabs v-model="activeTab">
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
import { getCategoryList } from '../api/category'

const loading = ref(false)
const activeTab = ref('expense')
const categories = ref([])

const expenseCategories = computed(() =>
  categories.value.filter(item => item.type === 1)
)

const incomeCategories = computed(() =>
  categories.value.filter(item => item.type === 2)
)

async function loadCategories() {
  loading.value = true
  try {
    const data = await getCategoryList()
    categories.value = data || []
  } finally {
    loading.value = false
  }
}

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
