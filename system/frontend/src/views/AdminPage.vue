<!--
  管理员用户管理页面
  路由: /admin（需管理员 role=1 才能访问）
  对应评分标准: 系统要求 ≥2 类用户角色（普通用户 + 管理员）

  功能说明:
    - 查看所有注册用户列表（id/用户名/角色/注册时间）
    - 删除用户（物理删除 · 二次确认 · 不可删自己）
    - 切换用户角色（普通用户↔管理员 · 不可切换自己）

  权限: 仅管理员（role=1）可访问此页面
  调用关系:
    → 调用 api/admin.js 的 listUsers() / deleteUser() / toggleRole()
-->
<template>
  <!-- v-loading：Element Plus 指令，页面加载中显示遮罩 -->
  <div class="admin-page" v-loading="loading">
    <h2>用户管理（管理员）</h2>
    <!-- el-table stripe border：Element Plus 表格，斑马纹+边框 -->
    <el-table :data="users" stripe border aria-label="用户列表">
      <template #empty><el-empty description="暂无用户数据" /></template>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <!-- 角色列：el-tag 根据不同角色显示不同颜色 -->
      <el-table-column label="角色" width="120">
        <template #default="{ row }">
          <!-- 管理员 danger 红色，普通用户 info 灰色 -->
          <el-tag :type="row.role === ROLE_ADMIN ? 'danger' : 'info'">{{ ROLE_LABELS[row.role] || '普通用户' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>  <!-- formatTime → utils/format.js -->
      </el-table-column>
      <!-- 操作列：切换角色 + 删除用户 -->
      <el-table-column label="操作" width="250">
        <template #default="{ row }">
          <!-- el-button type="warning"：Element Plus 警告色按钮，切换角色 -->
          <!-- :disabled="row.id === userStore.userId || operating"：禁止操作自己 或 操作进行中 -->
          <el-button type="warning" size="small" @click="handleToggleRole(row)"
                     :disabled="row.id === userStore.userId || operating">
            {{ row.role === ROLE_ADMIN ? '降为普通用户' : '提升为管理员' }}  <!-- 按钮文字随角色动态变化 -->
          </el-button>
          <!-- el-button type="danger"：Element Plus 危险色按钮，删除用户 -->
          <el-button type="danger" size="small" @click="handleDelete(row)"
                     :disabled="row.id === userStore.userId || operating">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'                        // 导入Vue组合式API
import { useRouter } from 'vue-router'                      // 导入路由（用于权限不足时跳转）
import { ElMessage, ElMessageBox } from 'element-plus'      // 导入消息提示和确认框
import { listUsers, deleteUser, toggleRole } from '../api/admin' // 导入管理员API
import { useUserStore } from '../stores/user'               // 导入用户状态store
import { formatTime } from '../utils/format'                // 导入时间格式化工具
import { ROLE_ADMIN, ROLE_LABELS } from '../constants/role' // 导入角色常量
import { logger } from '../utils/logger'                    // 导入日志工具

const log = logger('AdminPage')                             // 创建日志实例
const router = useRouter()                                  // 路由实例（用于权限不足时跳转首页）
const userStore = useUserStore()                            // 初始化用户store
const users = ref([])           // 用户列表数据
const loading = ref(true)       // 页面 loading 状态
const operating = ref(false)    // 操作防重复提交 loading

/**
 * 加载所有用户列表
 * → 调用 api/admin.js 的 listUsers()，对应后端 GET /api/admin/users
 * 返回数据格式: [{ id, username, role, createTime }]
 */
async function loadUsers() {
  try {
    users.value = await listUsers() || []                    // 调用API获取用户列表（修复：添加 || [] 避免 null 传入 el-table 导致警告）
  } catch (e) {
    log.error('加载用户列表失败:', e)                          // 记录错误日志
    ElMessage.error('用户列表加载失败，请刷新重试')             // 用户可见错误提示
    users.value = []                                          // 重置为空列表确保页面不崩溃
  } finally {
    loading.value = false                                   // 无论成功失败都关闭loading
  }
}

/**
 * 切换用户角色（管理员↔普通用户）
 * → 调用 api/admin.js 的 toggleRole(userId)，对应后端 PUT /api/admin/users/:userId/role
 * 切换后刷新列表（注意: userStore.role 不会实时更新，需重新登录才生效）
 */
async function handleToggleRole(row) {
  try {
    await ElMessageBox.confirm(                             // 二次确认弹窗
      `确认将用户「${row.username}」${row.role === ROLE_ADMIN ? '降为普通用户' : '提升为管理员'}？角色变更后该用户需重新登录才生效。`, // 确认提示文案
      '角色变更确认',                                        // 确认框标题
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' } // 确认框选项
    )
    operating.value = true                                  // 设置操作中状态
    await toggleRole(row.id)                                // 调用切换角色API
    ElMessage.success(`${row.username} 角色已切换`)          // 成功提示
    await loadUsers()                                       // 刷新用户列表
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {                   // 非取消/非关闭操作才记录错误（ElMessageBox 关闭事件为 'close'）
      log.error('切换角色失败:', e)                           // 记录错误日志
      ElMessage.error('角色切换失败，请重试')                 // 用户可见错误提示
    }
  } finally {
    operating.value = false                                 // 重置操作状态
  }
}

/**
 * 删除用户（二次确认，不可恢复）
 * → 调用 api/admin.js 的 deleteUser(userId)，对应后端 DELETE /api/admin/users/:userId
 * 删除后刷新列表
 */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除用户「${row.username}」？该操作不可恢复。`, '警告', { // 删除确认弹窗
      confirmButtonText: '确认删除',                         // 确认按钮文字
      cancelButtonText: '取消',                              // 取消按钮文字
      type: 'warning'                                        // 警告类型
    })
    operating.value = true                                  // 设置操作中状态
    await deleteUser(row.id)                                // 调用删除用户API
    ElMessage.success('用户已删除')                          // 成功提示
    await loadUsers()                                       // 刷新用户列表
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {                   // 非取消/非关闭操作才记录错误（ElMessageBox 关闭事件为 'close'）
      log.error('删除用户失败:', e)                           // 记录错误日志
      ElMessage.error('用户删除失败，请重试')                 // 用户可见错误提示
    }
  } finally {
    operating.value = false                                 // 重置操作状态
  }
}

// 页面挂载时先做组件级权限二次验证（防JWT篡改绕过路由守卫），再加载用户列表
onMounted(async () => {
  if (userStore.role !== ROLE_ADMIN) {                       // 组件级权限二次验证
    ElMessage.error('无权限访问管理员页面')                   // 权限不足提示
    router.replace('/')                                      // 跳转首页
    return                                                    // 不加载数据
  }
  await loadUsers()                                          // 挂载时加载用户列表（await保证异常可追踪）
})
</script>

<style scoped>
.admin-page h2 {
  margin-bottom: 20px;
  color: var(--color-title);
}
</style>