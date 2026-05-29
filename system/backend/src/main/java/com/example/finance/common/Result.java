// ============================================================
// §1.4 数据流 节点 ⑬ — 统一响应包装（所有 Controller 返回 {code, message, data}）
// §2.2 逐文件讲解 ⑥/⑩ — Result.java
//
// 这个文件做什么：Result<T> 是前后端约定的唯一通信格式
//                 code=200 成功 / 401 未登录 / 其他业务错误
//                 前端 axios 拦截器只写一段 if-else，所有页面自动受益
//
// 答辩怎么讲（15 秒）："统一返回格式——前端拦截器只写一段 if-else，10 个模块 11 个页面自动走。"
//
// ▶ 逐文件讲解下一个（Ctrl+P）：
//   system/frontend/src/api/transaction.js
//   （§1.4 节点 ③ · §2.2 逐文件讲解 ⑦/⑩ — API 封装层）
// ============================================================
package com.example.finance.common;  // 通用层：统一响应包装类 + 异常处理

import lombok.Data;  // Lombok：自动生成 getter/setter/toString/equals/hashCode

/**
 * 统一响应结果包装类（全栈接口契约 · 与前端 axios 拦截器对齐）
 *
 * <p>所有 Controller 接口统一返回 Result&lt;T&gt;，不直接返回 entity / List / Map。</p>
 * <p>前端 axios 拦截器识别 code 字段：200=成功，401=未登录，其他=业务错误。</p>
 *
 * <p>调用方：所有 Controller 类（controller/ 目录）通过静态工厂方法构建返回值；
 *           前端 api/request.js 拦截器读取 code 字段判定业务状态。</p>
 *
 * @param <T> 响应数据体的类型（泛型，支持任意业务数据类型）
 */
@Data  // Lombok注解：自动生成getter/setter/toString/equals/hashCode
public class Result<T> {  // 泛型响应包装类（T为业务数据类型）

  // ===== 业务状态码常量 =====
  /** 业务状态码：成功（前端 axios 拦截器以此判断请求是否成功） */
  // 引用：前端 api/request.js 拦截器检查 body.code === 200 走业务逻辑
  public static final int CODE_SUCCESS = 200;
  /** 业务状态码：默认服务器错误（便捷方法 result.error(msg) 使用的默认值） */
  public static final int CODE_DEFAULT_ERROR = 500;

  // ===== 字段定义 =====
  /** 业务状态码（200=成功，401=未登录，4xx=客户端错误，5xx=服务器错误 · 非 HTTP 状态码） */
  // 引用：前端 api/request.js 响应拦截器检查此 code 字段判定业务状态
  private Integer code;

  /** 响应消息或错误描述（成功时默认"操作成功"，错误时描述具体原因） */
  private String message;

  /** 响应数据体（成功时返回业务数据，错误时为 null） */
  private T data;

  // ===== 构造器 =====
  /** 无参构造器（Jackson 反序列化使用，JSON → Result 对象转换必须） */
  public Result() {}

  /**
   * 全参构造器（供静态工厂方法内部调用）
   *
   * @param code    业务状态码
   * @param message 响应消息
   * @param data    响应数据体
   */
  public Result(Integer code, String message, T data) {  // 全参构造器
    this.code = code;        // 设置业务状态码
    this.message = message;  // 设置响应消息
    this.data = data;        // 设置响应数据体
  }

  // ===== 静态工厂方法 =====

  /**
   * 成功响应，返回数据（code=200, message="操作成功"）
   *
   * <p>调用方：所有 Controller 返回成功结果时使用。</p>
   *
   * @param data 业务数据
   * @return Result&lt;T&gt;（code=200, message="操作成功"）
   */
  public static <T> Result<T> success(T data) {  // 成功响应（默认消息）
    return new Result<>(CODE_SUCCESS, "操作成功", data);  // 构造code=200+默认消息的Result
  }

  /**
   * 成功响应，返回数据 + 自定义消息（code=200）
   *
   * @param data    业务数据
   * @param message 自定义成功消息
   * @return Result&lt;T&gt;（code=200）
   */
  public static <T> Result<T> success(T data, String message) {  // 成功响应（自定义消息）
    return new Result<>(CODE_SUCCESS, message, data);  // 构造code=200+自定义消息的Result
  }

  /**
   * 错误响应，自定义错误码和消息（data=null）
   *
   * <p>调用方：Service 层抛 BusinessException → GlobalExceptionHandler 统一调用此方法构造错误响应。</p>
   * <p>引用：GlobalExceptionHandler.handleBusinessException() 第 34 行调用 Result.error(code, msg)。</p>
   *
   * @param code    业务错误码（对齐 ErrorCode 枚举，如 1001=用户名已存在）
   * @param message 错误描述（前端 ElMessage.error 展示）
   * @return Result&lt;T&gt;（data=null）
   */
  public static <T> Result<T> error(Integer code, String message) {  // 错误响应（指定错误码）
    return new Result<>(code, message, null);  // 构造Result(data=null)，仅传状态码和消息
  }

  /**
   * 错误响应，默认500错误码（data=null · 便捷方法）
   *
   * <p>当前代码库中通过 GlobalExceptionHandler 统一使用双参数版本 error(code,msg)，
   *    此方法预留给未来快速错误响应场景（如工具类内部直接返回错误）。</p>
   * <p>引用：GlobalExceptionHandler.handleException() 末行兜底处理使用 error(500, msg)。</p>
   *
   * @param message 错误描述
   * @return Result&lt;T&gt;（code=500, data=null）
   */
  public static <T> Result<T> error(String message) {  // 错误响应（默认500错误码·便捷方法）
    return new Result<>(CODE_DEFAULT_ERROR, message, null);  // 构造code=500的Result(data=null)
  }
}
