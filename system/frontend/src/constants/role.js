/**
 * 角色常量定义
 * 统一管理角色值和标签，避免魔法数字和硬编码中文字符串散布各处
 */

/** 角色值常量（对齐数据库 user.role 字段：0=普通用户, 1=管理员） */
export const ROLE_NORMAL = 0   // 普通用户角色值
export const ROLE_ADMIN = 1    // 管理员角色值

/** 角色标签映射 */
export const ROLE_LABELS = {           // 角色标签映射表
  [ROLE_NORMAL]: '普通用户',            // 普通用户标签
  [ROLE_ADMIN]: '管理员'               // 管理员标签
}