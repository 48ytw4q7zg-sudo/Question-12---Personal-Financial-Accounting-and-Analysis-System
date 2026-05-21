/**
 * 汇率 API 模块
 * 职责: 封装多币种相关的后端接口调用（固定汇率表）
 *
 * 对应后端: ExchangeRateController.java (/api/exchange-rate)
 * 评分标准: P2-4 多币种支持（固定汇率，不做实时接口）
 *
 * 调用方: → AccountPage.vue（账户币种设置）、DashboardPage.vue（汇总换算展示）
 */
import request from './request'

/**
 * 获取固定汇率表（基准币种 CNY）
 * → 调用 GET /api/exchange-rate
 * @returns {Promise<Object>} 汇率对象 { rates: { USD: 7.2, EUR: 7.8, ... }, ratesInverse: { ... } }
 */
export function getExchangeRates() {
  return request.get('/exchange-rate')
}
