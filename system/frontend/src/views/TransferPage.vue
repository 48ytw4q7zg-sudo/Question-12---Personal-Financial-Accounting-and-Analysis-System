<!--
============================================================
§1.4 数据流 节点 ①②⑯ — 转账全栈链路的前端起终点（用户操作→表单校验→成功处理）
§2.2 逐文件讲解 ⑧/⑩ — TransferPage.vue（转账页面 · 路由 /transfer）

这个文件做什么：转账表单页面——选择转出/转入账户（互斥）、输入金额、备注
               转账完整数据流的起点——用户点"确认转账"触发 17 个节点的全栈链路

答辩讲什么：handleSubmit() — 转账提交的 4 步
  第1步 validate() 表单校验：金额>0、账户不同（前端秒级反馈，不通过不发请求）
  第2步 submitting=true 按钮 loading：防用户连点发出重复转账
  第3步 await transfer(formData)：调 API → 从这里触发全栈 17 节点数据流
  第4步 成功后 ElMessage + resetFields + loadAccounts 刷新余额
  finally 保证 loading 一定关闭——按钮不会永远转圈

★ 答辩讲稿（§1.4 数据流 · 节点 ①②⑯ · 直接念）：
  "节点①，用户在 TransferPage.vue 转账页面选择转出账户、转入账户、输入金额，
   点'确认转账'。这是整个数据流的起点——用户的一次点击，触发后面 16 个节点依次执行。
   节点②，handleSubmit() 方法里，el-form.validate() 先做前端校验——金额必须大于 0、
   转出和转入账户不能相同。这层校验是秒级反馈，校验不通过根本不发 HTTP 请求，省带宽。
   节点⑯，数据回到 TransferPage.vue。handleSubmit() 拿到响应后：
   ElMessage.success 弹出'转账成功'提示 → resetFields() 清空表单 →
   loadAccounts() 重新加载账户余额。用户看到的是：成功提示、表单清空、余额更新——
   一次完整的转账操作结束。"

★ 答辩讲稿（§2.2 核心代码 · ⑧/⑩ · 2分钟 · 直接念）：
  "这是 TransferPage.vue，转账页面。它是转账完整数据流的起点——
   用户在这里选择转出/转入账户、输金额、点确认。
   handleSubmit() 分 4 步：
   第 1 步 formRef.value.validate()——Element Plus 表单校验，金额>0、
   账户不能相同。前端秒级反馈，不通过不发 HTTP 请求。
   第 2 步 submitting.value = true——按钮进入 loading 转圈+disabled 状态，
   防止用户连点发出重复转账。
   第 3 步 await transfer(formData)——调 API，从这里触发 17 个节点的全栈数据流。
   第 4 步 成功后 ElMessage.success 提示 → resetFields() 清空表单 →
   loadAccounts() 刷新账户余额。
   最后 finally { submitting.value = false }——无论成功还是异常，
   保证 loading 一定关闭，按钮不会永远转圈。"

▶ 逐文件讲解下一个（Ctrl+P）：
  system/frontend/src/api/request.js
  （§1.4 节点 ④⑮ · §2.2 逐文件讲解 ⑨/⑩ — axios 请求/响应拦截器）
============================================================
-->
<template>
  <div class="transfer-page">
    <h2>转账</h2>

    <!-- el-card：Element Plus 卡片，v-loading 绑定初始加载状态 -->
    <el-card shadow="hover" class="transfer-card" v-loading="loading" aria-label="转账表单">
      <!-- el-form：Element Plus 表单容器，居中最大宽度 500px -->
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px" style="max-width: 500px; margin: 0 auto;">
        <el-form-item label="转出账户" prop="fromAccountId">
          <!-- Q-CR修复: @change触发toAccountId重校验，防止转出账户变更后转入账户校验状态残留 -->
          <!-- el-select：Element Plus 下拉选择器，已选为转入账户的选项 disabled 禁止选择（互斥约束） -->
          <el-select v-model="formData.fromAccountId" placeholder="请选择转出账户" style="width: 100%" @change="() => formRef.validateField('toAccountId').catch(() => {})">
            <el-option
              v-for="acc in accountList"
              :key="acc.id"
              :label="acc.name"
              :value="acc.id"
              :disabled="acc.id === formData.toAccountId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="转入账户" prop="toAccountId">
          <el-select v-model="formData.toAccountId" placeholder="请选择转入账户" style="width: 100%">
            <el-option
              v-for="acc in accountList"
              :key="acc.id"
              :label="acc.name"
              :value="acc.id"
              :disabled="acc.id === formData.fromAccountId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <!-- el-input-number：Element Plus 数字输入框，:precision="2" 保留2位小数 -->
          <el-input-number v-model="formData.amount" :precision="2" :min="MIN_TRANSACTION_AMOUNT" :step="AMOUNT_STEP_ROUGH" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="note">
          <!-- el-input type="textarea"：Element Plus 多行文本输入，:rows="3" 显示3行 -->
          <el-input v-model="formData.note" type="textarea" placeholder="备注（可选）" :rows="3" />
        </el-form-item>
        <el-form-item>
          <!-- el-button：Element Plus 全宽提交按钮，:loading 防止重复提交 -->
          <el-button type="primary" :loading="submitting" @click="handleSubmit" style="width: 100%;">确认转账</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'              // 导入Vue组合式API
import { ElMessage } from 'element-plus'                    // 导入消息提示
import { transfer } from '../api/transaction'               // 导入转账API（api/transaction.js）
import { getAccountList } from '../api/account'              // 导入账户列表API（api/account.js）
import { MIN_TRANSACTION_AMOUNT, MAX_NOTE_LENGTH, AMOUNT_STEP_ROUGH } from '../constants/finance' // 导入常量
import { logger } from '../utils/logger'                    // 导入日志工具（utils/logger.js）

const log = logger('TransferPage')                          // 创建日志实例
const loading = ref(false)          // 初始加载状态
const submitting = ref(false)       // 提交 loading
const formRef = ref(null)           // 表单引用
const accountList = ref([])         // 账户下拉选项列表

const formData = reactive({
  fromAccountId: null,              // 转出账户ID
  toAccountId: null,                // 转入账户ID
  amount: null,                     // 转账金额
  note: ''                          // 备注内容
})

const formRules = {
  fromAccountId: [{ required: true, message: '请选择转出账户', trigger: 'change' }], // 转出账户必填
  toAccountId: [                                            // 转入账户校验规则
    { required: true, message: '请选择转入账户', trigger: 'change' }, // 必填校验
    {
      validator: (rule, value, callback) => {               // 自定义校验器
        if (value && value === formData.fromAccountId) {    // 与转出账户相同
          callback(new Error('转入账户不能与转出账户相同'))  // 校验失败
        } else {
          callback()                                        // 校验通过
        }
      },
      trigger: 'change'                                     // 选项变化触发
    }
  ],
  amount: [                                                 // 金额校验规则
    { required: true, message: '请输入转账金额', trigger: 'blur' }, // 必填校验
    { type: 'number', min: MIN_TRANSACTION_AMOUNT, message: '转账金额必须大于0', trigger: 'blur' } // 使用常量 constants/finance.js
  ],
  note: [{ max: MAX_NOTE_LENGTH, message: `备注长度不能超过${MAX_NOTE_LENGTH}`, trigger: 'blur' }] // 使用常量 constants/finance.js
}

/** 加载账户列表 */
async function loadAccounts() {
  loading.value = true                                      // 开启loading
  try {
    const data = await getAccountList()                      // 调用API获取账户列表
    accountList.value = data || []                           // 设置账户选项数据
  } catch (e) {
    // axios 拦截器已统一处理业务错误消息，此处额外做本地降级提示（账户下拉为空时用户需手动刷新）
    log.warn('加载账户列表失败:', e) // 开发环境日志
    ElMessage.warning('加载账户列表失败，请刷新重试')          // 降级提示
  } finally {
    loading.value = false                                    // 关闭loading
  }
}

/**
 * ★★【答辩·handleSubmit() 转账提交 — 转账数据流起点】★★
 * 做什么：用户点击"确认转账"→表单校验→调API→成功提示+重置+刷新余额→错误处理→关闭loading
 * 为什么在这里讲：这是转账完整数据流的起点——从这个函数开始触发14个节点的全栈链路
 * 覆盖知识点：Element Plus表单校验/async-await异步/loading防抖/axios-intercept错误处理/finally资源释放
 */
/** 提交转账 */
async function handleSubmit() {
  // ★★【答辩·handleSubmit() 转账提交 — 转账数据流起点】★★
  // 覆盖知识点：Element Plus表单校验/async-await异步/loading防抖/axios-intercept错误处理/finally资源释放

  // ★ 第1步：Element Plus 表单校验
  //  做什么：validate() 检查所有 el-form-item 的 rules——fromAccountId必填/toAccountId必填且不同于转出/amount>0
  //  为什么 .catch(() => false)：validate() 失败时 reject，不 catch 会变成 unhandled rejection；转成 false 方便 if(!valid) 判断
  const valid = await formRef.value.validate().catch(() => false) // 触发表单校验
  if (!valid) return                                        // ★ 校验失败直接退出——前端校验秒级反馈，不浪费带宽发HTTP请求

  // ★ 第2步：按钮 loading 防抖
  //  做什么：按钮变成转圈+disabled，防止用户连点发出多个重复转账请求
  //  为什么不用防抖函数（debounce）：防抖延迟用户体验差——loading是最直观的"正在处理中"反馈
  submitting.value = true                                   // 开启提交loading
  try {
    // ★★ 第3步：调用转账 API（核心步骤——从这里触发14节点全栈数据流）
    //  做什么：await transfer(formData) → api/transaction.js 的 transfer() →
    //          request.js 请求拦截器注入 token → POST /api/v1/transaction/transfer →
    //          Vite Proxy → CorsFilter → LoginInterceptor → TransactionController →
    //          TransactionServiceImpl.transfer()（fail-fast→加锁→BigDecimal→复式记账→双条INSERT→COMMIT）
    //  为什么 await：等待后端完整处理转账并返回结果——不加 await 会直接跳过，前端不知道转账是否成功
    await transfer(formData)                                // 调用转账API
    // ★★ 第4步：成功处理
    //  做什么：① ElMessage.success 用户级反馈 ② resetFields() 清空表单 ③ loadAccounts() 刷新账户余额
    //  为什么刷新账户列表：转账后两个账户的余额都变了——调用 getAccountList() 从后端拿最新余额，不刷新用户看到的还是旧数字
    ElMessage.success('转账成功')                             // 成功提示
    formRef.value.resetFields()                              // 重置表单字段
    // 转账后刷新账户列表（余额可能已变化）
    await loadAccounts()                                    // 刷新账户列表
  } catch (e) {
    // ★★ 第5步：错误处理——网络异常兜底
    //  说明：业务异常（余额不足/账户不存在/账户相同）已被 axios 响应拦截器统一 ElMessage.error 处理
    //  这里只处理网络层异常——代码拿不到响应对象（error.response 为空）
    //  为什么区分 ERR_NETWORK 和 ECONNABORTED：
    //    ERR_NETWORK = 网络断开/DNS解析失败/服务器宕机——请求根本没发出去
    //    ECONNABORTED = axios 的 10 秒 timeout 触发——服务器响应太慢
    //  axios 拦截器已统一处理业务错误消息（如余额不足、账户禁用等），此处处理网络/超时异常
    log.error('转账异常:', e)                            // 记录异常日志
    if (e.code === 'ERR_NETWORK' || e.code === 'ECONNABORTED') {  // 网络错误或超时
      ElMessage.error('网络异常，转账失败')              // 网络级错误提示
    }
  } finally {
    // ★★ 第6步：finally 关闭 loading（保证一定执行）
    //  为什么 finally：如果只在 try 里关，catch 里抛了新异常会跳过——finally 无论成功/失败/异常都执行
    //  不关 loading 的后果：按钮永远转圈，用户无法再次提交——只能刷新页面
    submitting.value = false                                 // 关闭提交loading
  }
}

/** 页面挂载时加载账户列表（async+await 确保未捕获异常不会变成 unhandled rejection） */
onMounted(async () => {
  await loadAccounts()                                      // 挂载时加载账户（await保证异常可追踪）
})
</script>

<style scoped>
.transfer-page h2 {
  margin-bottom: 20px;
  color: var(--color-title);
}

.transfer-card {
  max-width: 600px;
  margin: 0 auto;
}
</style>
