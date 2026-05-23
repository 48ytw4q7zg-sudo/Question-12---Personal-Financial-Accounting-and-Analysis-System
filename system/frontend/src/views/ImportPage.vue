<!--
  数据导入页面
  路由：/import
  对应 PRD 功能：P2 数据导入（CSV 文件导入收支记录）

  功能说明：
    - 选择目标账户 + 上传 CSV 文件
    - CSV 格式说明表格
    - 导入结果展示（成功/失败条数 + 失败详情）

  调用关系：
    → 调用 api/transaction.js 的 importCsv()（上传 CSV 文件）
    → 调用 api/account.js 的 getAccountList()（加载账户下拉选项）
-->
<template>
  <div class="import-page">
    <h2>数据导入</h2>

    <!-- CSV 上传表单 -->
    <el-card shadow="hover" v-loading="loading" aria-label="CSV导入表单">
      <template #header>导入 CSV 文件</template>

      <el-form ref="formRef" :model="importForm" :rules="formRules" label-width="90px" style="max-width: 500px;">
        <el-form-item label="目标账户" prop="accountId">
          <el-select v-model="importForm.accountId" placeholder="请选择账户" style="width: 100%;">
            <el-option v-for="acc in accountList" :key="acc.id" :label="acc.name" :value="acc.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="CSV 文件" prop="file">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".csv"
            :on-change="handleFileChange"
            :on-remove="() => importForm.file = null"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">仅支持 .csv 格式文件</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <!-- 文件和账户都选择后才能导入 -->
          <el-button type="primary" :loading="importing" :disabled="!importForm.file || !importForm.accountId" @click="handleImport">
            开始导入
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- CSV 文件格式说明 -->
    <el-card shadow="hover" class="format-card">
      <template #header>CSV 文件格式说明</template>
      <p>CSV 文件需包含以下列（第一行为表头）：</p>
      <el-table :data="formatData" border size="small" aria-label="CSV格式说明">
        <el-table-column prop="col" label="列名" width="120" />
        <el-table-column prop="desc" label="说明" />
        <el-table-column prop="required" label="必填" width="80" />
        <el-table-column prop="example" label="示例" width="150" />
      </el-table>
      <p class="tip-text">示例格式：<code>time,categoryId,type,amount,note</code>（第一行为表头，会被自动跳过）</p>
    </el-card>

    <!-- 导入结果（仅导入完成后显示） -->
    <el-card v-if="importResult" shadow="hover" class="result-card">
      <template #header>导入结果</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="成功条数">
          <el-tag type="success">{{ importResult.successCount }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="失败条数">
          <el-tag :type="importResult.failCount > 0 ? 'danger' : 'info'">{{ importResult.failCount }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <!-- 失败详情表格（有失败记录时显示） -->
      <div v-if="importResult.failRows && importResult.failRows.length > 0" class="fail-rows">
        <h4>失败详情：</h4>
        <el-table :data="importResult.failRows" border size="small" max-height="300">
          <el-table-column prop="row" label="行号" width="80" />
          <el-table-column prop="reason" label="原因" min-width="200" />
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'              // 导入Vue组合式API
import { ElMessage } from 'element-plus'                    // 导入消息提示
import { logger } from '../utils/logger'                    // 导入日志工具
// → 调用 api/transaction.js 的 importCsv()（CSV 导入接口）
import { importCsv } from '../api/transaction'               // 导入CSV导入API
// → 调用 api/account.js 的 getAccountList()（加载账户选项）
import { getAccountList } from '../api/account'              // 导入账户列表API
import { MAX_CSV_FILE_SIZE } from '../constants/finance'    // 导入文件大小上限常量

const log = logger('ImportPage')                            // 创建日志实例

const importing = ref(false)        // 导入中 loading
const loading = ref(false)          // 初始加载状态
const uploadRef = ref(null)         // 上传组件引用
const formRef = ref(null)           // 表单引用
const accountList = ref([])         // 账户列表
const importResult = ref(null)      // 导入结果（成功/失败数 + 失败详情）

// 导入表单数据
const importForm = reactive({
  accountId: null,    // 目标账户 ID（导入的交易记录归属此账户）
  file: null          // CSV 文件对象
})

// 表单校验规则
const formRules = {
  accountId: [{ required: true, message: '请选择目标账户', trigger: 'change' }], // 账户必选
  file: [{                                                  // 文件校验规则
    validator: (rule, value, callback) => {                 // 自定义文件校验器
      if (!importForm.file) {                               // 未选文件
        callback(new Error('请选择 CSV 文件'))              // 校验失败
      } else if (importForm.file.size > MAX_CSV_FILE_SIZE) { // 文件超过5MB
        // PRD P2-3 业务规则①: 文件大小 ≤ 5MB，前端预检避免浪费上传时间
        callback(new Error('文件大小不能超过 5MB'))          // 校验失败
      } else {
        callback()                                          // 校验通过
      }
    },
    trigger: 'change'                                       // 文件变化触发
  }]
}

// CSV 文件格式说明数据（对齐后端 TransactionServiceImpl.importCsv() 解析的 5 列格式）
// 后端解析顺序：日期,分类ID,类型(1=收入/2=支出),金额,备注
const formatData = [                                         // CSV格式说明数据
  { col: 'time', desc: '交易时间（yyyy-MM-dd HH:mm:ss）', required: '是', example: '2026-05-16 10:30:00' }, // 时间列
  { col: 'categoryId', desc: '分类ID（数字，对应系统分类列表）', required: '是', example: '1' },              // 分类ID列
  { col: 'type', desc: '类型（1=收入, 2=支出）', required: '是', example: '2' },                            // 类型列
  { col: 'amount', desc: '金额（正数，两位小数）', required: '是', example: '50.00' },                      // 金额列
  { col: 'note', desc: '备注', required: '否', example: '午餐' }                                          // 备注列
]

/** el-upload 文件选择回调：保存原始文件对象 + 手动触发表单校验 */
function handleFileChange(file) {
  importForm.file = file.raw                                 // 保存原始File对象
  // el-upload 的 on-change 不会自动触发 el-form 的校验，需手动调用
  formRef.value?.validateField('file')                       // 手动触发文件字段校验
}

/**
 * 加载账户列表
 * → 调用 api/account.js 的 getAccountList()
 */
async function loadAccounts() {
  loading.value = true                                      // 开启loading
  try {
    const data = await getAccountList()                      // 调用API获取账户列表
    accountList.value = data || []                           // 设置账户数据
  } catch (e) {
    log.warn('加载账户列表失败:', e) /* 开发环境日志 */
    ElMessage.error('账户列表加载失败，请刷新重试')            // 用户级错误提示
    accountList.value = []                                   // 清空数据
  } finally {
    loading.value = false                                    // 关闭loading
  }
}

/**
 * 执行 CSV 导入
 * → 调用 api/transaction.js 的 importCsv(formData)
 * 使用 FormData 格式上传文件 + accountId 参数
 */
async function handleImport() {
  const valid = await formRef.value.validate().catch(() => false) // 触发全表单校验
  if (!valid) return                                        // 校验不通过不提交

  importing.value = true                                    // 开启导入loading
  importResult.value = null                                 // 清空上次导入结果
  try {
    // 构建 FormData（multipart/form-data 格式上传文件）
    const formData = new FormData()                          // 创建FormData对象
    formData.append('file', importForm.file)                 // 添加CSV文件
    formData.append('accountId', importForm.accountId)       // 添加目标账户ID
    // → 调用 api/transaction.js 的 importCsv(formData)
    const data = await importCsv(formData)                   // 调用CSV导入API
    importResult.value = data                                // 保存导入结果
    ElMessage.success('导入完成')                             // 成功提示
  } catch (e) {
    // axios 拦截器已统一处理业务错误消息（如文件超限、格式错误等）
    log.error('CSV导入异常:', e)                            // 记录异常日志
    // Q-CR修复：补充用户级错误提示，处理拦截器未覆盖的网络异常
    if (e.code === 'ERR_NETWORK' || e.code === 'ECONNABORTED') {
      ElMessage.error('网络异常，CSV导入失败')              // 网络级错误提示
    }
  } finally {
    importing.value = false                                  // 关闭导入loading
  }
}

// 页面挂载时加载账户列表（async+await 确保未捕获异常不会变成 unhandled rejection）
onMounted(async () => {
  await loadAccounts()                                      // 挂载时加载账户（await保证异常可追踪）
})
</script>

<style scoped>
.import-page h2 {
  margin-bottom: 20px;
  color: var(--color-title);
}

.format-card {
  margin-top: 20px;
}

.result-card {
  margin-top: 20px;
}

.tip-text {
  margin-top: 12px;
  color: var(--color-muted, #909399);
  font-size: 14px;
}

.tip-text code {
  background: var(--el-fill-color-light);
  padding: 2px 6px;
  border-radius: 4px;
}

.fail-rows {
  margin-top: 16px;
}

.fail-rows h4 {
  margin-bottom: 8px;
  color: var(--color-expense);
}
</style>
