<!--
  账户管理页面
  路由：/account
  对应 PRD 功能：P0 账户 CRUD（多账户管理：列表 + 增 + 改，软删除）+ 按账户汇总余额
    + P2-4 多币种支持（余额卡片含 CNY 等值换算）

  功能说明：
    - 账户列表表格（名称/类型/初始余额/币种/状态/创建时间/操作）
    - 新增/编辑账户弹窗（el-dialog + el-form）
    - 删除账户二次确认（ElMessageBox.confirm）
    - 账户余额汇总卡片（按账户统计当前余额 + 多币种CNY等值换算 · P2-4）

  调用关系：
    → 调用 api/account.js 的 getAccountList()（加载账户列表）
    → 调用 api/account.js 的 createAccount()（新增账户）
    → 调用 api/account.js 的 updateAccount()（编辑账户）
    → 调用 api/account.js 的 deleteAccount()（删除账户）
    → 调用 api/account.js 的 getAccountBalance()（加载余额汇总）
    → 调用 request.js 的 axios 实例（GET /api/exchange-rate · P2-4 汇率换算）
-->
<template>
  <div class="account-page">
    <div class="page-header">
      <h2>账户管理</h2>
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon>新增账户
      </el-button>
    </div>

    <!-- 账户列表表格 -->
    <el-card shadow="hover">
      <el-table :data="accountList" v-loading="loading" stripe>
        <template #empty><el-empty description="暂无账户" /></template>
        <el-table-column prop="name" label="账户名称" min-width="120" />
        <!-- 账户类型：数字映射为中文显示（1=现金, 2=银行卡, 3=支付宝, 4=微信） -->
        <el-table-column prop="type" label="账户类型" width="100">
          <template #default="{ row }">
            {{ accountTypeMap[row.type] || row.type }}
          </template>
        </el-table-column>
        <el-table-column prop="initialBalance" label="初始余额" width="120">
          <template #default="{ row }">
            ¥ {{ formatAmount(row.initialBalance) }}
          </template>
        </el-table-column>
        <el-table-column prop="currency" label="币种" width="80" />
        <!-- 状态标签：1=启用(success绿色), 0=停用(info灰色) -->
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ statusMap[row.status] || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <!-- 操作列：编辑 + 删除 -->
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)" :disabled="deletingId === row.id">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 余额汇总卡片：每个账户显示当前余额 + 非CNY币种显示CNY等值换算（P2-4） -->
    <el-card shadow="hover" class="balance-card" v-if="balanceList.length > 0">
      <template #header>账户余额汇总</template>
      <el-row :gutter="16">
        <el-col v-for="item in balanceList" :key="item.accountId" :xs="12" :sm="6">
          <div class="balance-item">
            <div class="balance-name">{{ item.accountName }}</div>
            <div class="balance-amount">¥ {{ formatAmount(item.currentBalance) }}</div>
            <!-- P2-4: 非CNY币种展示CNY等值换算 -->
            <div v-if="accountCurrency(item.accountId) !== 'CNY'" class="balance-cny-equivalent">
              折合 ≈ ¥ {{ formatCnyEquivalent(item) }}
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑账户' : '新增账户'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="账户名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入账户名称" />
        </el-form-item>
        <el-form-item label="账户类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择账户类型" style="width: 100%">
            <el-option label="现金" :value="1" />
            <el-option label="银行卡" :value="2" />
            <el-option label="支付宝" :value="3" />
            <el-option label="微信" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="初始余额" prop="initialBalance">
          <el-input-number v-model="formData.initialBalance" :precision="2" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="币种" prop="currency">
          <el-select v-model="formData.currency" placeholder="请选择币种" style="width: 100%">
            <el-option v-for="c in CURRENCY_LIST" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
// → 调用 api/account.js 的 5 个接口函数
import { getAccountList, createAccount, updateAccount, deleteAccount, getAccountBalance } from '../api/account'
// → P2-4 多币种：调用 api/exchange-rate.js 的 getExchangeRates()（汇率数据）
import { getExchangeRates } from '../api/exchange-rate'
import { formatTime, formatAmount } from '../utils/format'
import { ACCOUNT_TYPE_MAP, CURRENCY_LIST, STATUS_MAP } from '../constants/finance'

/** P2-4: 汇率数据缓存（1外币→CNY），例 { USD: 7.3, EUR: 7.94, ... } */
const exchangeRates = ref({})

// 账户类型映射（从常量文件统一管理，避免硬编码散布各页面）
const accountTypeMap = ACCOUNT_TYPE_MAP
const statusMap = STATUS_MAP

const loading = ref(false)          // 列表 loading
const submitting = ref(false)       // 表单提交 loading
const deletingId = ref(null)        // 当前正在删除的账户 ID
const dialogVisible = ref(false)    // 弹窗显隐
const isEdit = ref(false)           // 是否编辑模式（true=编辑, false=新增）
const editId = ref(null)            // 当前编辑的账户 ID
const formRef = ref(null)           // 表单引用（用于校验）

const accountList = ref([])         // 账户列表数据
const balanceList = ref([])         // 余额汇总数据

// 新增/编辑表单数据
const formData = reactive({
  name: '',
  type: null,
  initialBalance: 0,
  currency: 'CNY'
})

// 表单校验规则
const formRules = {
  name: [{ required: true, message: '请输入账户名称', trigger: 'blur' }, { max: 20, message: '账户名称不超过20个字符', trigger: 'blur' }],
  type: [{ required: true, message: '请选择账户类型', trigger: 'change' }],
  initialBalance: [
    { required: true, message: '请输入初始余额', trigger: 'blur' },
    { type: 'number', min: 0, message: '初始余额不能为负数', trigger: 'blur' }
  ],
  currency: [{ required: true, message: '请选择币种', trigger: 'change' }]
}

/**
 * P2-4: 根据 accountId 查找账户的币种
 * @param {Number} accountId - 账户 ID
 * @returns {String} 币种代码（如 CNY/USD/EUR）
 */
function accountCurrency(accountId) {
  const acct = accountList.value.find(a => a.id === accountId)
  return acct ? acct.currency : 'CNY'
}

/**
 * P2-4: 将指定账户的余额换算为 CNY 等值（非 CNY 币种用硬编码汇率换算）
 * @param {Object} balanceItem - 余额汇总项 { accountId, currentBalance }
 * @returns {String} 格式化后的 CNY 等值金额
 */
function formatCnyEquivalent(balanceItem) {
  const currency = accountCurrency(balanceItem.accountId)
  if (currency === 'CNY') return formatAmount(balanceItem.currentBalance)
  const rate = exchangeRates.value[currency]
  if (!rate) return 'N/A'
  return formatAmount(Number(balanceItem.currentBalance) * Number(rate))
}

/**
 * P2-4: 加载汇率数据（通过 api/exchange-rate.js 封装函数调用 GET /api/exchange-rate）
 * 返回 { ratesInverse: { USD: "7.3000", ... } }
 */
async function loadExchangeRates() {
  try {
    const data = await getExchangeRates()
    if (data && data.ratesInverse) {
      // ratesInverse 是 1外币→CNY 的汇率（如 USD: 7.3 表示 1美元=7.3人民币）
      exchangeRates.value = data.ratesInverse
    }
  } catch (e) {
    if (import.meta.env.DEV) console.warn('加载汇率数据失败:', e)
    ElMessage.warning('汇率数据加载失败，CNY换算暂不可用')
    // exchangeRates 保持空，换算显示 N/A
  }
}

/**
 * 加载账户列表
 * → 调用 api/account.js 的 getAccountList()
 */
async function loadAccounts() {
  loading.value = true
  try {
    const data = await getAccountList()
    accountList.value = data || []
  } finally {
    loading.value = false
  }
}

/**
 * 加载账户余额汇总
 * → 调用 api/account.js 的 getAccountBalance()
 */
async function loadBalance() {
  try {
    const data = await getAccountBalance()
    balanceList.value = data || []
  } catch (e) {
    if (import.meta.env.DEV) console.warn('加载账户余额失败:', e)
    ElMessage.warning('余额汇总加载失败')
    balanceList.value = []
  }
}

/**
 * 打开新增/编辑弹窗
 * @param {Object|null} row - 传入行数据为编辑模式，不传为新增模式
 */
function openDialog(row) {
  isEdit.value = !!row
  editId.value = row?.id || null
  if (row) {
    // 编辑模式：回填表单数据
    formData.name = row.name
    formData.type = row.type
    formData.initialBalance = Number(row.initialBalance || 0)
    formData.currency = row.currency || 'CNY'
  } else {
    // 新增模式：重置表单
    formData.name = ''
    formData.type = null
    formData.initialBalance = 0
    formData.currency = 'CNY'
  }
  dialogVisible.value = true
}

/**
 * 提交表单（新增或编辑）
 * → 调用 api/account.js 的 createAccount() 或 updateAccount()
 */
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      // 编辑模式 → 调用 updateAccount(id, data)
      await updateAccount(editId.value, formData)
      ElMessage.success('更新成功')
    } else {
      // 新增模式 → 调用 createAccount(data)
      await createAccount(formData)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    // 操作成功后刷新列表和余额
    loadAccounts()
    loadBalance()
  } finally {
    submitting.value = false
  }
}

/**
 * 删除账户（二次确认后执行）
 * → 调用 api/account.js 的 deleteAccount(id)
 */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该账户吗？', '提示', { type: 'warning' })
    deletingId.value = row.id
    await deleteAccount(row.id)
    ElMessage.success('删除成功')
    loadAccounts()
    loadBalance()
  } catch (e) {
    if (e === 'cancel') {
      // 用户取消删除，无需处理
    } else {
      // 其他错误（网络异常等）由 axios 拦截器统一处理，此处记录日志便于排查
      console.error('删除账户失败:', e)
    }
  } finally {
    deletingId.value = null
  }
}

// 页面挂载时加载账户列表和余额汇总 + 汇率（P2-4）
onMounted(() => {
  loadAccounts()
  loadBalance()
  loadExchangeRates()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  color: var(--color-title);
}

.balance-card {
  margin-top: 20px;
}

.balance-item {
  text-align: center;
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
}

.balance-name {
  font-size: 14px;
  color: var(--color-muted);
  margin-bottom: 8px;
}

.balance-amount {
  font-size: 18px;
  font-weight: bold;
  color: var(--el-color-primary);
}
</style>
