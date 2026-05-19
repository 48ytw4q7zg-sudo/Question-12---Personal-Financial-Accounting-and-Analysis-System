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
  <div class="empty-state">
    <el-icon class="empty-icon" :size="64"><component :is="icon" /></el-icon>
    <p class="empty-text">{{ description }}</p>
    <!-- 仅当 actionText 非空时显示操作按钮 -->
    <el-button v-if="actionText" type="primary" @click="$emit('action')">
      {{ actionText }}
    </el-button>
  </div>
</template>

<script setup>
import { FolderOpened } from '@element-plus/icons-vue'

defineProps({
  description: { type: String, default: '暂无数据' },  // 空状态描述文字
  actionText: { type: String, default: '' },             // 操作按钮文字，空字符串不显示按钮
  icon: { type: Object, default: () => FolderOpened }    // 图标组件，默认文件夹图标
})

defineEmits(['action'])
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
