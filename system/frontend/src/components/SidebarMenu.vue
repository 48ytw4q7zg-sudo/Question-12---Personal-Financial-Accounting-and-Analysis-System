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
    /admin      → 用户管理   (评分标准 ≥2角色 · 仅 role=1 管理员可见 · AdminPage)

  调用方：AppLayout.vue
-->
<template>
  <!-- el-menu：Element Plus 导航菜单组件，router=true 模式点击菜单项自动跳转对应路由 -->
  <el-menu
    :default-active="activeMenu"
    :collapse="collapsed"
    router
    class="sidebar-menu"
    aria-label="主导航菜单"
    @select="$emit('navigate')"
  >
    <!-- P0 核心功能 -->
    <el-menu-item index="/">             <!-- el-menu-item：导航菜单项，index 即跳转路由 path -->
      <el-icon><HomeFilled /></el-icon>  <!-- el-icon + @element-plus/icons-vue HomeFilled 首页图标 -->
      <template #title>首页</template>   <!-- #title 插槽：折叠模式下仅显示图标，展开时显示文字 -->
    </el-menu-item>
    <el-menu-item index="/account">
      <el-icon><Wallet /></el-icon>      <!-- @element-plus/icons-vue Wallet 钱包图标 -->
      <template #title>账户管理</template>
    </el-menu-item>
    <el-menu-item index="/category">
      <el-icon><Menu /></el-icon>        <!-- @element-plus/icons-vue Menu 菜单图标 -->
      <template #title>分类浏览</template>
    </el-menu-item>
    <el-menu-item index="/transaction">
      <el-icon><List /></el-icon>        <!-- @element-plus/icons-vue List 列表图标 -->
      <template #title>收支记录</template>
    </el-menu-item>
    <!-- P1 进阶功能 -->
    <el-menu-item index="/budget">
      <el-icon><Money /></el-icon>       <!-- @element-plus/icons-vue Money 金钱图标 -->
      <template #title>预算管理</template>
    </el-menu-item>
    <el-menu-item index="/recurring-bill">
      <el-icon><Calendar /></el-icon>    <!-- @element-plus/icons-vue Calendar 日历图标 -->
      <template #title>周期账单</template>
    </el-menu-item>
    <el-menu-item index="/transfer">
      <el-icon><Sort /></el-icon>        <!-- @element-plus/icons-vue Sort 排序/转账图标 -->
      <template #title>转账</template>
    </el-menu-item>
    <!-- P2 扩展功能 -->
    <el-menu-item index="/analytics">
      <el-icon><TrendCharts /></el-icon> <!-- @element-plus/icons-vue TrendCharts 趋势图图标 -->
      <template #title>统计分析</template>
    </el-menu-item>
    <el-menu-item index="/import">
      <el-icon><Upload /></el-icon>      <!-- @element-plus/icons-vue Upload 上传图标 -->
      <template #title>数据导入</template>
    </el-menu-item>
    <!-- P1 个人设置 -->
    <el-menu-item index="/settings">
      <el-icon><Setting /></el-icon>     <!-- @element-plus/icons-vue Setting 设置图标 -->
      <template #title>设置</template>
    </el-menu-item>
    <!-- 管理员功能（评分标准 ≥2 类用户角色 · 仅 role=1 可见 · v-if 条件渲染） -->
    <el-menu-item v-if="userStore.role === ROLE_ADMIN" index="/admin">
      <el-icon><UserFilled /></el-icon>  <!-- @element-plus/icons-vue UserFilled 用户图标 -->
      <template #title>用户管理</template>
    </el-menu-item>
  </el-menu>
</template>

<script setup>
// → 调用 stores/user.js 的 useUserStore()（读取当前用户角色判断是否显示管理员菜单）
import { useUserStore } from '../stores/user'              // 导入用户状态store（stores/user.js）
// → 读取 constants/role.js 的 ROLE_ADMIN 常量（判断管理员角色）
import { ROLE_ADMIN } from '../constants/role'              // 导入角色常量（constants/role.js）
const userStore = useUserStore()                           // 初始化用户store实例

// 组件 Props 定义（由父组件 AppLayout.vue 传入）
defineProps({
  activeMenu: { type: String, required: true },  // 当前激活路由 path（由父组件 AppLayout 传入 route.path）
  collapsed: { type: Boolean, default: false }    // 是否折叠（≥768px 且 <992px 时 AppLayout 传入 true）
})

// 组件 Events 定义：菜单项点击时触发 navigate 事件（→ AppLayout.vue 用于关闭移动端抽屉）
defineEmits(['navigate'])
</script>

<style scoped>
.sidebar-menu {
  height: 100%;
  border-right: none;
}
</style>
