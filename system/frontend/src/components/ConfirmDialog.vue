<!--
  通用确认弹窗组件（可复用）
  职责：二次确认操作（如删除），支持自定义标题、内容、按钮文字

  Props:
    - modelValue (Boolean): 弹窗显隐，支持 v-model 双向绑定
    - title (String): 弹窗标题，默认「确认操作」
    - content (String): 确认内容文字，默认「确定要执行此操作吗？」
    - confirmText (String): 确认按钮文字，默认「确定删除」

  Events:
    - update:modelValue: v-model 双向绑定
    - confirm: 点击确认按钮时触发（父组件在此执行实际删除逻辑）
    - cancel: 点击取消按钮时触发

  Methods（通过 defineExpose 暴露给父组件）:
    - resetLoading(): 重置确认按钮的 loading 状态（父组件操作完成后调用）

  使用示例：
    <ConfirmDialog v-model="showDialog" content="确定删除吗？" @confirm="doDelete" ref="confirmRef" />
-->
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

// Props 定义
const props = defineProps({
  modelValue: { type: Boolean, default: false },    // 弹窗显隐
  title: { type: String, default: '确认操作' },      // 弹窗标题
  content: { type: String, default: '确定要执行此操作吗？' },  // 确认提示文字
  confirmText: { type: String, default: '确定删除' } // 确认按钮文字
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

// 内部弹窗显隐状态（与 props.modelValue 双向同步）
const visible = ref(props.modelValue)
// 确认按钮 loading 状态（防重复提交）
const confirming = ref(false)

// 监听外部 v-model 变化 → 同步到内部 visible + 弹窗关闭时重置 confirming 状态
watch(() => props.modelValue, (val) => {
  visible.value = val
  if (!val) confirming.value = false  // 弹窗关闭时重置 loading 状态，避免下次打开残留
})
// 监听内部 visible 变化 → 通知外部更新 v-model
watch(visible, (val) => { emit('update:modelValue', val) })

/** 点击确认 → 开启 loading + 触发 confirm 事件，由父组件执行实际逻辑 */
function handleConfirm() {
  confirming.value = true
  emit('confirm')
}

/** 点击取消 → 关闭弹窗 + 触发 cancel 事件 */
function handleCancel() {
  visible.value = false
  emit('cancel')
}

/** 重置 loading 状态（暴露给父组件，操作完成后调用） */
function resetLoading() {
  confirming.value = false
}

// 暴露方法给父组件通过 ref 调用
defineExpose({ resetLoading })
</script>

<style scoped>
.confirm-content {
  padding: 8px 0;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}
</style>
