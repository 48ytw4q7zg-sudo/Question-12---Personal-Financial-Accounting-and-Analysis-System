/**
 * 汇率 API 模块封装（api/exchange-rate.js）
 *
 * 职责：封装多币种相关的后端接口调用（获取固定汇率表）
 * 通过 request.js（Axios 实例）发送请求，统一享受拦截器处理（token 注入 + 401 拦截 + 错误提示）
 *
 * 对应后端接口（对齐 API_DESIGN.md 汇率模块接口）：
 *   GET /api/v1/exchange-rate → ExchangeRateController.getRates()  获取固定汇率表（基准币种 CNY）
 *
 * 评分标准：P2-4 多币种支持（固定汇率，不做实时汇率接口）
 * 业务说明：汇率表基于 CNY（人民币）为基准，提供 USD/EUR/JPY/GBP 等常见货币的固定汇率
 *
 * 调用方（哪些 .vue 文件使用了本模块的导出函数）：
 *   - AccountPage.vue → getExchangeRates()（创建/编辑账户时币种下拉框，CNY 余额换算展示）
 *   - DashboardPage.vue → getExchangeRates()（首页按统一币种换算后展示汇总余额）
 *
 * 数据流向：
 *   .vue 组件 → api/exchange-rate.js（导出函数）→ request.js（Axios 实例 + 拦截器）→ ExchangeRateController → ExchangeRateServiceImpl → MySQL
 *                 ← Result<ExchangeRate> 响应 ← Axios 响应拦截器解析后返回到 .vue 组件
 *
 * 关联文件：
 *   - api/request.js：Axios 实例（baseURL=/api/v1、timeout=10000、请求拦截器注入 token、响应拦截器处理 401/业务错）
 *   - views/AccountPage.vue：账户管理页面（币种选择下拉框 + 余额换算）
 *   - views/DashboardPage.vue：首页概览（多币种余额统一换算为 CNY 展示）
 *   - backend/controller/ExchangeRateController.java：汇率控制器
 *   - backend/entity/ExchangeRate.java：汇率实体（表名 exchange_rates）
 */
import request from './request'                                    // 导入 Axios 实例（→ api/request.js），包含 baseURL + 拦截器配置

/**
 * 获取固定汇率表（基准币种 CNY = 人民币）
 *
 * 请求详情：GET /api/v1/exchange-rate
 * 请求头：Authorization: Bearer <token>（由 request.js 请求拦截器自动注入）
 * 无查询参数
 * 响应体：Result<ExchangeRateData>
 *   字段：
 *     rates: { USD: 7.2, EUR: 7.8, JPY: 0.048, GBP: 9.1, HKD: 0.92, ... }
 *       — 1 单位外币 = ? 元人民币（直接标价法）
 *     ratesInverse: { USD: 0.139, EUR: 0.128, JPY: 20.83, ... }
 *       — 1 元人民币 = ? 单位外币（间接标价法）
 *
 * 业务用途：
 *   - 多币种账户余额统一换算为 CNY 展示
 *   - 账户创建/编辑时提供币种下拉选项
 *
 * 调用方：AccountPage.vue / DashboardPage.vue
 *
 * @returns {Promise<Object>} - Axios Promise，resolve 后返回汇率表 { rates, ratesInverse }
 */
export function getExchangeRates() {                               // 导出 getExchangeRates 函数（→ AccountPage.vue + DashboardPage.vue 调用）
  return request.get('/exchange-rate')                             // GET 请求 → /api/v1/exchange-rate（返回固定汇率表）
}
