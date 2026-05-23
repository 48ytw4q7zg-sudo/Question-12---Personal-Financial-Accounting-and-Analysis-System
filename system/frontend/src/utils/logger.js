/**
 * 统一日志工具
 *
 * 功能：
 * - 开发环境输出日志，生产环境静默
 * - 支持 log/warn/error/info/debug 五个级别
 * - 自动添加时间戳和模块标识
 *
 * 使用示例：
 * import { logger } from '@/utils/logger'
 * const log = logger('AccountPage')
 * log.info('加载账户列表')
 * log.error('请求失败', error)
 */

const isDev = import.meta.env.DEV

/**
 * 创建带模块标识的日志实例
 * @param {string} module - 模块名称（如 'AccountPage'）
 * @returns {Object} 日志实例，包含 log/warn/error/info/debug 方法
 */
export function logger(module) {
  const prefix = `[${module}]`

  return {
    log: (...args) => {
      if (isDev) {
        console.log(`${prefix}`, ...args)
      }
    },

    warn: (...args) => {
      if (isDev) {
        console.warn(`${prefix}`, ...args)
      }
    },

    error: (...args) => {
      if (isDev) {
        console.error(`${prefix}`, ...args)
      }
    },

    info: (...args) => {
      if (isDev) {
        console.info(`${prefix}`, ...args)
      }
    },

    debug: (...args) => {
      if (isDev) {
        console.debug(`${prefix}`, ...args)
      }
    }
  }
}
