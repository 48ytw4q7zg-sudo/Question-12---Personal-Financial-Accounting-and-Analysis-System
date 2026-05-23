/**
 * JWT 工具函数
 * 职责：提供 JWT token 的解码和过期检查功能
 *
 * 设计说明：
 *   - 教学项目使用手动 Base64 解码避免引入 jwt-decode 依赖
 *   - 生产环境建议使用 jwt-decode 库（npm install jwt-decode），更安全可靠
 *   - 解码逻辑集中在此处，消除 router/index.js 和 stores/user.js 的重复代码
 *
 * 调用方：
 *   - router/index.js → decodeJwtPayload()（token 过期预检）
 *   - stores/user.js → decodeJwtPayload()（页面刷新时恢复用户信息）
 */

/**
 * 解码 JWT payload（手动 Base64URL 解码）
 * @param {String} token - JWT token 字符串
 * @returns {Object|null} 解码后的 payload 对象，失败返回 null
 */
export function decodeJwtPayload(token) {  // 解码 JWT payload
  if (!token) return null  // token 为空直接返回 null
  try {
    // 提取 payload 部分（JWT 格式：header.payload.signature）
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')  // Base64URL → Base64
    // Base64 解码（支持 UTF-8 字符）
    const decoded = decodeURIComponent(atob(base64).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join(''))
    return JSON.parse(decoded)  // 解析 JSON 返回 payload 对象
  } catch (e) {  // 解码失败
    if (import.meta.env.DEV) console.warn('JWT payload 解码失败:', e)  // 开发环境日志
    return null  // 解码失败返回 null
  }
}

/**
 * 检查 JWT token 是否过期
 * @param {String} token - JWT token 字符串
 * @returns {Boolean} true=已过期或无效，false=未过期
 */
export function isTokenExpired(token) {  // 检查 token 是否过期
  const payload = decodeJwtPayload(token)  // 解码 payload
  if (!payload) return true  // 解码失败视为过期
  // exp 类型安全：显式转为数字（防止字符串/其他类型导致误判）
  const exp = Number(payload.exp)  // 显式转换为数字（payload.exp 可能是字符串或其他类型）
  return !isNaN(exp) && exp > 0 && exp * 1000 < Date.now()  // exp 为有效正数字且小于当前时间则过期
}
