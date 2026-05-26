/**
 * 分类模块 API 封装（api/category.js）
 *
 * 职责：封装分类相关的 HTTP 请求（仅查询全量分类列表，不做增改删）
 * 通过 request.js（Axios 实例）发送请求，统一享受拦截器处理（token 注入 + 401 拦截 + 错误提示）
 *
 * 对应后端接口（对齐 API_DESIGN.md 分类模块接口）：
 *   GET /api/v1/category → CategoryController.list()  获取全部分类列表（含收入分类和支出分类）
 *
 * 对应 PRD 功能：P0 分类 GET 列表（种子数据，系统预置，用户不可新增/修改/删除）
 *
 * 调用方（多个页面的下拉选择框和表格使用本模块）：
 *   - CategoryPage.vue → getCategoryList()（分类浏览页面，展示所有分类）
 *   - TransactionListPage.vue → getCategoryList()（记一笔/筛选时的分类下拉选择框）
 *   - BudgetPage.vue → getCategoryList()（设置预算时筛选仅支出分类）
 *   - RecurringBillPage.vue → getCategoryList()（新增/编辑周期账单时的分类下拉框）
 *
 * 数据流向：
 *   .vue 组件 → api/category.js（导出函数）→ request.js（Axios 实例 + 拦截器）→ CategoryController → CategoryServiceImpl → CategoryMapper → MySQL
 *                 ← Result<Category[]> 响应 ← Axios 响应拦截器解析后返回到 .vue 组件
 *
 * 关联文件：
 *   - api/request.js：Axios 实例（baseURL=/api/v1、timeout=10000、请求拦截器注入 token、响应拦截器处理 401/业务错）
 *   - views/CategoryPage.vue：分类浏览页面
 *   - views/TransactionListPage.vue：收支记录页面（分类下拉框使用）
 *   - views/BudgetPage.vue：预算管理页面（仅支出分类可选）
 *   - views/RecurringBillPage.vue：周期账单页面（分类下拉框使用）
 *   - backend/controller/CategoryController.java：分类控制器
 *   - backend/entity/Category.java：分类实体（表名 categories，种子数据预置）
 */
import request from './request'                                    // 导入 Axios 实例（→ api/request.js），包含 baseURL + 拦截器配置

/**
 * 获取全部分类列表（包含收入分类和支出分类，均为系统预置种子数据）
 *
 * 请求详情：GET /api/v1/category
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 无查询参数（返回全量数据，分类总数约 20-30 条，无需分页）
 * 响应体：Result<Category[]>
 *   每个 Category 对象含：{ id, name, type, icon, sortOrder }
 *   - type=1 → 支出分类（餐饮、交通、购物、娱乐等）
 *   - type=2 → 收入分类（工资、奖金、兼职、投资收益等）
 *
 * 注意：分类为系统预置种子数据，前端不提供新增/编辑/删除入口
 *
 * 调用方：CategoryPage.vue / TransactionListPage.vue / BudgetPage.vue / RecurringBillPage.vue
 *
 * @returns {Promise<Array>} - Axios Promise，resolve 后返回分类对象数组
 */
export function getCategoryList() {                                // 导出 getCategoryList 函数（→ 多个页面的分类下拉框和表格使用）
  return request.get('/category')                                  // GET 请求 → /api/v1/category（无 params，返回全量分类列表）
}
