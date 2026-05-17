package com.example.finance.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

/**
 * 全局异常处理器 — 继承 ResponseEntityExceptionHandler 以正确覆盖 DefaultHandlerExceptionResolver。
 * 所有响应统一使用 Result&lt;T&gt; 格式。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  /**
   * 业务异常处理
   */
  @ExceptionHandler(BusinessException.class)
  public Result<?> handleBusinessException(BusinessException e) {
    log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
    return Result.error(e.getCode(), e.getMessage());
  }

  /**
   * 参数校验异常 — 覆盖父类以使用统一 Result 格式
   */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    String message = e.getBindingResult().getFieldErrors().stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.joining("; "));
    log.warn("参数校验失败: {}", message);
    return new ResponseEntity<>(Result.error(400, message), HttpStatus.BAD_REQUEST);
  }

  /**
   * 缺少请求参数 — 覆盖父类以使用统一 Result 格式
   */
  @Override
  protected ResponseEntity<Object> handleMissingServletRequestParameter(
      MissingServletRequestParameterException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    log.warn("缺少请求参数: {}", e.getMessage());
    return new ResponseEntity<>(Result.error(400, "缺少必要参数: " + e.getParameterName()), HttpStatus.BAD_REQUEST);
  }

  /**
   * 参数类型不匹配
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public Result<?> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
    log.warn("参数类型不匹配: {}", e.getMessage());
    return Result.error(400, "参数格式错误: " + e.getName());
  }

  /**
   * 全局异常兜底
   */
  @ExceptionHandler(Exception.class)
  public Result<?> handleException(Exception e) {
    log.error("服务器内部错误", e);
    return Result.error(500, "服务器内部错误");
  }
}
