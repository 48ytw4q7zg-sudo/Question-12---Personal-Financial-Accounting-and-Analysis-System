/**
 * 统一日志工具（开发/生产环境自适应）
 *
 * 功能：
 *   - 开发环境（import.meta.env.DEV）输出完整日志，生产环境静默不输出
 *   - 支持 log / warn / error / info / debug 五个日志级别
 *   - 自动在每条日志前添加 [模块名] 前缀，方便在浏览器控制台按模块过滤
 *   - 替代零散的 console.log/warn/error 调用，确保生产环境零日志泄漏
 *
 * 设计说明：
 *   - 基于 import.meta.env.DEV（Vite 构建时静态替换）做环境判断，生产构建后 isDev 直接被替换为 false
 *   - 生产环境下所有日志方法变成空操作，Vite tree-shaking 会移除死代码（零运行时开销）
 *   - 不依赖任何第三方日志库（如 loglevel/winston），保持零额外依赖
 *
 * 使用示例：
 *   import { logger } from '@/utils/logger'
 *   const log = logger('AccountPage')    // 创建带模块前缀的日志实例
 *   log.info('加载账户列表')             // 开发环境输出：[AccountPage] 加载账户列表
 *   log.error('请求失败', error)         // 开发环境输出：[AccountPage] 请求失败 + 错误详情
 */
const isDev = import.meta.env.DEV                           // Vite 构建时常量：开发=true，生产=false（tree-shaking 优化）

/**
 * 创建带模块标识的日志实例（工厂函数）
 *
 * 返回的对象包含 5 个方法（log/warn/error/info/debug），每个方法内部通过闭包持有 module 名称，
 * 自动在日志前添加 [模块名] 前缀。
 *
 * @param {string} module - 模块名称（通常为 Vue 页面组件名，如 'DashboardPage'、'TransactionListPage'）
 * @returns {Object} 日志实例对象，包含 log/warn/error/info/debug 五个方法
 */
export function logger(module) {                            // 创建日志实例（闭包持有 module 名称）
  const prefix = `[${module}]`                              // 日志前缀格式：[模块名]，用于控制台过滤

  return {
    /**
     * 一般日志输出（console.log 级别）
     * 用途：状态变化、数据加载、用户操作等正常流程记录
     * @param {...any} args - 任意参数，与 console.log 参数格式一致
     */
    log: (...args) => {                                     // 一般日志（如数据加载、状态更新）
      if (isDev) {                                          // 仅开发环境输出，生产环境跳过（Vite 静态替换）
        console.log(`${prefix}`, ...args)                   // 输出格式：[模块名] <参数1> <参数2> ...
      }
    },

    /**
     * 警告日志输出（console.warn 级别）
     * 用途：非致命错误、数据异常但仍可继续运行的场景
     * @param {...any} args - 任意参数，与 console.warn 参数格式一致
     */
    warn: (...args) => {                                    // 警告日志（如数据格式异常、降级处理）
      if (isDev) {
        console.warn(`${prefix}`, ...args)                  // 输出格式：[模块名] ⚠ <参数1> ...
      }
    },

    /**
     * 错误日志输出（console.error 级别）
     * 用途：API 请求失败、数据处理异常等需要关注的错误
     * @param {...any} args - 任意参数，与 console.error 参数格式一致
     */
    error: (...args) => {                                   // 错误日志（如 API 失败、异常捕获）
      if (isDev) {
        console.error(`${prefix}`, ...args)                 // 输出格式：[模块名] ❌ <参数1> ...
      }
    },

    /**
     * 信息日志输出（console.info 级别）
     * 用途：关键业务流程节点记录（如"用户登录成功"、"账户创建完成"）
     * @param {...any} args - 任意参数，与 console.info 参数格式一致
     */
    info: (...args) => {                                    // 信息日志（如关键业务流程节点）
      if (isDev) {
        console.info(`${prefix}`, ...args)                  // 输出格式：[模块名] ℹ <参数1> ...
      }
    },

    /**
     * 调试日志输出（console.debug 级别，折叠显示）
     * 用途：开发调试时的详细信息（如原始 API 响应、中间计算值）
     * @param {...any} args - 任意参数，与 console.debug 参数格式一致
     */
    debug: (...args) => {                                   // 调试日志（如原始 API 响应、中间计算值）
      if (isDev) {
        console.debug(`${prefix}`, ...args)                 // 输出格式：[模块名] 🔍 <参数1> ...
      }
    }
  }
}