<!--
  ╔══════════════════════════════════════════════════════════════════════╗
  ║  📋 答辩文件 ⑥/⑦ — ★ 核心代码讲解（30 分重点）★                         ║
  ║                                                                      ║
  ║  【文件整体实现什么】                                                    ║
  ║  TransactionListPage.vue — 收支记录页面，放在 views/ 目录，路由 /transaction   ║
  ║  整个系统最复杂的页面之一：筛选栏 + 分页表格 + 记一笔弹窗 + URL同步               ║
  ║  <template> 有 4 个功能区块：筛选栏/交易表格/分页栏/新增编辑弹窗                   ║
  ║  <script setup> 含 7 个核心函数，重点讲 3 个：loadTransactions / handleSubmit / handleSearch ║
  ║                                                                      ║
  ║  【答辩要讲什么】                                                        ║
  ║  重点讲 3 个核心函数（当前文件第 294-440 行左右），覆盖前端开发的完整范式：         ║
  ║    loadTransactions() — 构建筛选参数 + 调API + 兼容响应 + loading/错误处理     ║
  ║    handleSubmit() — 表单校验 + 新增/编辑分支 + 转账保护 + 加载刷新             ║
  ║    syncFiltersToUrl() — 筛选条件持久化到URL + router.replace防历史污染         ║
  ║                                                                      ║
  ║  【讲解步骤】                                                           ║
  ║  1. 开场白（10秒）：为什么选收支记录页——筛选/分页/CRUD/URL同步，全链路           ║
  ║  2. 花 20 秒展示 <template> 四大区块（筛选栏/表格/分页/弹窗）                     ║
  ║  3. ★ 重点：依次讲解 loadTransactions / handleSubmit / syncFiltersToUrl      ║
  ║     每段代码已在文件内标注了详细的答辩注释，直接念即可                            ║
  ║  4. 收尾串讲：前端 3 个核心功能覆盖 5 个知识点                         ║
  ║                                                                      ║
  ║  【具体讲稿开场白】                                                       ║
  ║  "老师好，我选的前端组件是 TransactionListPage.vue——收支记录页面。              ║
  ║   这是系统里最复杂的页面：多条件筛选+分页+新增编辑+URL状态持久化。               ║
  ║   三个核心函数 loadTransactions/handleSubmit/syncFiltersToUrl               ║
  ║   覆盖了前端开发完整范式：API调用→状态管理→URL同步→错误处理。"                    ║
  ║                                                                      ║
  ║  第 294 行 loadTransactions()：构建params→调API→兼容响应→loading/错误兜底       ║
  ║  第 440 行 handleSubmit()：validate校验→新增/编辑分支→转账保护→刷新列表          ║
  ║  第 346 行 syncFiltersToUrl()：筛选条件写入URL→router.replace→防历史污染        ║
  ║                                                                      ║
  ║  收尾："这3个函数覆盖了5个知识点：async/await异步、Promise.all并行、             ║
  ║    reactive筛选状态、URL↔状态双向同步、Element Plus表单校验。"                  ║
  ╚══════════════════════════════════════════════════════════════════════╝

  ▶ 讲完后，下一个文件（最后一个，按 Ctrl+P 粘贴打开）：
    system/frontend/src/router/index.js
    （路由守卫 — 前端怎么在页面切换时检查登录状态、拦截未登录用户）

  收支记录页面
  路由：/transaction
  对应 PRD 功能：
    - P0 收支记录（记一笔 + 改 + 列表分页）
    - P1 多条件筛选（时间/账户/分类/关键词）

  功能说明：
    - 顶部筛选栏：日期范围 + 账户 + 分类 + 关键词搜索
    - 交易记录表格：时间/账户/分类/类型/金额/备注/转账标识/操作
    - 分页组件（pageNum + pageSize）
    - 「记一笔」新增弹窗 + 编辑弹窗
    - 筛选条件持久化到 URL query params（支持浏览器前进后退）

  调用关系：
    → 调用 api/transaction.js 的 getTransactionList()（加载交易列表，支持筛选+分页）
    → 调用 api/transaction.js 的 createTransaction()（记一笔）
    → 调用 api/transaction.js 的 updateTransaction()（编辑记录）
    → 调用 api/transaction.js 的 deleteTransaction()（删除记录，转账记录不显示删除按钮）
    → 调用 api/account.js 的 getAccountList()（加载账户下拉选项）
    → 调用 api/category.js 的 getCategoryList()（加载分类下拉选项）
-->
<template>
  <div class="transaction-page">
    <div class="page-header">
      <h2>收支记录</h2>
      <!-- el-button：Element Plus 按钮，"记一笔"新增入口 → openDialog() -->
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon>记一笔  <!-- @element-plus/icons-vue Plus 图标 -->
      </el-button>
    </div>

    <!-- 筛选栏（PRD P1-1: 多条件筛选） -->
    <!-- el-card：Element Plus 卡片，role="search" 无障碍语义 -->
    <el-card shadow="hover" class="filter-card" role="search" aria-label="筛选条件">
      <!-- el-form :inline="true"：行内表单，表单项水平排列 -->
      <el-form :inline="true" :model="filters">
        <el-form-item label="日期范围">
          <!-- el-date-picker type="daterange"：Element Plus 日期范围选择器 -->
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item label="账户">
          <!-- el-select clearable：Element Plus 下拉选择器，可清空 -->
          <el-select v-model="filters.accountId" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="acc in accountList" :key="acc.id" :label="acc.name" :value="acc.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="filters.categoryId" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="cat in categoryList" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <!-- el-input clearable：Element Plus 输入框，可一键清空 -->
          <el-input v-model="filters.keyword" placeholder="备注搜索" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>  <!-- 触发筛选 + URL 同步 -->
          <el-button @click="resetFilters">重置</el-button>    <!-- 清空筛选条件 + URL 参数 -->
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 交易记录列表 -->
    <el-card shadow="hover">
      <el-table :data="transactionList" v-loading="loading" stripe>
        <template #empty><el-empty description="暂无收支记录" /></template>
        <el-table-column prop="time" label="时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.time) }}  <!-- formatTime → utils/format.js -->
          </template>
        </el-table-column>
        <el-table-column prop="accountName" label="账户" width="120" />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <!-- 类型标签：1=收入(绿色 success), 2=支出(红色 danger) -->
        <el-table-column prop="type" label="类型" width="80">
          <template #default="{ row }">
            <!-- el-tag size="small"：Element Plus 小号标签 -->
            <el-tag :type="row.type === TRANSACTION_TYPE_INCOME ? 'success' : 'danger'" size="small">
              {{ TRANSACTION_TYPE_MAP[row.type]?.label || '未知' }}  <!-- 从常量映射中文 -->
            </el-tag>
          </template>
        </el-table-column>
        <!-- 金额：收入显示 +，支出显示 - -->
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">
            <!-- 动态 CSS 类：收入绿色(amount-income) / 支出红色(amount-expense) -->
            <span :class="row.type === TRANSACTION_TYPE_INCOME ? 'amount-income' : 'amount-expense'">
              {{ TRANSACTION_TYPE_MAP[row.type]?.sign || '' }}¥ {{ formatAmount(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <!-- show-overflow-tooltip：内容超出列宽时显示省略号 + tooltip -->
        <el-table-column prop="note" label="备注" min-width="150" show-overflow-tooltip />
        <!-- 转账标识：有 transferId 的记录标记为「转入/转出」 -->
        <el-table-column label="转账标识" width="100">
          <template #default="{ row }">
            <!-- el-tag：Element Plus 标签，转账支出=warning(橙)转出，转账收入=success(绿)转入 -->
            <el-tag v-if="row.transferId && row.type === TRANSACTION_TYPE_EXPENSE" type="warning" size="small">(转出)</el-tag>
            <el-tag v-else-if="row.transferId && row.type === TRANSACTION_TYPE_INCOME" type="success" size="small">(转入)</el-tag>
            <!-- 兜底条件：type 只能是 1(收入) 或 2(支出)，此分支理论上不会触发，保留以防御未来枚举扩展 -->
            <el-tag v-else-if="row.transferId && row.type !== TRANSACTION_TYPE_EXPENSE && row.type !== TRANSACTION_TYPE_INCOME" type="warning" size="small">转账</el-tag>
          </template>
        </el-table-column>
        <!-- 操作列：fixed="right" 右侧固定列（水平滚动时保持可见） -->
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
            <!-- 转账记录不显示删除按钮（v-if="!row.transferId"） -->
            <el-button
              v-if="!row.transferId"
              type="danger"
              link
              @click="handleDelete(row)"
              :disabled="deletingId === row.id"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- el-pagination：Element Plus 分页组件（对齐 API_DESIGN.md §1 分页参数 pageNum + pageSize） -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="PAGE_SIZE_OPTIONS"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- el-dialog：Element Plus 弹窗，记一笔/编辑记录 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑记录' : '记一笔'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="账户" prop="accountId">
          <!-- 编辑转账记录时禁用账户选择（:disabled="isEdit && isTransfer"），防止破坏转账关联 -->
          <el-select v-model="formData.accountId" placeholder="请选择账户" style="width: 100%" :disabled="isEdit && isTransfer">
            <el-option v-for="acc in accountList" :key="acc.id" :label="acc.name" :value="acc.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <!-- filteredCategories 计算属性：根据当前类型动态过滤分类列表 -->
          <el-select v-model="formData.categoryId" placeholder="请选择分类" style="width: 100%" :disabled="isEdit && isTransfer">
            <el-option v-for="cat in filteredCategories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <!-- el-radio-group：Element Plus 单选按钮组，支出/收入切换 -->
          <el-radio-group v-model="formData.type" :disabled="isEdit && isTransfer">
            <el-radio :value="TRANSACTION_TYPE_EXPENSE">支出</el-radio>    <!-- value=2 -->
            <el-radio :value="TRANSACTION_TYPE_INCOME">收入</el-radio>    <!-- value=1 -->
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="formData.amount" :precision="2" :min="MIN_TRANSACTION_AMOUNT" :step="AMOUNT_STEP_PRECISE" style="width: 100%" :disabled="isEdit && isTransfer" />
        </el-form-item>
        <el-form-item label="备注" prop="note">
          <!-- el-input type="textarea"：Element Plus 多行文本输入 -->
          <el-input v-model="formData.note" type="textarea" placeholder="备注（可选）" />
        </el-form-item>
        <el-form-item label="时间" prop="time">
          <!-- el-date-picker type="datetime"：Element Plus 日期时间选择器 -->
          <el-date-picker v-model="formData.time" type="datetime" placeholder="选择时间" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" :disabled="isEdit && isTransfer" />
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
import { ref, reactive, computed, onMounted, watch } from 'vue' // 导入Vue组合式API
import { useRoute, useRouter } from 'vue-router'            // 导入路由
import { ElMessage, ElMessageBox } from 'element-plus'      // 导入消息和确认框
// → 调用 api/transaction.js 的 4 个接口
import { getTransactionList, createTransaction, updateTransaction, deleteTransaction } from '../api/transaction' // 导入交易API
// → 调用 api/account.js 的 getAccountList()（下拉选项）
import { getAccountList } from '../api/account'              // 导入账户API
// → 调用 api/category.js 的 getCategoryList()（下拉选项）
import { getCategoryList } from '../api/category'             // 导入分类API
import { formatTime, formatDateTime, formatAmount } from '../utils/format' // 导入格式化工具
import { TRANSACTION_TYPE_MAP, TRANSACTION_TYPE_INCOME, TRANSACTION_TYPE_EXPENSE, CATEGORY_TYPE_EXPENSE, CATEGORY_TYPE_INCOME, MIN_TRANSACTION_AMOUNT, MAX_NOTE_LENGTH, DEFAULT_PAGE_SIZE, PAGE_SIZE_OPTIONS, AMOUNT_STEP_PRECISE } from '../constants/finance' // 导入常量
import { logger } from '../utils/logger' // 导入日志工具

const log = logger('TransactionListPage') // 创建日志实例

const route = useRoute()                                    // 当前路由信息
const router = useRouter()                                  // 路由实例

const loading = ref(false)                                  // 页面loading
const submitting = ref(false)                               // 提交loading
const deletingId = ref(null)                                // 正在删除的记录ID
const dialogVisible = ref(false)                            // 弹窗显隐
const isEdit = ref(false)           // 是否编辑模式
const editId = ref(null)            // 编辑的记录 ID
const isTransfer = ref(false)       // 当前编辑的记录是否为转账记录（转账记录禁用部分字段修改）
const formRef = ref(null)                                    // 表单引用

const transactionList = ref([])     // 交易记录列表
const accountList = ref([])         // 账户下拉选项
const categoryList = ref([])        // 分类下拉选项

// 筛选条件（PRD P1-1 多条件筛选）
const TIME_START_OF_DAY = ' 00:00:00'
const TIME_END_OF_DAY = ' 23:59:59'

const filters = reactive({
  dateRange: null,      // 日期范围 [startDate, endDate]
  accountId: null,      // 按账户筛选
  categoryId: null,     // 按分类筛选
  keyword: ''           // 关键词搜索（备注）
})

// 分页参数
const pagination = reactive({
  pageNum: 1,                                               // 当前页码
  pageSize: DEFAULT_PAGE_SIZE,                               // 每页条数（使用常量 constants/finance.js）
  total: 0                                                  // 总条数
})

// 新增/编辑表单数据
const formData = reactive({
  accountId: null,                                          // 账户ID
  categoryId: null,                                         // 分类ID
  type: TRANSACTION_TYPE_EXPENSE,        // 默认支出
  amount: null,                                             // 金额
  note: '',                                                 // 备注
  time: ''                                                  // 时间
})

// 表单校验规则
const formRules = {
  accountId: [{ required: true, message: '请选择账户', trigger: 'change' }], // 账户必选
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }], // 分类必选
  type: [{ required: true, message: '请选择类型', trigger: 'change' }], // 类型必选
  amount: [                                                 // 金额校验
    { required: true, message: '请输入金额', trigger: 'blur' }, // 必填
    { type: 'number', min: MIN_TRANSACTION_AMOUNT, message: '金额必须大于0', trigger: 'blur' } // 最小值（使用常量：constants/finance.js MIN_TRANSACTION_AMOUNT）
  ],
  time: [{ required: true, message: '请选择时间', trigger: 'change' }], // 时间必选
  note: [{ max: MAX_NOTE_LENGTH, message: `备注长度不能超过${MAX_NOTE_LENGTH}`, trigger: 'blur' }] // 备注长度（使用常量：constants/finance.js MAX_NOTE_LENGTH）
}

/**
 * 根据当前选择的类型动态过滤可选分类
 * 类型映射关系：
 *   category.type: 1=支出, 2=收入
 *   transaction.type: 1=收入, 2=支出
 *   选支出(2) → 显示 category.type=1 的分类
 *   选收入(1) → 显示 category.type=2 的分类
 */
const filteredCategories = computed(() => {
  const categoryType = formData.type === TRANSACTION_TYPE_EXPENSE ? CATEGORY_TYPE_EXPENSE : CATEGORY_TYPE_INCOME // 反转映射类型值
  return categoryList.value.filter(item => item.type === categoryType) // 筛选匹配分类
})

// 切换类型时重置分类选择→ 避免选中不匹配的分类
watch(() => formData.type, () => {
  formData.categoryId = null                                // 重置分类选择
})

/** 分页 pageSize 变化处理（重置到第1页并加载） */
function handleSizeChange(size) {
  pagination.pageSize = size                                // 更新每页条数
  pagination.pageNum = 1                                    // 重置到第1页
  syncFiltersToUrl()                                        // 修复：同步分页状态到 URL（浏览器前进/后退按钮可恢复）→ 调用 router.replace
  loadTransactions()                                        // 重新加载列表
}

/** 分页页码变化处理 */
function handlePageChange(page) {
  pagination.pageNum = page                                 // 更新页码
  syncFiltersToUrl()                                        // 修复：同步分页状态到 URL（浏览器前进/后退按钮可恢复）→ 调用 router.replace
  loadTransactions()                                        // 重新加载列表
}

/**
 * ★★【答辩·函数1：加载交易记录列表（支持筛选 + 分页）】★★
 * → 调用 api/transaction.js 的 getTransactionList(params)
 * 覆盖知识点：async/await异步、reactive状态管理、可选链?.防御、try/catch/finally完整错误处理
 */
async function loadTransactions() {
  // ★ 做什么：设置el-table的v-loading为true→表格显示骨架屏loading动画 / 为什么：给用户即时视觉反馈，知道数据在加载中
  loading.value = true
  try {
    // ★ 做什么：构建请求参数对象，包含必填的分页参数 / 为什么：后端要求pageNum从1开始，pageSize默认10
    const params = {
      pageNum: pagination.pageNum,                          // ★ 页码（reactive响应式→修改时自动触发UI更新）
      pageSize: pagination.pageSize                         // ★ 每页条数（reactive响应式）
    }
    // ★★ 拼接筛选参数——每个条件都用if判断，只有用户实际选择了才拼接 ★★
    //  为什么用if而非三目运算符全拼：避免把null/undefined/空字符串传给后端——后端可能把空字符串当有效值导致查询异常
    if (filters.dateRange) {                                // ★ dateRange是el-date-picker的v-model绑定值，格式为[startDate, endDate]
      params.startTime = filters.dateRange[0] + TIME_START_OF_DAY // ★ 日期拼接" 00:00:00"→后端能直接比较的datetime格式
      params.endTime = filters.dateRange[1] + TIME_END_OF_DAY   // ★ 拼接" 23:59:59"→涵盖一整天，不遗漏当天最后1秒的数据
    }
    if (filters.accountId != null) params.accountId = filters.accountId // ★ !=null而非if(accountId)：因为0是合法ID，但0是falsy值会被if跳过
    if (filters.categoryId != null) params.categoryId = filters.categoryId // ★ 同理用!=null防御0值
    if (filters.keyword) params.keyword = filters.keyword  // ★ 关键词可以为空字符串——用户清空搜索时keyword=''，不传此参数

    // ★ 做什么：await异步等待后端响应→api/transaction.js的getTransactionList发送GET /api/v1/transaction?pageNum=1&...
    //  为什么await：JavaScript异步非阻塞——不加await拿到的是Promise对象而非数据；加了await才真正"等网络返回"
    const data = await getTransactionList(params)
    // ★ 做什么：可选链?. + || 链式兜底——兼容后端三种可能的返回结构 / 为什么：后端重构或分页插件切换时返回字段名可能变化（records/list/直接数组），前端兼容不用改
    transactionList.value = data?.records || data?.list || data || []
    pagination.total = data?.total || 0                     // ★ total控制el-pagination的总条数显示和页码计算
  } catch (e) {
    // ★ 做什么：catch捕获网络异常（断网/超时/5xx服务器错误） / 为什么：业务异常（如密码错误）已被axios响应拦截器处理过，这里只捕获网络层异常
    log.warn('加载交易列表失败:', e)
    ElMessage.error('交易数据加载失败，请检查筛选条件后重试')  // ★ 用户级错误提示——明确告诉用户"怎么办"而不仅是"出错了"
    transactionList.value = []                              // ★ 错误兜底：清空列表数据——如果不置空，老数据残留会让用户误以为数据还在
    pagination.total = 0                                    // ★ 重置总数——分页组件依赖total显示页码，不重置会导致分页条显示旧数据的页数
  } finally {
    // ★ 做什么：无论成功/失败/异常，都关闭loading / 为什么：finally保证一定执行——如果只在try里关loading，catch抛异常后loading永远不关，按钮转圈到天荒地老
    loading.value = false
  }
}

/**
 * 加载下拉选项数据（账户 + 分类）
 * → 调用 api/account.js 的 getAccountList() + api/category.js 的 getCategoryList()
 * 并行请求提高加载速度
 */
// → 调用 api/account.js 的 getAccountList() + api/category.js 的 getCategoryList()
async function loadOptions() {
  try {
    const [accounts, categories] = await Promise.all([getAccountList(), getCategoryList()]) // 并行加载
    accountList.value = accounts || []                       // 设置账户选项
    categoryList.value = categories || []                    // 设置分类选项
  } catch (e) {
    log.warn('加载选项数据失败:', e) // 开发环境日志
    ElMessage.error('账户/分类选项加载失败，下拉框可能为空，请刷新页面重试') // 明确错误提示（含影响说明）
  }
}

/**
 * ★★【答辩·函数3：同步筛选条件到 URL query params（PRD P1-1）】★★
 * 做什么：把当前的筛选条件（日期/账户/分类/关键词/页码）写入浏览器地址栏URL的query参数
 * 为什么用 router.replace 而非 router.push：
 *   replace替换当前历史记录→不产生新的history条目→用户点"后退"回到上一页而非上一个筛选状态
 *   如果用push——用户每次筛选都产生一条历史——点10次筛选要点10次后退才能离开——这叫"历史污染"
 * 为什么pageNum>1才写入URL：第1页是默认值——不写?page=1可以减少URL噪音——readFiltersFromUrl读不到page默认就是1
 */
function syncFiltersToUrl() {
  const q = {}
  if (filters.dateRange) { q.startDate = filters.dateRange[0]; q.endDate = filters.dateRange[1] }
  if (filters.accountId) q.accountId = filters.accountId   // ★ 只写有值的筛选条件→URL干净清爽
  if (filters.categoryId) q.categoryId = filters.categoryId
  if (filters.keyword) q.keyword = filters.keyword
  if (pagination.pageNum > 1) q.page = pagination.pageNum  // ★ 默认第1页不写→减少URL参数噪音
  router.replace({ query: q })                              // ★ replace≠push：不污染浏览器history栈
}

/**
 * ★★ 从 URL query params 读取筛选条件（页面初始化/浏览器前进后退时调用）★★
 * 做什么：syncFiltersToUrl()的反向操作——把URL参数还原为页面筛选状态
 * 为什么Number()转换：URL query参数都是字符串（"?accountId=3"→"3"是字符串），但el-select的:value是数字类型
 *   不转Number会导致类型不匹配——数字3≠字符串"3"——el-select无法匹配到对应选项、显示空白
 * readFiltersFromUrl + syncFiltersToUrl 形成"URL↔状态双向同步"闭环
 */
function readFiltersFromUrl() {
  const q = route.query
  if (q.startDate && q.endDate) filters.dateRange = [q.startDate, q.endDate]
  if (q.accountId) filters.accountId = Number(q.accountId) // ★ String→Number：URL参数是字符串，el-select value是数字
  if (q.categoryId) filters.categoryId = Number(q.categoryId)
  if (q.keyword) filters.keyword = q.keyword
  if (q.page) pagination.pageNum = Number(q.page)
}

/**
 * ★★ 搜索按钮处理：重置页码+同步URL+重新加载 ★★
 * 做什么：用户点击"搜索"按钮后的三连操作——页码归1→条件写入URL→发请求
 * 为什么loading防抖：if(loading.value)return——防止用户快速双击发出两个并发请求——两次请求谁先返回不确定，可能旧数据覆盖新数据
 */
function handleSearch() {
  if (loading.value) return                                 // ★ loading防抖：正在加载中忽略重复点击
  pagination.pageNum = 1                                    // ★ 重置到第1页——换了筛选条件就要从头看
  syncFiltersToUrl()                                        // ★ 条件写入URL——方便复制链接分享/浏览器后退恢复
  loadTransactions()                                        // ★ 发起HTTP请求加载新数据
}

/** 重置筛选条件 + 清空 URL query params */
function resetFilters() {
  filters.dateRange = null                                  // 清空日期范围
  filters.accountId = null                                  // 清空账户筛选
  filters.categoryId = null                                 // 清空分类筛选
  filters.keyword = ''                                      // 清空关键词
  router.replace({ query: {} })                             // 清空URL参数
  handleSearch()                                            // 重置后搜索
}

/**
 * 打开新增/编辑弹窗
 * @param {Object|null} row - 传入行数据为编辑模式，不传为「记一笔」新增模式
 */
function openDialog(row) {
  isEdit.value = !!row                                      // 判断是否编辑模式
  editId.value = row?.id || null                            // 获取编辑记录ID
  isTransfer.value = !!row?.transferId    // 判断是否为转账记录
  if (row) {
    // 编辑模式：回填表单
    formData.accountId = row.accountId                      // 回填账户ID
    formData.categoryId = row.categoryId                    // 回填分类ID
    formData.type = row.type                                // 回填类型
    formData.amount = Number(row.amount || 0)               // 回填金额
    formData.note = row.note || ''                           // 回填备注
    formData.time = row.time || ''                           // 回填时间
  } else {
    // 新增模式：重置表单，默认时间设为当前
    formData.accountId = null                               // 清空账户
    formData.categoryId = null                              // 清空分类
    formData.type = TRANSACTION_TYPE_EXPENSE                 // 默认支出
    formData.amount = null                                  // 清空金额
    formData.note = ''                                      // 清空备注
    formData.time = formatDateTime(new Date())              // 默认当前时间
  }
  dialogVisible.value = true                                // 显示弹窗
}

/**
 * 删除交易记录
 * → 调用 api/transaction.js 的 deleteTransaction(id)
 * 转账记录不显示删除按钮（已在模板中 v-if="!row.transferId" 隐藏）
 */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除这条${TRANSACTION_TYPE_MAP[row.type]?.label || '未知'}记录吗？`, '确认删除', { // 删除确认
      type: 'warning'                                       // 警告类型
    })
    deletingId.value = row.id                               // 标记正在删除
    await deleteTransaction(row.id)                          // 调用删除API
    ElMessage.success('记录已删除')                          // 成功提示
    await loadTransactions()                                 // 刷新列表（添加await确保列表更新完成）
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {  // 仅真实错误（非用户取消/关闭弹窗）才记录日志（对齐AccountPage/BudgetPage模式）
      // axios 拦截器（api/request.js）已统一处理业务错误，此处记录日志便于排查非业务异常
      log.error('删除记录失败:', e)                    // 记录错误日志
    }
  } finally {
    deletingId.value = null                                 // 重置删除标记
  }
}

/**
 * ★★【答辩·函数2：提交表单（记一笔或编辑）】★★
 * → 调用 api/transaction.js 的 createTransaction() 或 updateTransaction()
 * 覆盖知识点：Element Plus表单校验、新增/编辑分支处理、转账记录字段保护、async/await错误处理
 */
async function handleSubmit() {
  // ★★ 做什么：调Element Plus表单的validate()校验所有el-form-item的rules / 为什么：.catch(()=>false)把validate的reject转成false防止未捕获异常；不通过直接return不发送HTTP请求——省带宽省服务器资源
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return                                        // ★ 校验失败直接退出——前端校验秒级反馈，不用等服务器返回

  submitting.value = true                                   // ★ 按钮进入loading状态→转圈+disabled→防用户狂点重复提交（防抖的UI实现）
  try {
    if (isEdit.value) {
      // ★★ 编辑模式：调用updateTransaction / 为什么区分新增/编辑两个API：后端的update方法对转账记录有特殊保护（只允许改备注不许改金额）
      await updateTransaction(editId.value, formData)       // ★ PUT /api/v1/transaction/{id}——id从editId.value来，数据从reactive的formData来
      ElMessage.success('更新成功')
    } else {
      // ★★ 新增模式：调用createTransaction / 为什么新增和编辑分两个API而不是合并：RESTful规范——POST创建新资源、PUT更新已有资源——语义清晰，后端校验逻辑也不同
      await createTransaction(formData)                     // ★ POST /api/v1/transaction——formData含accountId/categoryId/type/amount/note/time
      ElMessage.success('记账成功')
    }
    dialogVisible.value = false                             // ★ 成功后关闭弹窗——放在try里：只有API调用成功才关，失败了弹窗保持打开方便用户修改
    await loadTransactions()                                // ★ 刷新列表重新加载数据——加await确保列表更新完成后才继续，防止用户立即操作看到旧数据
  } catch (e) {
    // ★ 说明：业务异常（如"金额不能为空"、"分类不存在"）已被axios响应拦截器统一弹ElMessage.error处理
    //   此处只处理响应拦截器覆盖不到的网络层异常（断网/超时）
    log.warn('记账操作失败:', e)
    if (e.code === 'ERR_NETWORK' || e.code === 'ECONNABORTED') {  // ★ ERR_NETWORK=断网/DNS失败、ECONNABORTED=axios10秒超时
      ElMessage.error('网络异常，操作失败')
    }
  } finally {
    submitting.value = false                                // ★ finally保证loading一定关闭——即使catch里抛了新异常也不会跳过这行
  }
}

// 页面挂载时：从 URL 读取筛选条件 → 加载下拉选项 → 加载交易列表
onMounted(async () => {
  readFiltersFromUrl();                                     // 从URL读取筛选条件（同步函数无需await）
  try {
    await Promise.all([                                       // 并行加载下拉选项+交易列表（Promise.all更高效）
      loadOptions(),                                          // 加载下拉选项
      loadTransactions()                                      // 加载交易列表
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

.filter-card {
  margin-bottom: 20px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.amount-income {
  color: var(--color-income);
  font-weight: bold;
}

.amount-expense {
  color: var(--color-expense);
  font-weight: bold;
}
</style>
