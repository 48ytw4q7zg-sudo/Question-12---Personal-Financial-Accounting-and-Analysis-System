<!--
  侧栏导航菜单组件（可复用）
  职责：渲染应用侧栏的菜单导航，使用 el-menu 的 router 模式自动跳转路由

  Props:
    - activeMenu (String, required): 当前激活菜单项（对应路由 path），用于高亮
    - collapsed (Boolean): 是否折叠模式（true 时只显示图标）

  Events:
    - navigate: 点击菜单项时触发（用于移动端关闭抽屉）

  路由映射（对齐 TECH_DESIGN.md §3 路由表 + §6.12 页面路由表）：
    /           → 首页       (PRD P0 DashboardPage)
    /account    → 账户管理   (PRD P0 AccountPage)
    /category   → 分类浏览   (PRD P0 CategoryPage)
    /transaction → 收支记录  (PRD P0 TransactionListPage)
    /budget     → 预算管理   (PRD P1 BudgetPage)
    /recurring-bill → 周期账单 (PRD P1 RecurringBillPage)
    /transfer   → 转账       (PRD P1 TransferPage)
    /analytics  → 统计分析   (PRD P2 AnalyticsPage)
    /import     → 数据导入   (PRD P2 ImportPage)
    /settings   → 设置       (PRD P1 UserSettingsPage)

  调用方：AppLayout.vue
-->
<template>
  <!-- router 模式：点击菜单项自动导航到 index 对应的路由 -->
  <el-menu
    :default-active="activeMenu"
    :collapse="collapsed"
    router
    class="sidebar-menu"
    @select="$emit('navigate')"
  >
    <!-- P0 核心功能 -->
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
    <!-- P1 进阶功能 -->
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
    <!-- P2 扩展功能 -->
    <el-menu-item index="/analytics">
      <el-icon><TrendCharts /></el-icon>
      <template #title>统计分析</template>
    </el-menu-item>
    <el-menu-item index="/import">
      <el-icon><Upload /></el-icon>
      <template #title>数据导入</template>
    </el-menu-item>
    <!-- P1 个人设置 -->
    <el-menu-item index="/settings">
      <el-icon><Setting /></el-icon>
      <template #title>设置</template>
    </el-menu-item>
  </el-menu>
</template>

<script setup>
defineProps({
  activeMenu: { type: String, required: true },  // 当前激活路由 path（由父组件 AppLayout 传入 route.path）
  collapsed: { type: Boolean, default: false }    // 是否折叠（≥768px 且 <992px 时折叠为 64px 宽）
})

defineEmits(['navigate'])
</script>

<style scoped>
.sidebar-menu {
  height: 100%;
  border-right: none;
}
</style>
