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
      <!-- el-button：Element Plus 按钮，type="primary" 蓝色主按钮，@click 打开新增弹窗 -->
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon>新增账户  <!-- el-icon + @element-plus/icons-vue Plus 图标 -->
      </el-button>
    </div>

    <!-- 账户列表表格 -->
    <!-- el-card：Element Plus 卡片容器，包裹 el-table -->
    <el-card shadow="hover">
      <!-- el-table：Element Plus 表格，stripe 斑马纹，v-loading 绑定 loading 状态 -->
      <el-table :data="accountList" v-loading="loading" stripe>
        <!-- el-empty：空数据占位组件（当 accountList 为空时显示） -->
        <template #empty><el-empty description="暂无账户" /></template>
        <!-- el-table-column：表格列，prop 绑定数据字段名 -->
        <el-table-column prop="name" label="账户名称" min-width="120" />
        <!-- 账户类型：数字映射为中文显示（1=现金, 2=银行卡, 3=支付宝, 4=微信） -->
        <el-table-column prop="type" label="账户类型" width="100">
          <!-- #default 插槽：自定义列内容，row 为当前行数据 -->
          <template #default="{ row }">
            {{ accountTypeMap[row.type] || row.type }}  <!-- 从 ACCOUNT_TYPE_MAP 常量映射中文名 -->
          </template>
        </el-table-column>
        <el-table-column prop="initialBalance" label="初始余额" width="120">
          <template #default="{ row }">
            ¥ {{ formatAmount(row.initialBalance) }}  <!-- formatAmount → utils/format.js -->
          </template>
        </el-table-column>
        <el-table-column prop="currency" label="币种" width="80" />
        <!-- 状态标签：1=启用(success绿色), 0=停用(info灰色) -->
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <!-- el-tag：Element Plus 标签，动态 type：启用=success(绿)/停用=info(灰) -->
            <el-tag :type="row.status === STATUS_ACTIVE ? 'success' : 'info'">
              {{ statusMap[row.status] || '未知' }}  <!-- 从 STATUS_MAP 常量映射中文名 -->
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}  <!-- formatTime → utils/format.js -->
          </template>
        </el-table-column>
        <!-- 操作列：编辑 + 删除，fixed="right" 右固定列 -->
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <!-- el-button link：无边框文本按钮 -->
            <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)" :disabled="deletingId === row.id">删除</el-button>
            <!-- :disabled 绑定：当前行正在删除中时禁用按钮防重复点击 -->
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 余额汇总卡片：每个账户显示当前余额 + 非CNY币种显示CNY等值换算（P2-4） -->
    <!-- v-if：仅在有余额数据时渲染 -->
    <el-card shadow="hover" class="balance-card" v-if="balanceList.length > 0">
      <template #header>账户余额汇总</template>
      <el-row :gutter="16">  <!-- 栅格间距 16px -->
        <el-col v-for="item in balanceList" :key="item.accountId" :xs="12" :sm="6">
          <!-- xs=12 手机端每行2个，sm=6 宽屏每行4个 -->
          <div class="balance-item">
            <div class="balance-name">{{ item.accountName }}</div>
            <div class="balance-amount">¥ {{ formatAmount(item.currentBalance) }}</div>
            <!-- P2-4: 非CNY币种展示CNY等值换算 -->
            <div v-if="accountCurrency(item.accountId) !== 'CNY'" class="balance-cny-equivalent">
              折合 ≈ ¥ {{ formatCnyEquivalent(item) }}
              <!-- formatCnyEquivalent() 通过汇率数据换算 -->
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- el-dialog：Element Plus 弹窗对话框，新增/编辑账户 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑账户' : '新增账户'" width="480px" destroy-on-close>
      <!-- destroy-on-close：关闭弹窗时销毁DOM，防止下次回显残留 -->
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="账户名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入账户名称" />
        </el-form-item>
        <el-form-item label="账户类型" prop="type">
          <!-- el-select：Element Plus 下拉选择器 -->
          <el-select v-model="formData.type" placeholder="请选择账户类型" style="width: 100%">
            <!-- el-option：下拉选项，:value 绑定数字类型值 -->
            <el-option label="现金" :value="1" />
            <el-option label="银行卡" :value="2" />
            <el-option label="支付宝" :value="3" />
            <el-option label="微信" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="初始余额" prop="initialBalance">
          <!-- el-input-number：Element Plus 数字输入框，:precision="2" 保留2位小数 -->
          <el-input-number v-model="formData.initialBalance" :precision="2" :step="AMOUNT_STEP_ROUGH" :min="0" :max="MAX_ACCOUNT_BALANCE" style="width: 100%" />
        </el-form-item>
        <el-form-item label="币种" prop="currency">
          <el-select v-model="formData.currency" placeholder="请选择币种" style="width: 100%">
            <!-- v-for 遍历 CURRENCY_LIST 常量生成币种选项 -->
            <el-option v-for="c in CURRENCY_LIST" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>  <!-- #footer 插槽：自定义弹窗底部按钮 -->
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'    // Q-CR修复：添加computed用于accountCurrencyMap性能优化
import { ElMessage, ElMessageBox } from 'element-plus'      // 导入消息和确认框
// → 调用 api/account.js 的 5 个接口函数
import { getAccountList, createAccount, updateAccount, deleteAccount, getAccountBalance } from '../api/account' // 导入账户API
// → P2-4 多币种：调用 api/exchange-rate.js 的 getExchangeRates()（汇率数据）
import { getExchangeRates } from '../api/exchange-rate'      // 导入汇率API
import { formatTime, formatAmount } from '../utils/format'  // 导入格式化工具
import { ACCOUNT_TYPE_MAP, CURRENCY_LIST, STATUS_MAP, STATUS_ACTIVE, MAX_ACCOUNT_BALANCE, ACCOUNT_NAME_MAX_LENGTH, AMOUNT_STEP_ROUGH } from '../constants/finance' // 导入常量
import { logger } from '../utils/logger'                    // 导入统一日志工具

const log = logger('AccountPage')                          // 创建日志实例

/** P2-4: 汇率数据缓存（1外币→CNY），例 { USD: 7.3, EUR: 7.94, ... } */
const exchangeRates = ref({})                               // 汇率缓存对象

// 账户类型映射（从常量文件统一管理，避免硬编码散布各页面）
const accountTypeMap = ACCOUNT_TYPE_MAP                     // 账户类型映射
const statusMap = STATUS_MAP                                // 状态映射

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
  name: '',                                                // 账户名称
  type: null,                                              // 账户类型
  initialBalance: 0,                                       // 初始余额
  currency: 'CNY'                                          // 币种（默认人民币）
})

// 表单校验规则
const formRules = {
  name: [{ required: true, message: '请输入账户名称', trigger: 'blur' }, { max: ACCOUNT_NAME_MAX_LENGTH, message: `账户名称不超过${ACCOUNT_NAME_MAX_LENGTH}个字符`, trigger: 'blur' }], // 使用常量 constants/finance.js
  type: [{ required: true, message: '请选择账户类型', trigger: 'change' }], // 类型必选
  initialBalance: [                                        // 余额校验
    { required: true, message: '请输入初始余额', trigger: 'blur' }, // 必填
    { type: 'number', min: 0, message: '初始余额不能为负数', trigger: 'blur' } // 最小值0
  ],
  currency: [{ required: true, message: '请选择币种', trigger: 'change' }] // 币种必选
}

// Q-CR修复：预计算 accountId→currency Map，O(1)查找替代 O(n) 的 .find()，避免 v-for 中 O(n*m) 性能退化
const accountCurrencyMap = computed(() => {
  const map = {}
  accountList.value.forEach(a => { map[a.id] = a.currency || 'CNY' })
  return map
})

/**
 * P2-4: 根据 accountId 查找账户的币种（使用预计算Map实现O(1)查找）
 * @param {Number} accountId - 账户 ID
 * @returns {String} 币种代码（如 CNY/USD/EUR）
 */
function accountCurrency(accountId) {
  return accountCurrencyMap.value[accountId] || 'CNY'       // 从预计算Map查找（O(1)替代O(n)）
}

/**
 * P2-4: 将指定账户的余额换算为 CNY 等值（非 CNY 币种用硬编码汇率换算）
 * @param {Object} balanceItem - 余额汇总项 { accountId, currentBalance }
 * @returns {String} 格式化后的 CNY 等值金额
 */
function formatCnyEquivalent(balanceItem) {
  const currency = accountCurrency(balanceItem.accountId)   // 获取账户币种
  if (currency === 'CNY') return formatAmount(balanceItem.currentBalance) // CNY直接格式化
  const rate = exchangeRates.value[currency]                // 获取汇率
  if (!rate) return 'N/A'                                   // 无汇率返回N/A
  return formatAmount(Number(balanceItem.currentBalance) * Number(rate)) // 汇率换算
}

/**
 * P2-4: 加载汇率数据（通过 api/exchange-rate.js 封装函数调用 GET /api/exchange-rate）
 * 返回 { ratesInverse: { USD: "7.3000", ... } }
 */
async function loadExchangeRates() {
  try {
    const data = await getExchangeRates()                    // 调用汇率API
    if (data && data.ratesInverse) {
      // ratesInverse 是 1外币→CNY 的汇率（如 USD: 7.3 表示 1美元=7.3人民币）
      exchangeRates.value = data.ratesInverse               // 缓存汇率数据
    }
  } catch (e) {
    log.warn('加载汇率数据失败:', e)
    ElMessage.warning('汇率数据加载失败，CNY换算暂不可用')    // 降级提示
    // exchangeRates 保持空，换算显示 N/A
  }
}

/**
 * 加载账户列表
 * → 调用 api/account.js 的 getAccountList()
 */
async function loadAccounts() {
  loading.value = true                                      // 开启loading
  try {
    const data = await getAccountList()                      // 调用账户列表API（api/account.js getAccountList）
    accountList.value = data || []                           // 设置账户数据
  } catch (e) {
    log.warn('加载账户列表失败:', e) /* 开发环境日志 */
    ElMessage.error('账户列表加载失败，请刷新重试')            // 用户级错误提示
    accountList.value = []                                   // 清空数据防止显示残留
  } finally {
    loading.value = false                                    // 关闭loading
  }
}

/**
 * 加载账户余额汇总
 * → 调用 api/account.js 的 getAccountBalance()
 */
async function loadBalance() {
  try {
    const data = await getAccountBalance()                   // 调用余额汇总API
    balanceList.value = data || []                           // 设置余额数据
  } catch (e) {
    log.warn('加载账户余额失败:', e) // 开发环境日志
    ElMessage.warning('余额汇总加载失败')                     // 降级提示
    balanceList.value = []                                   // 清空余额数据
  }
}

/**
 * 打开新增/编辑弹窗
 * @param {Object|null} row - 传入行数据为编辑模式，不传为新增模式
 */
function openDialog(row) {
  isEdit.value = !!row                                      // 判断是否编辑模式
  editId.value = row?.id || null                            // 获取编辑ID
  if (row) {
    // 编辑模式：回填表单数据
    formData.name = row.name                                // 回填名称
    formData.type = row.type                                // 回填类型
    formData.initialBalance = Number(row.initialBalance || 0) // 回填余额
    formData.currency = row.currency || 'CNY'               // 回填币种
  } else {
    // 新增模式：重置表单
    formData.name = ''                                      // 清空名称
    formData.type = null                                    // 清空类型
    formData.initialBalance = 0                             // 重置余额
    formData.currency = 'CNY'                               // 默认人民币
  }
  dialogVisible.value = true                                // 显示弹窗
}

/**
 * 提交表单（新增或编辑）
 * → 调用 api/account.js 的 createAccount() 或 updateAccount()
 */
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false) // 触发校验
  if (!valid) return                                        // 校验不通过不提交

  submitting.value = true                                   // 开启提交loading
  try {
    if (isEdit.value) {
      // 编辑模式 → 调用 api/account.js 的 updateAccount(id, data)
      await updateAccount(editId.value, formData)           // 调用更新API
      ElMessage.success('更新成功')                          // 成功提示
    } else {
      // 新增模式 → 调用 api/account.js 的 createAccount(data)
      await createAccount(formData)                         // 调用创建API
      ElMessage.success('新增成功')                          // 成功提示
    }
    dialogVisible.value = false                             // 关闭弹窗
    // 操作成功后刷新列表和余额（await 确保刷新完成后再关闭 loading，避免数据闪烁和未捕获的 Promise rejection）
    await loadAccounts()                                    // 刷新账户列表（→ api/account.js getAccountList()）
    await loadBalance()                                     // 刷新余额汇总（→ api/account.js getAccountBalance()）
  } catch (e) {
    log.warn('保存账户失败:', e) /* 开发环境日志 */
    // axios 拦截器（api/request.js）已统一处理业务错误弹窗，此处仅处理拦截器未覆盖的网络/超时异常
    if (e.code === 'ERR_NETWORK' || e.code === 'ECONNABORTED') {  // 网络或超时错误
      ElMessage.error('网络异常，账户保存失败')              // 网络级错误提示
    }
  } finally {
    submitting.value = false                                // 关闭提交loading
  }
}

/**
 * 删除账户（二次确认后执行）
 * → 调用 api/account.js 的 deleteAccount(id)
 */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该账户吗？', '提示', { type: 'warning' }) // 删除确认
    deletingId.value = row.id                               // 标记正在删除的行
    await deleteAccount(row.id)                              // 调用删除API（→ api/account.js deleteAccount()）
    ElMessage.success('删除成功')                            // 成功提示
    await loadAccounts()                                    // 刷新账户列表（→ api/account.js getAccountList()）
    await loadBalance()                                     // 刷新余额汇总（→ api/account.js getAccountBalance()）
  } catch (e) {
    if (e === 'cancel' || e === 'close') {                     // 用户取消（按钮）或关闭（X/遮罩）
      // 用户取消删除或关闭弹窗，无需处理
    } else {
      // 其他错误（网络异常等）由 axios 拦截器统一处理，此处记录日志便于排查
      log.error('删除账户失败:', e)                     // 记录错误日志
    }
  } finally {
    deletingId.value = null                                 // 重置删除标记
  }
}

// 页面挂载时加载账户列表和余额汇总 + 汇率（P2-4 · async+Promise.all 并行加载）
onMounted(async () => {
  try {
    await Promise.all([                                        // 并行加载三项数据（互不依赖）
      loadAccounts(),                                          // 加载账户列表
      loadBalance(),                                           // 加载余额汇总
      loadExchangeRates()                                      // 加载汇率数据
    ])
  } catch (e) {
    log.error('页面初始化加载失败:', e)                          // 记录错误日志（各子函数已有独立错误处理）
  }
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

/* 修复：添加缺失的 CNY 等值余额样式类（AccountPage.vue 第 79 行引用但此前未定义） */
.balance-cny-equivalent {
  font-size: 12px;
  color: var(--color-muted, #909399);
  margin-top: 4px;
}
</style>
