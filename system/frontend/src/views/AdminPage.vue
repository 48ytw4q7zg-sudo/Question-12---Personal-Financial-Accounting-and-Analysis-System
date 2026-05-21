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
  <div class="admin-page" v-loading="loading">
    <h2>用户管理（管理员）</h2>
    <el-table :data="users" stripe border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column label="角色" width="120">
        <template #default="{ row }">
          <el-tag :type="row.role === ROLE_ADMIN ? 'danger' : 'info'">{{ ROLE_LABELS[row.role] || '普通用户' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="250">
        <template #default="{ row }">
          <el-button type="warning" size="small" @click="handleToggleRole(row)"
                     :disabled="row.id === userStore.userId">
            {{ row.role === ROLE_ADMIN ? '降为普通用户' : '提升为管理员' }}
          </el-button>
          <el-button type="danger" size="small" @click="handleDelete(row)"
                     :disabled="row.id === userStore.userId">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listUsers, deleteUser, toggleRole } from '../api/admin'
import { useUserStore } from '../stores/user'
import { formatTime } from '../utils/format'
import { ROLE_ADMIN, ROLE_LABELS } from '../constants/role'

const userStore = useUserStore()
const users = ref([])           // 用户列表数据
const loading = ref(true)       // 页面 loading 状态

/**
 * 加载所有用户列表
 * → 调用 api/admin.js 的 listUsers()，对应后端 GET /api/admin/users
 * 返回数据格式: [{ id, username, role, createTime }]
 */
async function loadUsers() {
  try {
    users.value = await listUsers()
  } finally {
    loading.value = false
  }
}

/**
 * 切换用户角色（管理员↔普通用户）
 * → 调用 api/admin.js 的 toggleRole(userId)，对应后端 PUT /api/admin/users/:userId/role
 * 切换后刷新列表（注意: userStore.role 不会实时更新，需重新登录才生效）
 */
async function handleToggleRole(row) {
  try {
    await ElMessageBox.confirm(
      `确认将用户「${row.username}」${row.role === ROLE_ADMIN ? '降为普通用户' : '提升为管理员'}？角色变更后该用户需重新登录才生效。`,
      '角色变更确认',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )
    await toggleRole(row.id)
    ElMessage.success(`${row.username} 角色已切换`)
    await loadUsers()
  } catch {
    // 用户取消或非业务错误，静默处理
  }
}

/**
 * 删除用户（二次确认，不可恢复）
 * → 调用 api/admin.js 的 deleteUser(userId)，对应后端 DELETE /api/admin/users/:userId
 * 删除后刷新列表
 */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除用户「${row.username}」？该操作不可恢复。`, '警告', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteUser(row.id)
    ElMessage.success('用户已删除')
    await loadUsers()
  } catch {
    // 用户取消删除或非业务错误，静默处理
  }
}

// 页面挂载时加载用户列表
onMounted(loadUsers)
</script>

<style scoped>
.admin-page h2 {
  margin-bottom: 20px;
  color: #303133;
}
</style>