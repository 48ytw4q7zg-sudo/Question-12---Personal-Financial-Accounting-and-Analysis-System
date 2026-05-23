package com.example.finance.common;

import lombok.Data;

/**
 * 统一响应结果包装类（全栈接口契约 · 与前端 axios 拦截器对齐）
 *
 * <p>所有 Controller 接口统一返回 Result&lt;T&gt;，不直接返回 entity / List / Map。</p>
 * <p>前端 axios 拦截器识别 code 字段：200=成功，401=未登录，其他=业务错误。</p>
 */
@Data
public class Result<T> {

  public static final int CODE_SUCCESS = 200;
  public static final int CODE_DEFAULT_ERROR = 500;

  /** HTTP 状态码或业务状态码（200=成功，401=未登录，4xx/5xx=错误） */
  private Integer code;

  /** 响应消息或错误描述 */
  private String message;

  /** 响应数据体（成功时返回业务数据，错误时为 null） */
  private T data;

  /** 无参构造器（Jackson 反序列化使用） */
  public Result() {}

  /**
   * 全参构造器
   */
  public Result(Integer code, String message, T data) {
    this.code = code;
    this.message = message;
    this.data = data;
  }

  /**
   * 成功响应，返回数据（code=200, message="操作成功"）
   */
  public static <T> Result<T> success(T data) {
    return new Result<>(CODE_SUCCESS, "操作成功", data);
  }

  /**
   * 成功响应，返回数据 + 自定义消息（code=200）
   */
  public static <T> Result<T> success(T data, String message) {
    return new Result<>(CODE_SUCCESS, message, data);
  }

  /**
   * 错误响应，自定义错误码和消息（data=null）
   */
  public static <T> Result<T> error(Integer code, String message) {
    return new Result<>(code, message, null);
  }

  /**
   * 错误响应，默认500错误码（data=null）
   */
  public static <T> Result<T> error(String message) {
    return new Result<>(CODE_DEFAULT_ERROR, message, null);
  }
}
