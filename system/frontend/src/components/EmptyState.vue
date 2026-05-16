<template>
  <div class="empty-state">
    <el-icon class="empty-icon" :size="64"><component :is="icon" /></el-icon>
    <p class="empty-text">{{ description }}</p>
    <el-button v-if="actionText" type="primary" @click="$emit('action')">
      {{ actionText }}
    </el-button>
  </div>
</template>

<script setup>
import { FolderOpened } from '@element-plus/icons-vue'

defineProps({
  description: { type: String, default: '暂无数据' },
  actionText: { type: String, default: '' },
  icon: { type: Object, default: () => FolderOpened }
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
