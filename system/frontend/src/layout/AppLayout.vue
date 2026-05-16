<template>
  <el-container class="app-layout">
    <el-header class="app-header">
      <div class="header-left">
        <el-icon class="menu-toggle" @click="toggleSidebar" :size="20">
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
      <el-aside :width="isCollapsed ? '64px' : '200px'" class="app-aside">
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapsed"
          router
          class="aside-menu"
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
      </el-aside>
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

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapsed = ref(false)

const activeMenu = computed(() => route.path)

function toggleSidebar() {
  isCollapsed.value = !isCollapsed.value
}

function handleLogout() {
  localStorage.removeItem('token')
  userStore.clearUser()
  router.push('/login')
}

// 响应式：小屏幕自动折叠侧栏
function handleResize() {
  if (window.innerWidth < 768) {
    isCollapsed.value = true
  }
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
</style>
