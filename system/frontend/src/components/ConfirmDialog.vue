<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="420px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <p class="confirm-content">{{ content }}</p>
    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="danger" @click="handleConfirm" :loading="confirming">
        {{ confirmText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '确认操作' },
  content: { type: String, default: '确定要执行此操作吗？' },
  confirmText: { type: String, default: '确定删除' }
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

const visible = ref(props.modelValue)
const confirming = ref(false)

watch(() => props.modelValue, (val) => { visible.value = val })
watch(visible, (val) => { emit('update:modelValue', val) })

function handleConfirm() {
  confirming.value = true
  emit('confirm')
}

function handleCancel() {
  visible.value = false
  emit('cancel')
}

function resetLoading() {
  confirming.value = false
}

defineExpose({ resetLoading })
</script>

<style scoped>
.confirm-content {
  padding: 8px 0;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}
</style>
