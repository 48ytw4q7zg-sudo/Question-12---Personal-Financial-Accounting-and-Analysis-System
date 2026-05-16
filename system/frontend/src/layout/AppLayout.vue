<template>
  <el-container class="app-layout">
    <el-header class="app-header">
      <div class="header-left">
        <el-icon v-if="isMobile" class="menu-toggle" @click="drawerVisible = true" :size="20">
          <Expand />
        </el-icon>
        <el-icon v-else class="menu-toggle" @click="toggleSidebar" :size="20">
          <Fold v-if="!isCollapsed" />
          <Expand v-else />
        </el-icon>
        <span class="system-name">个人财务记账与分析系统</span>
      </div>
      <div class="header-right">
        <span class="username">{{ userStore.username }}</span>
        <el-button type="danger" link @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          退出登录
        </el-button>
      </div>
    </el-header>
    <el-container class="app-body">
      <!-- Desktop sidebar: >= 768px -->
      <el-aside v-if="!isMobile" :width="isCollapsed ? '64px' : '200px'" class="app-aside">
        <sidebar-menu :active-menu="activeMenu" :collapsed="isCollapsed" />
      </el-aside>
      <!-- Mobile drawer: < 768px -->
      <el-drawer
        v-model="drawerVisible"
        direction="ltr"
        size="200px"
        :with-header="false"
        class="mobile-drawer"
      >
        <sidebar-menu :active-menu="activeMenu" :collapsed="false" @navigate="drawerVisible = false" />
      </el-drawer>
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
import SidebarMenu from '../components/SidebarMenu.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapsed = ref(false)
const drawerVisible = ref(false)

const activeMenu = computed(() => route.path)

// TECH_DESIGN §6.0: < 768px → mobile drawer mode
const isMobile = ref(false)

function toggleSidebar() {
  isCollapsed.value = !isCollapsed.value
}

function handleLogout() {
  localStorage.removeItem('token')
  userStore.clearUser()
  router.push('/login')
}

// Responsive breakpoint handler
function handleResize() {
  const w = window.innerWidth
  isMobile.value = w < 768
  if (w >= 768 && w < 992) {
    isCollapsed.value = true
  } else if (w >= 992) {
    isCollapsed.value = false
  }
  // < 768px: isMobile = true → drawer; desktop sidebar hidden
}

onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize)
})

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

.aside-menu {
  height: 100%;
  border-right: none;
}

.app-main {
  background-color: #f5f7fa;
  overflow-y: auto;
  padding: 20px;
}

/* Mobile drawer styles */
.mobile-drawer :deep(.el-drawer__body) {
  padding: 0;
}
</style>
