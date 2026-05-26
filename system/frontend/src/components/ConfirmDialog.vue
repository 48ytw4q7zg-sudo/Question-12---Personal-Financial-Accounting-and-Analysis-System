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
  <!-- el-dialog：Element Plus 弹窗对话框组件 -->
  <el-dialog
    v-model="visible"                   <!-- 双向绑定弹窗显隐（内部状态） -->
    :title="title"                      <!-- 弹窗标题（由父组件传入） -->
    width="420px"                       <!-- 弹窗宽度 420px -->
    :close-on-click-modal="false"       <!-- 禁止点击遮罩层关闭弹窗（防误操作） -->
    destroy-on-close                    <!-- 关闭弹窗时销毁DOM，避免表单残留 -->
  >
    <p class="confirm-content">{{ content }}</p>  <!-- 确认提示文字（由父组件传入） -->
    <template #footer>
      <el-button @click="handleCancel">取消</el-button>  <!-- el-button：取消按钮，点击关闭弹窗 -->
      <el-button type="danger" @click="handleConfirm" :loading="confirming">
        <!-- 确认按钮：danger 红色样式 + loading 防止重复点击 -->
        {{ confirmText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
// → 导入 Vue 3 响应式 API：ref 创建响应式变量，watch 监听数据变化
import { ref, watch } from 'vue'                            // 导入Vue组合式API（vue）

// ========== Props 定义 ==========
const props = defineProps({
  modelValue: { type: Boolean, default: false },    // 弹窗显隐（v-model 双向绑定，由父组件传入）
  title: { type: String, default: '确认操作' },      // 弹窗标题（由父组件传入）
  content: { type: String, default: '确定要执行此操作吗？' },  // 确认提示文字（由父组件传入）
  confirmText: { type: String, default: '确定删除' } // 确认按钮文字（由父组件传入）
})

// ========== Events 定义 ==========
const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])
// update:modelValue → v-model 双向绑定通知父组件更新显隐
// confirm → 用户点击确认按钮时触发（父组件执行实际操作）
// cancel → 用户点击取消/关闭时触发

// 内部弹窗显隐状态（与 props.modelValue 双向同步，避免直接修改 prop）
const visible = ref(props.modelValue)
// 确认按钮 loading 状态（防重复提交，操作完成后由父组件调用 resetLoading() 重置）
const confirming = ref(false)

// 监听外部 v-model 变化 → 同步到内部 visible + 弹窗关闭时重置 confirming 状态
watch(() => props.modelValue, (val) => {
  visible.value = val                                      // 同步显隐状态
  if (!val) confirming.value = false  // 弹窗关闭时重置 loading 状态，避免下次打开残留
})
// 监听内部 visible 变化 → 通知外部更新 v-model（实现双向绑定闭环）
watch(visible, (val) => { emit('update:modelValue', val) })

/**
 * 点击确认按钮 → 开启 loading + 触发 confirm 事件
 * 安全加固：使用 try-catch 包裹 emit，防止父组件同步抛出异常导致 loading 状态卡死
 * 父组件应始终在异步操作完成后调用 confirmRef.resetLoading() 重置状态
 * 调用链：ConfirmDialog handleConfirm() → 父组件 @confirm → 父组件执行业务操作
 */
function handleConfirm() {
  confirming.value = true                           // 开启loading防重复提交
  try {
    emit('confirm')                                 // 触发父组件 @confirm 事件（父组件在此执行删除/操作逻辑）
  } catch (e) {
    confirming.value = false                        // 父组件同步抛出异常时自动重置loading
  }
}

/**
 * 点击取消按钮 → 关闭弹窗 + 触发 cancel 事件
 * 调用链：ConfirmDialog handleCancel() → 父组件 @cancel
 */
function handleCancel() {
  visible.value = false                                    // 关闭弹窗（触发 watch → emit('update:modelValue')）
  emit('cancel')                                           // 触发父组件 @cancel 事件
}

/**
 * 重置 loading 状态（暴露给父组件，操作完成后通过 ref 调用）
 * 使用场景：父组件异步操作完成后调用 confirmRef.resetLoading() 关闭按钮 loading
 */
function resetLoading() {
  confirming.value = false
}

// 暴露方法给父组件通过 ref 调用（如 confirmRef.value.resetLoading()）
defineExpose({ resetLoading })
</script>

<style scoped>
.confirm-content {
  padding: 8px 0;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}
</style>
