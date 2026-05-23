<!--
  应用主布局组件（layout/AppLayout.vue）

  职责：提供整个应用的 UI 骨架——顶栏（header）+ 侧栏导航（aside/drawer）+ 内容区（main）
  作为 Vue Router 中所有业务页面的父路由组件（路由表中 path="/" 的 component）

  布局结构（DOM 树，对齐 TECH_DESIGN.md §6.0 AppLayout 原型描述）：
    el-container.app-layout（flex 列方向，高度 100vh）
    ├── el-header.app-header（高度 60px，flex 行方向，fixed 定位）
    │   ├── .header-left          — 菜单切换按钮 + 系统名称
    │   │   ├── el-icon 菜单按钮  — 桌面端切换折叠 / 移动端打开抽屉
    │   │   └── span 系统名称     — "个人财务记账与分析系统"
    │   └── .header-right         — 用户信息
    │       ├── span 用户名       — 来自 userStore.username
    │       └── el-button 退出    — 点击触发 handleLogout()
    └── el-container.app-body（flex 行方向，剩余高度 calc(100vh - 60px)）
        ├── el-aside（桌面端侧栏，>=768px 显示）
        │   └── SidebarMenu 组件  — 11 个导航菜单项（→ components/SidebarMenu.vue）
        ├── el-drawer（移动端抽屉，<768px 显示）
        │   └── SidebarMenu 组件  — 同上，点击菜单项后自动关闭抽屉
        └── el-main（内容区，背景色浅灰 var(--el-fill-color-light)）
            ├── el-breadcrumb 面包屑导航（首页 / 当前页面名）
            └── router-view     — Vue Router 的渲染出口，显示子路由对应的页面组件

  响应式断点（对齐 TECH_DESIGN.md §6.0 AppLayout 响应式设计，三断点策略）：
    >= 992px   → 侧栏展开 200px，isCollapsed=false
    768-991px  → 侧栏折叠 64px，isCollapsed=true（仅显示图标，文字隐藏）
    < 768px    → 侧栏完全隐藏，改用 el-drawer 抽屉菜单（移动端体验）

  resize 防抖处理：
    窗口 resize 事件使用 200ms debounce 防抖，避免高频触发导致性能抖动和响应式变量频繁更新

  挂载/卸载生命周期管理：
    onMounted → 初始化响应式状态 + 注册 resize 事件监听
    onUnmounted → 移除 resize 监听 + 清除防抖计时器 + 重置挂载标志（防止内存泄漏 + 操作已销毁变量）

  调用关系（被哪些文件引用 / 调用了哪些文件）：
    被引用方：
      → router/index.js：路由表中 path="/" 的 component 指向本文件
    调用方（本文件引用）：
      → stores/user.js：读取 username（顶栏显示用户名）、调用 clearUser()（退出登录时清除状态）
      → constants/role.js：导入 ROLE_ADMIN + ROLE_LABELS（角色标签显示："管理员"/"普通用户"）
      → components/SidebarMenu.vue：侧栏/抽屉中的导航菜单组件
      → router/index.js：通过 useRoute() 读取当前路由信息（面包屑 + 激活菜单项）
-->
<template>
  <!-- 最外层容器：Element Plus 的 el-container，flexbox 列方向，填满整个视口 -->
  <el-container class="app-layout">
    <!-- ========== 顶栏（header）：固定高度 60px，flex 行方向，左右两端对齐 ========== -->
    <el-header class="app-header">
      <!-- 顶栏左侧：菜单切换按钮 + 系统名称 -->
      <div class="header-left">
        <!-- 移动端（<768px）：点击显示抽屉菜单 → drawerVisible = true -->
        <el-icon v-if="isMobile" class="menu-toggle" @click="drawerVisible = true" :size="20">
          <Expand />                                          <!-- → 展开图标（已从 main.js 全局注册为 Vue 组件） -->
        </el-icon>
        <!-- 桌面端（>=768px）：点击切换侧栏折叠/展开状态 → toggleSidebar() -->
        <el-icon v-else class="menu-toggle" @click="toggleSidebar" :size="20">
          <Fold v-if="!isCollapsed" />                        <!-- 侧栏展开时 → 显示折叠图标（点击收缩） -->
          <Expand v-else />                                   <!-- 侧栏折叠时 → 显示展开图标（点击展开） -->
        </el-icon>
        <!-- 系统名称（始终显示在顶栏最左侧，18px 加粗白色文字） -->
        <span class="system-name">个人财务记账与分析系统</span>
      </div>

      <!-- 顶栏右侧：当前登录用户名 + 角色标签 + 退出登录按钮 -->
      <div class="header-right">
        <!-- 显示用户名（来自 Pinia userStore.username）+ 角色标签（管理员/普通用户） -->
        <!-- ROLE_LABELS 对象：{ 0: '普通用户', 1: '管理员' }，来自 constants/role.js -->
        <span class="username">
          {{ userStore.username }}
          <el-tag :type="userStore.role === ROLE_ADMIN ? 'warning' : 'info'" size="small">
            <!-- 管理员用橙色 warning 标签，普通用户用灰色 info 标签 -->
            {{ ROLE_LABELS[userStore.role] || '普通用户' }}
          </el-tag>
        </span>
        <!-- 退出登录按钮（红色 danger 链接样式）→ 调用 handleLogout() -->
        <el-button type="danger" link @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>                 <!-- → 切换图标（退出按钮视觉符号） -->
          退出登录
        </el-button>
      </div>
    </el-header>

    <!-- ========== 主体区域（header 下方剩余空间） ========== -->
    <el-container class="app-body">
      <!-- 桌面端侧栏（>=768px）：宽度随折叠状态动态变化，transition 0.3s 平滑动效 -->
      <el-aside v-if="!isMobile" :width="isCollapsed ? '64px' : '200px'" class="app-aside" aria-label="侧栏导航">
        <!-- SidebarMenu 接收 activeMenu（当前路径）和 collapsed（是否折叠）两个 props -->
        <sidebar-menu :active-menu="activeMenu" :collapsed="isCollapsed" />
      </el-aside>

      <!-- 移动端抽屉（<768px）：从左侧滑出（direction="ltr"），无自带头部（with-header=false） -->
      <!-- v-model 双向绑定显示状态 · direction=ltr 从左滑出 · size=200px 固定宽度 · with-header=false 隐藏自带标题头 -->
      <el-drawer
        v-model="drawerVisible"
        direction="ltr"
        size="200px"
        :with-header="false"
        class="mobile-drawer"
      >
        <!-- 移动端菜单点击后 @navigate 事件关闭抽屉（drawerVisible = false） -->
        <sidebar-menu :active-menu="activeMenu" :collapsed="false" @navigate="drawerVisible = false" />
      </el-drawer>

      <!-- ========== 内容区（main）：面包屑 + 子路由页面 ========== -->
      <el-main class="app-main">
        <!-- 面包屑导航（Element Plus el-breadcrumb）：显示当前页面层级路径 -->
        <el-breadcrumb separator="/" class="app-breadcrumb">
          <!-- 固定第一级：首页（点击跳回根路径 /） -->
          <el-breadcrumb-item :to="{ path: '/' }">
            <el-icon><HomeFilled /></el-icon> 首页            <!-- → 首页图标 + 文字 -->
          </el-breadcrumb-item>
          <!-- 动态第二级：当前页面名称（从路由 meta.title 读取，如"账户管理"、"收支记录"） -->
          <el-breadcrumb-item v-if="breadcrumbTitle">{{ breadcrumbTitle }}</el-breadcrumb-item>
        </el-breadcrumb>
        <!-- ===== Vue Router 渲染出口 ===== -->
        <!-- 根据当前 URL 匹配子路由，渲染对应的页面组件（如 /account → AccountPage.vue） -->
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
// ========== Vue 依赖导入 ==========
import { ref, computed, onMounted, onUnmounted } from 'vue'   // ref（响应式单值）、computed（计算属性）、onMounted/onUnmounted（生命周期钩子）
import { useRoute, useRouter } from 'vue-router'             // useRoute() 读取当前路由信息、useRouter() 执行编程式导航
import { ElMessageBox } from 'element-plus'                   // 导入确认对话框组件（退出登录时弹出确认框）

// ========== 业务模块导入 ==========
import { useUserStore } from '../stores/user'                 // Pinia 用户状态 store（→ stores/user.js），包含 username / role / clearUser()
import { ROLE_ADMIN, ROLE_LABELS } from '../constants/role'  // 角色常量（→ constants/role.js）：
                                                              //   ROLE_ADMIN = 1（管理员角色值）
                                                              //   ROLE_LABELS = { 0: '普通用户', 1: '管理员' }（角色中文名映射）
import SidebarMenu from '../components/SidebarMenu.vue'       // 侧栏导航菜单组件（→ components/SidebarMenu.vue），接收 props: activeMenu, collapsed

// ========== 路由相关 ==========
const route = useRoute()                                      // 获取当前路由的响应式对象（含 path / query / params / meta 等）
const router = useRouter()                                    // 获取路由实例（用于 push / replace 编程式导航）

// ========== Pinia Store ==========
// 用户状态 store 实例（响应式），读取 username/role 用于顶栏显示，调用 clearUser() 用于退出登录
const userStore = useUserStore()

// ========== 响应式状态 ==========
const isCollapsed = ref(false)        // 侧栏折叠状态（桌面端使用）：true=折叠为64px仅图标，false=展开为200px显示图标+文字
const drawerVisible = ref(false)      // 移动端抽屉显隐状态：true=从左侧滑出，false=关闭
// 面包屑标题：从当前路由 meta.title 动态读取（路由表中已定义各页面中文名，对齐 PRD §5 页面层级）
// 例如路由 /account 的 meta: { title: '账户管理' } → 面包屑显示 "首页 / 账户管理"
const breadcrumbTitle = computed(() => {
  // 过滤出所有带 meta.title 的匹配记录（从根到当前路由的完整链路）
  const matched = route.matched.filter(r => r.meta && r.meta.title)
  // 如果匹配层级 > 1（即有子路由）→ 显示最后一层的 title（当前页面名称）
  // 如果仅 1 层（如首页 / 本身）→ 返回 null 不显示第二级面包屑
  return matched.length > 1 ? matched[matched.length - 1].meta.title : null
})

// 当前激活菜单项：取当前路由的完整 path（自动高亮 SidebarMenu 中对应的菜单项）
// 例如访问 /account → activeMenu 值为 '/account' → SidebarMenu 高亮"账户管理"菜单
const activeMenu = computed(() => route.path)

// ========== 响应式断点（移动端检测） ==========
// 初始化时检测窗口宽度（typeof window !== 'undefined' 防 SSR 报错，虽然本应用是纯客户端渲染但保留安全检查）
const isMobile = ref(typeof window !== 'undefined' && window.innerWidth < 768)

// ========== 方法：切换侧栏折叠/展开 ==========
/** 桌面端切换侧栏折叠状态：展开 → 折叠（64px），折叠 → 展开（200px） */
function toggleSidebar() {
  isCollapsed.value = !isCollapsed.value                      // 切换布尔值，CSS transition 0.3s 提供平滑动效
}

// ========== 方法：退出登录 ==========
/**
 * 退出登录处理
 *
 * 执行流程：
 *   1. 弹出 ElMessageBox 确认对话框（"确定退出登录吗？"）
 *   2. 用户确认 → userStore.clearUser() 清空 Pinia store + 移除 localStorage token
 *   3. router.replace('/login') 跳转到登录页（用 replace 而非 push，使浏览器无法"后退"回已退出页面）
 *   4. 用户取消 → 静默处理（catch 空回调）
 *
 * 关联文件：
 *   - stores/user.js → clearUser()：清空 username/role 状态 + localStorage.removeItem('token')
 *   - router/index.js → beforeEach 守卫：下次访问需登录页面时因无 token 被拦截
 */
function handleLogout() {
  // ElMessageBox.confirm 返回 Promise：确认 → resolve，取消 → reject
  ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
    .then(() => {
      userStore.clearUser()                                   // 调用 Pinia store 的 clearUser()，内部已清除 localStorage token
      router.replace('/login')                                // replace（替换历史记录）而非 push（添加），防止用户后退回已退出页面
    })
    .catch(() => { /* 用户点击取消按钮，不执行任何操作 */ })
}

// ========== 方法：窗口大小变化处理 ==========
/**
 * 响应式布局处理函数
 *
 * 根据窗口宽度更新 isMobile 和 isCollapsed 响应式变量：
 *   <768px    → isMobile=true（隐藏侧栏，启用移动端抽屉）
 *   768-991px → isMobile=false, isCollapsed=true（桌面端折叠模式，仅显示图标）
 *   >=992px   → isMobile=false, isCollapsed=false（桌面端完整模式，200px 显示图标+文字）
 *
 * 对齐 TECH_DESIGN.md §6.0 AppLayout 响应式设计（三断点策略）
 *
 * 注意：resize 事件使用 200ms debounce 防抖（见下方 debouncedResize），避免高频触发导致性能问题
 */
function handleResize() {
  const w = window.innerWidth                                 // 获取当前视口宽度（px 整数，不含滚动条宽度）
  isMobile.value = w < 768                                    // <768px = 移动端模式（启用抽屉）
  if (w >= 768 && w < 992) {
    isCollapsed.value = true                                  // 中等宽度（768-991px）：自动折叠侧栏为 64px
  } else if (w >= 992) {
    isCollapsed.value = false                                 // 大屏（>=992px）：自动展开侧栏为 200px
  }
}

// ========== 防抖相关 ==========
let resizeTimer = null                                        // 防抖计时器 ID（setTimeout 返回值）
let isLayoutMounted = true                                    // 组件挂载状态标志：true=已挂载，false=已卸载

/**
 * 防抖包装的 resize 处理函数
 *
 * 机制：
 *   - 每次 resize 事件触发时清除上一次的 200ms 定时器
 *   - 新定时器在 200ms 后执行 handleResize()
 *   - 这样 200ms 内的连续 resize 只执行最后一次，大幅减少响应式变量更新频率
 *   - isLayoutMounted 标志：组件卸载后回调不再执行（防止操作已销毁的 Vue 响应式变量）
 */
function debouncedResize() {
  if (resizeTimer) clearTimeout(resizeTimer)                  // 清除上一次的 200ms 定时器（防抖核心：重新计时）
  resizeTimer = setTimeout(() => {
    if (!isLayoutMounted) return                              // 组件已卸载 → 跳过处理（防止操作已销毁的响应式变量导致内存警告）
    handleResize()                                            // 200ms 无新 resize 事件 → 执行实际处理函数
  }, 200)                                                     // 防抖延迟 200ms
}

// ========== 生命周期：组件挂载 ==========
onMounted(() => {
  isLayoutMounted = true                                      // 标记组件已挂载
  handleResize()                                              // 立即执行一次响应式状态初始化（设置 isMobile / isCollapsed）
  window.addEventListener('resize', debouncedResize)          // 注册 resize 事件监听（使用防抖版本，200ms 内只触发一次）
})

// ========== 生命周期：组件卸载 ==========
onUnmounted(() => {
  isLayoutMounted = false                                     // 标记组件已卸载（阻止进行中的 debounce 回调继续操作响应式变量）
  window.removeEventListener('resize', debouncedResize)       // 移除 resize 事件监听，防止内存泄漏（事件监听器残留）
  if (resizeTimer) clearTimeout(resizeTimer)                  // 清除尚未执行的防抖计时器，防止 setTimeout 回调残留
})
</script>

<style scoped>
/* ===== 最外层容器：flex 列方向，填满整个视口 ===== */
.app-layout {
  height: 100vh;                    /* 占满整个浏览器视口高度，内部通过 flex 划分顶栏和主体 */
}

/* ===== 顶栏：60px 高度，flex 行方向，两端对齐，Element Plus 主色背景 ===== */
.app-header {
  display: flex;                    /* flex 布局 */
  align-items: center;              /* 垂直居中 */
  justify-content: space-between;   /* 左右两端对齐（左侧菜单+名称，右侧用户名+退出按钮） */
  background-color: var(--el-color-primary); /* Element Plus 主题主色（#409EFF 蓝色） */
  color: #fff;                      /* 文字白色 */
  padding: 0 20px;                  /* 左右 20px 内边距（上下为 0） */
  height: 60px;                     /* 固定高度 60px */
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1); /* 底部阴影：增强层次感，顶栏悬浮于主体之上 */
}

/* ===== 顶栏左侧：菜单按钮 + 系统名称 ===== */
.header-left {
  display: flex;                    /* flex 布局，使菜单按钮和名称水平排列 */
  align-items: center;              /* 垂直居中 */
  gap: 12px;                        /* 菜单按钮和名称之间 12px 间距 */
}

/* 菜单切换按钮（桌面端折叠/展开 + 移动端抽屉） */
.menu-toggle {
  cursor: pointer;                  /* 鼠标悬停时显示手型 */
  color: #fff;                      /* 白色图标 */
}

/* 系统名称文字 */
.system-name {
  font-size: 18px;                  /* 18px 字号 */
  font-weight: bold;                /* 加粗 */
}

/* ===== 顶栏右侧：用户名 + 退出按钮 ===== */
.header-right {
  display: flex;                    /* flex 布局，用户名和退出按钮水平排列 */
  align-items: center;              /* 垂直居中 */
  gap: 16px;                        /* 用户名和退出按钮之间 16px 间距 */
}

/* 用户名字体 */
.username {
  font-size: 14px;                  /* 14px 字号 */
}

/* ===== 主体区域 ===== */
.app-body {
  height: calc(100vh - 60px);       /* 视口高度减去顶栏高度 = 剩余可用空间（确保不溢出） */
}

/* ===== 桌面端侧栏 ===== */
.app-aside {
  background-color: #fff;                             /* 白色背景 */
  border-right: 1px solid #e6e6e6;                   /* 右侧淡灰色分隔线 */
  transition: width 0.3s;                             /* 宽度变化时 0.3s 平滑动效（折叠/展开时） */
  overflow: hidden;                                   /* 隐藏溢出内容（折叠时文字需要隐藏） */
}

/* ===== 内容区 ===== */
.app-main {
  background-color: var(--el-fill-color-light);       /* Element Plus 浅色填充背景（#f5f7fa 浅灰） */
  overflow-y: auto;                                   /* 内容超出时可垂直滚动 */
  padding: 20px;                                      /* 20px 内边距 */
}

/* ===== 面包屑导航 ===== */
.app-breadcrumb {
  margin-bottom: 16px;                                /* 面包屑与下方 router-view 之间的间距 */
}

/* ===== 移动端抽屉覆盖样式 ===== */
/* 去掉 el-drawer 默认的 body 内边距（菜单需要贴边显示） */
.mobile-drawer :deep(.el-drawer__body) {
  padding: 0;                       /* :deep() 穿透 scoped 样式，修改 Element Plus 内部组件的默认样式 */
}
</style>
