/**
 * JWT 工具函数（教学项目 · 手动 Base64URL 解码）
 *
 * 职责：
 *   - 解码 JWT token 的 payload 部分（无需服务端介入）
 *   - 检查 token 是否过期（exp 字段校验）
 *   - 供 router 守卫和 Pinia store 在页面加载/导航时预检 token 有效性
 *
 * 设计说明（为什么不用 jwt-decode 库）：
 *   - 教学项目减少 npm 依赖，手动实现 Base64URL→JSON 解码流程
 *   - 生产环境建议使用 jwt-decode（npm install jwt-decode），由专业库处理边缘情况
 *   - 解码逻辑从 router/index.js 和 stores/user.js 抽取到此文件，消除重复代码
 *
 * 调用方与流程：
 *   router/index.js → isTokenExpired()（beforeEach 守卫预检 token，过期则清除并跳登录）
 *   stores/user.js  → decodeJwtPayload()（页面刷新时从 localStorage 恢复用户信息）
 */
/**
 * 解码 JWT payload（手动 Base64URL → UTF-8 JSON）
 *
 * JWT 格式为三段点分隔: header.payload.signature（每段都是 Base64URL 编码）
 * 本函数取第二段（payload），执行 Base64URL→Base64 转换 → atob 解码 → UTF-8 字节处理 → JSON 解析
 *
 * @param {String} token - JWT token 字符串（如 "eyJhbGci...header...payload...sig"）
 * @returns {Object|null} 解码后的 payload 对象（含 userId/role/exp 等字段），格式错误或解码失败返回 null
 */
export function decodeJwtPayload(token) {                  // 解码 JWT token 的 payload 部分
  if (!token) return null                                   // token 为空/undefined 直接返回 null
  try {                                                     // 用 try-catch 包裹解码全流程，捕获 Base64 格式错误 / JSON 解析异常
    // 第一步：取 JWT 的第二段（payload），格式为 header.payload.signature 用 '.' 分隔
    const base64 = token.split('.')[1]                      // 按 '.' 分割 JWT，取索引1（payload 段）
      .replace(/-/g, '+')                                   // Base64URL → Base64: '-' 替换为 '+'
      .replace(/_/g, '/')                                   // Base64URL → Base64: '_' 替换为 '/'
    // 第二步：Base64 → UTF-8 字符串（处理多字节 Unicode 字符如中文用户名）
    const decoded = decodeURIComponent(                     // 将百分号编码的 UTF-8 字节序列还原为原始字符串
      atob(base64)                                          // atob() 解码 Base64 为字节字符串（浏览器内置 API）
        .split('')                                          // 将字符串拆分为单个字符数组
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)) // 每字符转为百分号编码（%XX 格式）
        .join('')                                           // 合并回完整字符串
    )                                                       // 关闭 decodeURIComponent 调用
    return JSON.parse(decoded)                              // 解析 JSON 字符串为 JavaScript 对象并返回
  } catch (e) {                                             // 捕获所有解码异常（Base64 格式错误、JSON 解析失败等）
    if (import.meta.env.DEV) console.warn('JWT payload 解码失败:', e) // 开发环境输出警告（生产环境静默，安全考虑不泄露 token 内容）
    return null                                             // 解码失败返回 null，调用方据此判断 token 无效
  }                                                         // catch 块结束
}                                                           // decodeJwtPayload 函数结束 · 调用方: router/index.js 路由守卫 / stores/user.js 恢复用户信息

/**
 * 检查 JWT token 是否已过期（基于 exp 声明）
 *
 * JWT 标准中 exp 字段为 Unix 时间戳（秒），需要 ×1000 转换为毫秒后与 Date.now() 比较。
 * 已过期或无效的 token 需要前端主动清除，防止向后端发送必然失败的请求。
 *
 * @param {String} token - JWT token 字符串
 * @returns {Boolean} true=已过期或无效（应清除并跳登录），false=未过期（可继续使用）
 */
export function isTokenExpired(token) {                     // 检查 JWT token 是否过期
  const payload = decodeJwtPayload(token)                   // 先解码 payload，失败则直接判定为"过期"
  if (!payload) return true                                 // 解码失败（token 格式错误/篡改）→ 视为过期
  // exp 类型安全：JWT payload.exp 可能是字符串（如 "1759123456"）而非数字
  const exp = Number(payload.exp)                           // 显式转换为数字（防止字符串比较导致误判）
  // 三重校验：exp 不是 NaN（非数字）→ exp > 0（合法时间戳）→ 当前时间 > exp*1000（已过期）
  return !isNaN(exp) && exp > 0 && exp * 1000 < Date.now()  // exp 合法且已过当前时间 → 返回 true（已过期）
}