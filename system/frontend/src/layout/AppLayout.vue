<!--
  应用主布局组件（Layout）
  职责：提供应用的整体布局框架（顶栏 + 侧栏 + 内容区），处理响应式和退出登录

  布局结构（对齐 TECH_DESIGN.md §6.0）：
    el-container
    ├── el-header        顶栏（系统名称 + 用户名 + 退出按钮）
    └── el-container
        ├── el-aside     侧栏导航（≥768px 桌面端）
        ├── el-drawer    抽屉导航（<768px 移动端）
        └── el-main      内容区（<router-view /> 渲染子路由页面）

  响应式断点（对齐 TECH_DESIGN.md §6.0）：
    ≥992px   → 侧栏展开 200px
    768-991px → 侧栏折叠 64px
    <768px    → 侧栏隐藏，改用 el-drawer 抽屉

  调用关系：
    → 调用 stores/user.js 的 username（显示用户名）、clearUser()（退出登录）
    → 调用 components/SidebarMenu.vue（侧栏菜单组件）
-->
<template>
  <el-container class="app-layout">
    <!-- 顶栏：系统名称 + 用户信息 + 退出按钮 -->
    <el-header class="app-header">
      <div class="header-left">
        <!-- 移动端：点击打开抽屉菜单 -->
        <el-icon v-if="isMobile" class="menu-toggle" @click="drawerVisible = true" :size="20">
          <Expand />
        </el-icon>
        <!-- 桌面端：点击切换侧栏折叠/展开 -->
        <el-icon v-else class="menu-toggle" @click="toggleSidebar" :size="20">
          <Fold v-if="!isCollapsed" />
          <Expand v-else />
        </el-icon>
        <span class="system-name">个人财务记账与分析系统</span>
      </div>
      <div class="header-right">
        <!-- 显示当前登录用户名（→ 调用 stores/user.js 的 username） -->
        <span class="username">{{ userStore.username }} <el-tag :type="userStore.role === ROLE_ADMIN ? 'warning' : 'info'" size="small">{{ ROLE_LABELS[userStore.role] || '普通用户' }}</el-tag></span>
        <!-- 退出登录按钮 -->
        <el-button type="danger" link @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          退出登录
        </el-button>
      </div>
    </el-header>

    <el-container class="app-body">
      <!-- 桌面端侧栏（≥768px）：宽度随折叠状态变化 -->
      <el-aside v-if="!isMobile" :width="isCollapsed ? '64px' : '200px'" class="app-aside">
        <sidebar-menu :active-menu="activeMenu" :collapsed="isCollapsed" />
      </el-aside>

      <!-- 移动端抽屉（<768px）：从左侧滑出 -->
      <el-drawer
        v-model="drawerVisible"
        direction="ltr"
        size="200px"
        :with-header="false"
        class="mobile-drawer"
      >
        <!-- 点击菜单项后自动关闭抽屉（@navigate → drawerVisible = false） -->
        <sidebar-menu :active-menu="activeMenu" :collapsed="false" @navigate="drawerVisible = false" />
      </el-drawer>

      <!-- 内容区：渲染子路由对应的页面组件 -->
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ROLE_ADMIN, ROLE_LABELS } from '../constants/role'
import SidebarMenu from '../components/SidebarMenu.vue'

const route = useRoute()
const router = useRouter()
// → 调用 stores/user.js：读取用户名、清除用户信息
const userStore = useUserStore()
const isCollapsed = ref(false)        // 侧栏折叠状态
const drawerVisible = ref(false)      // 移动端抽屉显隐

// 当前激活菜单项 = 当前路由 path（自动高亮对应菜单）
const activeMenu = computed(() => route.path)

// 响应式：窗口宽度 < 768px 时为移动端模式
const isMobile = ref(false)

/** 切换侧栏折叠/展开状态 */
function toggleSidebar() {
  isCollapsed.value = !isCollapsed.value
}

/**
 * 退出登录
 * 逻辑：清 localStorage token → 清 userStore → 跳 /login
 * → 调用 stores/user.js 的 clearUser()
 */
function handleLogout() {
  localStorage.removeItem('token')
  userStore.clearUser()
  router.push('/login')
}

/**
 * 窗口大小变化处理（对齐 TECH_DESIGN.md §6.0 响应式断点）
 *   <768px   → isMobile=true，隐藏桌面侧栏，启用抽屉
 *   768-991px → 自动折叠侧栏为 64px
 *   ≥992px   → 展开侧栏为 200px
 */
function handleResize() {
  const w = window.innerWidth
  isMobile.value = w < 768
  if (w >= 768 && w < 992) {
    isCollapsed.value = true
  } else if (w >= 992) {
    isCollapsed.value = false
  }
}

// 组件挂载时初始化响应式状态 + 监听窗口 resize
onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize)
})

// 组件卸载时移除 resize 监听，防止内存泄漏
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.app-layout {
  height: 100vh;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #409eff;
  color: #fff;
  padding: 0 20px;
  height: 60px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.menu-toggle {
  cursor: pointer;
  color: #fff;
}

.system-name {
  font-size: 18px;
  font-weight: bold;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.username {
  font-size: 14px;
}

.app-body {
  height: calc(100vh - 60px);
}

.app-aside {
  background-color: #fff;
  border-right: 1px solid #e6e6e6;
  transition: width 0.3s;
  overflow: hidden;
}

.app-main {
  background-color: #f5f7fa;
  overflow-y: auto;
  padding: 20px;
}

/* 移动端抽屉：去掉默认内边距 */
.mobile-drawer :deep(.el-drawer__body) {
  padding: 0;
}
</style>
