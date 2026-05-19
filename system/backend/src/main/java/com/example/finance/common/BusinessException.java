package com.example.finance.common;

import lombok.Getter;

/**
 * 业务异常类（Service 层抛出 → GlobalExceptionHandler 统一处理 → Result.error(code, msg) 返回前端）
 *
 * 使用方式：
 *   throw new BusinessException(1001, "用户名已存在");
 *   throw new BusinessException(3005, "转出账户余额不足");
 *
 * 错误码规范（对齐 API_DESIGN.md §4）：
 *   1001-1099 = 用户模块 / 2001-2099 = 账户模块 / 3001-3099 = 交易模块
 *   4001-4099 = 预算模块 / 5001-5099 = 周期账单模块
 */
@Getter
public class BusinessException extends RuntimeException {

  /** 业务错误码（如 1001=用户名已存在） */
  private final Integer code;

  /**
   * @param code    业务错误码（如 1001）
   * @param message 错误描述（前端 ElMessage.error 展示）
   */
  public BusinessException(Integer code, String message) {
    super(message);
    this.code = code;
  }
}
