<!--
  空状态组件（可复用）
  职责：当列表/数据为空时显示的占位提示，支持自定义图标、描述文字和操作按钮

  Props:
    - description (String): 提示文字，默认「暂无数据」
    - actionText (String): 操作按钮文字（为空则不显示按钮）
    - icon (Object): 图标组件，默认 FolderOpened

  Events:
    - action: 点击操作按钮时触发

  使用示例：
    <EmptyState description="暂无交易记录" actionText="记一笔" @action="openAddDialog" />
-->
<template>
  <!-- 空状态容器：flex 居中布局，role=status 无障碍语义 -->
  <div class="empty-state" role="status" aria-live="polite">
    <!-- el-icon：Element Plus 图标组件，动态渲染传入的 icon prop（默认 FolderOpened 文件夹图标） -->
    <el-icon class="empty-icon" :size="64"><component :is="icon" /></el-icon>
    <p class="empty-text">{{ description }}</p>
    <!-- el-button：Element Plus 操作按钮，仅当 actionText 非空时渲染（v-if 条件渲染） -->
    <el-button v-if="actionText" type="primary" @click="$emit('action')">
      {{ actionText }}
    </el-button>
  </div>
</template>

<script setup>
// → 导入 @element-plus/icons-vue 的 FolderOpened 图标组件（作为默认空状态图标）
import { FolderOpened } from '@element-plus/icons-vue'  // 导入Element Plus图标（@element-plus/icons-vue）

// ========== Props 定义 ==========
defineProps({
  description: { type: String, default: '暂无数据' },  // 空状态描述文字（由父组件传入）
  actionText: { type: String, default: '' },             // 操作按钮文字，空字符串不显示按钮（v-if 控制渲染）
  icon: { type: Object, default: () => FolderOpened }    // 图标组件，默认文件夹图标（由父组件选择性传入）
})

// ========== Events 定义 ==========
defineEmits(['action'])  // 点击操作按钮时触发 action 事件（→ 父组件执行对应操作）
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: var(--el-text-color-secondary);
}
.empty-icon {
  color: var(--el-color-info-light-3);
  margin-bottom: 16px;
}
.empty-text {
  font-size: 14px;
  margin-bottom: 16px;
}
</style>
