package com.example.finance.common;

import lombok.Data;

/**
 * 统一响应结果包装类
 */
@Data
public class Result<T> {

  private Integer code;
  private String message;
  private T data;

  public Result() {}

  public Result(Integer code, String message, T data) {
    this.code = code;
    this.message = message;
    this.data = data;
  }

  /**
   * 成功响应，返回数据
   */
  public static <T> Result<T> success(T data) {
    return new Result<>(200, "操作成功", data);
  }

  /**
   * 成功响应，返回数据 + 自定义消息
   */
  public static <T> Result<T> success(T data, String message) {
    return new Result<>(200, message, data);
  }

  /**
   * 错误响应，自定义错误码和消息
   */
  public static <T> Result<T> error(Integer code, String message) {
    return new Result<>(code, message, null);
  }

  /**
   * 错误响应，默认500错误码
   */
  public static <T> Result<T> error(String message) {
    return new Result<>(500, message, null);
  }
}
