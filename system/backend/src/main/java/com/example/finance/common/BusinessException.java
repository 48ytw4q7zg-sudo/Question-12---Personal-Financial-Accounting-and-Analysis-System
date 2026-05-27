package com.example.finance.common;  // 通用层：业务异常类

import lombok.Getter;  // Lombok：自动生成 getter 方法

/**
 * 业务异常类（Service 层抛出 → GlobalExceptionHandler 统一处理 → Result.error(code, msg) 返回前端）
 *
 * <p>设计意图：与 RuntimeException 区分开，让 GlobalExceptionHandler 能精准捕获业务异常
 *    并提取 code + message 字段，构造 Result.error(code, msg) 返回前端。</p>
 *
 * <p>使用方式：
 *   throw new BusinessException(1001, "用户名已存在");
 *   throw new BusinessException(ErrorCode.PASSWORD_ERROR.getCode(), ErrorCode.PASSWORD_ERROR.getMsg());</p>
 *
 * <p>错误码规范（对齐 API_DESIGN.md §4 · ErrorCode 枚举号段分配）：
 *   1001-1099 = 用户模块 / 2001-2099 = 账户模块 / 3001-3099 = 交易模块
 *   4001-4099 = 预算模块 / 5001-5099 = 周期账单模块 / 6001-6099 = 管理员模块 / 7001-7099 = 参数校验模块</p>
 *
 * <p>调用方：所有 ServiceImpl 类（service/impl/ 目录）在业务规则校验失败时抛出；
 *           被 GlobalExceptionHandler.handleBusinessException() 捕获并统一处理。</p>
 *
 * <p>引用：common/ErrorCode.java — 提供标准化的错误码 + 错误消息；
 *          common/GlobalExceptionHandler.java — 统一捕获并转换为 Result.error 响应。</p>
 */
@Getter  // Lombok注解：自动生成 getCode() 和 getMessage()（继承自 Throwable）
public class BusinessException extends RuntimeException {  // 继承RuntimeException（非受检异常，无需显式throws声明）

  /** 业务错误码（如 1001=用户名已存在 · 对齐 ErrorCode 枚举号段分配） */
  private final Integer code;  // final修饰：错误码不可变（异常创建后不可修改）

  /**
   * 构造器：传入错误码 + 错误消息
   *
   * @param code    业务错误码（如 1001=用户名已存在 · 引用 common/ErrorCode.java）
   * @param message 错误描述（前端 ElMessage.error 展示给用户）
   */
  public BusinessException(Integer code, String message) {  // 全参构造器
    super(message);  // 调用 RuntimeException 父类构造器，将消息存入 Throwable.message
    this.code = code;  // 存储业务错误码（供 GlobalExceptionHandler 提取）
  }
}
