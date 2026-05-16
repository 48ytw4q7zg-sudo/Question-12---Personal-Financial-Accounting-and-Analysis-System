<template>
  <el-menu
    :default-active="activeMenu"
    :collapse="collapsed"
    router
    class="sidebar-menu"
    @select="$emit('navigate')"
  >
    <el-menu-item index="/">
      <el-icon><HomeFilled /></el-icon>
      <template #title>首页</template>
    </el-menu-item>
    <el-menu-item index="/account">
      <el-icon><Wallet /></el-icon>
      <template #title>账户管理</template>
    </el-menu-item>
    <el-menu-item index="/category">
      <el-icon><Menu /></el-icon>
      <template #title>分类浏览</template>
    </el-menu-item>
    <el-menu-item index="/transaction">
      <el-icon><List /></el-icon>
      <template #title>收支记录</template>
    </el-menu-item>
    <el-menu-item index="/budget">
      <el-icon><Money /></el-icon>
      <template #title>预算管理</template>
    </el-menu-item>
    <el-menu-item index="/recurring-bill">
      <el-icon><Calendar /></el-icon>
      <template #title>周期账单</template>
    </el-menu-item>
    <el-menu-item index="/transfer">
      <el-icon><Sort /></el-icon>
      <template #title>转账</template>
    </el-menu-item>
    <el-menu-item index="/analytics">
      <el-icon><TrendCharts /></el-icon>
      <template #title>统计分析</template>
    </el-menu-item>
    <el-menu-item index="/import">
      <el-icon><Upload /></el-icon>
      <template #title>数据导入</template>
    </el-menu-item>
    <el-menu-item index="/settings">
      <el-icon><Setting /></el-icon>
      <template #title>设置</template>
    </el-menu-item>
  </el-menu>
</template>

<script setup>
defineProps({
  activeMenu: { type: String, required: true },
  collapsed: { type: Boolean, default: false }
})

defineEmits(['navigate'])
</script>

<style scoped>
.sidebar-menu {
  height: 100%;
  border-right: none;
}
</style>
