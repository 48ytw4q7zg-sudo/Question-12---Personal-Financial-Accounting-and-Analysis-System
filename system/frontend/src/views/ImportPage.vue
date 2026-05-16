<template>
  <div class="import-page">
    <h2>数据导入</h2>

    <el-card shadow="hover">
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
          <el-button type="primary" :loading="importing" :disabled="!importForm.file || !importForm.accountId" @click="handleImport">
            开始导入
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 文件格式说明 -->
    <el-card shadow="hover" class="format-card">
      <template #header>CSV 文件格式说明</template>
      <p>CSV 文件需包含以下列（第一行为表头）：</p>
      <el-table :data="formatData" border size="small">
        <el-table-column prop="col" label="列名" width="120" />
        <el-table-column prop="desc" label="说明" />
        <el-table-column prop="required" label="必填" width="80" />
        <el-table-column prop="example" label="示例" width="150" />
      </el-table>
      <p class="tip-text">示例格式：<code>time,categoryName,type,amount,note</code></p>
    </el-card>

    <!-- 导入结果 -->
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { importCsv } from '../api/transaction'
import { getAccountList } from '../api/account'

const importing = ref(false)
const uploadRef = ref(null)
const formRef = ref(null)
const accountList = ref([])
const importResult = ref(null)

const importForm = reactive({
  accountId: null,
  file: null
})

const formRules = {
  accountId: [{ required: true, message: '请选择目标账户', trigger: 'change' }],
  file: [{
    validator: (rule, value, callback) => {
      if (!importForm.file) {
        callback(new Error('请选择 CSV 文件'))
      } else {
        callback()
      }
    },
    trigger: 'change'
  }]
}

const formatData = [
  { col: 'time', desc: '交易时间', required: '是', example: '2026-05-16 10:30:00' },
  { col: 'categoryName', desc: '分类名称', required: '是', example: '餐饮' },
  { col: 'type', desc: '类型(income/expense)', required: '是', example: 'expense' },
  { col: 'amount', desc: '金额', required: '是', example: '50.00' },
  { col: 'note', desc: '备注', required: '否', example: '午餐' }
]

function handleFileChange(file) {
  importForm.file = file.raw
}

async function loadAccounts() {
  try {
    const data = await getAccountList()
    accountList.value = data || []
  } catch {
    // 静默处理
  }
}

async function handleImport() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  importing.value = true
  importResult.value = null
  try {
    const formData = new FormData()
    formData.append('file', importForm.file)
    formData.append('accountId', importForm.accountId)
    const data = await importCsv(formData)
    importResult.value = data
    ElMessage.success('导入完成')
  } catch {
    // 错误由 axios 拦截器处理
  } finally {
    importing.value = false
  }
}

onMounted(() => {
  loadAccounts()
})
</script>

<style scoped>
.import-page h2 {
  margin-bottom: 20px;
  color: #303133;
}

.format-card {
  margin-top: 20px;
}

.result-card {
  margin-top: 20px;
}

.tip-text {
  margin-top: 12px;
  color: #909399;
  font-size: 14px;
}

.tip-text code {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
}

.fail-rows {
  margin-top: 16px;
}

.fail-rows h4 {
  margin-bottom: 8px;
  color: #f56c6c;
}
</style>
