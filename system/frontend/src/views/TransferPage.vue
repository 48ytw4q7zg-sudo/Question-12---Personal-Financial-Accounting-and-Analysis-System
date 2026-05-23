<!--
  转账页面
  路由：/transfer
  对应 PRD 功能：P1 转账（在两个账户间转移资金）

  功能说明：
    - 转账表单：选择转出账户、转入账户（互斥，不能选同一个）、输入金额、备注
    - 提交后后端自动创建一对关联的收支记录（转出方支出 + 转入方收入）

  调用关系：
    → 调用 api/transaction.js 的 transfer()（转账接口）
    → 调用 api/account.js 的 getAccountList()（加载账户下拉选项）
-->
<template>
  <div class="transfer-page">
    <h2>转账</h2>

    <el-card shadow="hover" class="transfer-card" v-loading="loading" aria-label="转账表单">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px" style="max-width: 500px; margin: 0 auto;">
        <el-form-item label="转出账户" prop="fromAccountId">
          <el-select v-model="formData.fromAccountId" placeholder="请选择转出账户" style="width: 100%">
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
          <el-input-number v-model="formData.amount" :precision="2" :min="0.01" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="note">
          <el-input v-model="formData.note" type="textarea" placeholder="备注（可选）" :rows="3" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit" style="width: 100%;">确认转账</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'              // 导入Vue组合式API
import { ElMessage } from 'element-plus'                    // 导入消息提示
import { transfer } from '../api/transaction'               // 导入转账API
import { getAccountList } from '../api/account'              // 导入账户列表API
import { logger } from '../utils/logger'                    // 导入日志工具

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
    { type: 'number', min: 0.01, message: '转账金额必须大于0', trigger: 'blur' } // 最小值校验
  ],
  note: [{ max: 200, message: '备注长度不能超过200', trigger: 'blur' }] // 备注长度限制
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

/** 提交转账 */
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false) // 触发表单校验
  if (!valid) return                                        // 校验不通过不提交

  submitting.value = true                                   // 开启提交loading
  try {
    await transfer(formData)                                // 调用转账API
    ElMessage.success('转账成功')                             // 成功提示
    formRef.value.resetFields()                              // 重置表单字段
    // 转账后刷新账户列表（余额可能已变化）
    await loadAccounts()                                    // 刷新账户列表
  } catch (e) {
    // axios 拦截器已统一处理业务错误消息（如余额不足、账户禁用等），此处仅记录异常到控制台
    log.error('转账异常:', e)                            // 记录异常日志
  } finally {
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
