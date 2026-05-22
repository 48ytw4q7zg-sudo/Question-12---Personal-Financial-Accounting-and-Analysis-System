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
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import jakarta.validation.ConstraintViolationException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器 — 继承 ResponseEntityExceptionHandler 以正确覆盖 DefaultHandlerExceptionResolver。
 *
 * <p><b>body-code-first 约定</b>：所有响应统一使用 HTTP 200 + Result&lt;T&gt; body 格式。</p>
 * <p>前端 axios 拦截器检查 res.data.code 而非 HTTP status code，因此：
 *   业务状态码(200/400/401/403/404/500 等)放在 JSON body 的 code 字段，
 *   HTTP 状态码始终为 200，以确保前端拦截器能正确解析中文错误消息。</p>
 * <p>此约定与 LoginInterceptor(body code 401 + HTTP 200) 保持一致。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  // HTTP 协议层状态码常量（与业务号段 1001-6004 分属不同语义空间）
  private static final int HTTP_BAD_REQUEST = 400;
  private static final int HTTP_FORBIDDEN = 403;
  private static final int HTTP_NOT_FOUND = 404;
  private static final int HTTP_METHOD_NOT_ALLOWED = 405;
  private static final int HTTP_UNSUPPORTED_MEDIA_TYPE = 415;
  private static final int HTTP_INTERNAL_ERROR = 500;

  /**
   * 业务异常处理 — HTTP 200 + Result.error(code, msg)
   */
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<Result<?>> handleBusinessException(BusinessException e) {
    log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
    return new ResponseEntity<>(Result.error(e.getCode(), e.getMessage()), HttpStatus.OK);
  }

  /**
   * 参数校验异常 — 覆盖父类以使用统一 Result 格式
   * 使用 HTTP 200 + Result.error(400, msg) 保持与前端 axios 拦截器一致
   * （前端拦截器检查 body.code 而非 HTTP status，HTTP 400 会丢失中文消息）
   */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    String message = e.getBindingResult().getFieldErrors().stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.joining("; "));
    log.warn("参数校验失败: {}", message);
    return new ResponseEntity<>(Result.error(HTTP_BAD_REQUEST, message), HttpStatus.OK);
  }

  /**
   * 缺少请求参数 — 覆盖父类以使用统一 Result 格式
   * 使用 HTTP 200 + Result.error(400, msg) 保持与前端 axios 拦截器一致
   */
  @Override
  protected ResponseEntity<Object> handleMissingServletRequestParameter(
      MissingServletRequestParameterException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    log.warn("缺少请求参数: {}", e.getMessage());
    return new ResponseEntity<>(Result.error(HTTP_BAD_REQUEST, "缺少必要参数: " + e.getParameterName()), HttpStatus.OK);
  }

  /**
   * 参数类型不匹配 — HTTP 200 + Result.error(400, msg)
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<Result<?>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
    log.warn("参数类型不匹配: {}", e.getMessage());
    return new ResponseEntity<>(Result.error(HTTP_BAD_REQUEST, "参数格式错误: " + e.getName()), HttpStatus.OK);
  }

  /**
   * 文件上传大小超限异常 — 覆盖父类以使用统一 Result 格式
   * 使用 HTTP 200 + Result.error(400, msg) 保持与前端 axios 拦截器一致
   */
  @Override
  protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
      MaxUploadSizeExceededException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    log.warn("文件大小超限: {}", e.getMessage());
    return new ResponseEntity<>(Result.error(HTTP_BAD_REQUEST, "文件大小超过限制（最大 5MB）"), HttpStatus.OK);
  }

  /**
   * 不支持的媒体类型 — 覆盖父类以使用统一 Result ��式
   * 使用 HTTP 200 + Result.error(415, msg) 保持与前端 axios 拦截器一致
   */
  @Override
  protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    log.warn("不支持的媒体类型: {}", e.getContentType());
    return new ResponseEntity<>(Result.error(HTTP_UNSUPPORTED_MEDIA_TYPE, "不支持的媒体类型"), HttpStatus.OK);
  }

  /**
   * 请求方法不支持 — 覆盖父类以使用统一 Result 格式
   * 使用 HTTP 200 + Result.error(405, msg) 保持与前端 axios 拦截器一致
   */
  @Override
  protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
      HttpRequestMethodNotSupportedException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    log.warn("请求方法不支持: {}", e.getMethod());
    return new ResponseEntity<>(Result.error(HTTP_METHOD_NOT_ALLOWED, "请求方法不支持: " + e.getMethod()), HttpStatus.OK);
  }

  /**
   * @Validated 校验失败（@RequestParam 路径参数校验触发） — HTTP 200 + Result.error(400, msg)
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Result<?>> handleConstraintViolation(ConstraintViolationException e) {
    String message = e.getConstraintViolations().stream()
        .map(v -> v.getMessage())
        .collect(Collectors.joining("; "));
    log.warn("参数校验失败(ConstraintViolation): {}", message);
    return new ResponseEntity<>(Result.error(HTTP_BAD_REQUEST, message), HttpStatus.OK);
  }

  /**
   * 请求体格式错误（JSON 解析失败） — HTTP 200 + Result.error(400, msg)
   */
  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    log.warn("请求体格式错误: {}", e.getMessage());
    return new ResponseEntity<>(Result.error(HTTP_BAD_REQUEST, "请求体格式错误"), HttpStatus.OK);
  }

  /**
   * 数据库约束违反（唯一键/外键冲突） — HTTP 200 + Result.error(400, msg)
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Result<?>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
    log.warn("数据库约束违反: {}", e.getMessage());
    return new ResponseEntity<>(Result.error(HTTP_BAD_REQUEST, "数据操作冲突，请检查输入是否重复"), HttpStatus.OK);
  }

  /**
   * 权限拒绝 — ServletException 中权限相关子类
   * 项目权限校验主要由 LoginInterceptor + AdminServiceImpl 抛 BusinessException(6004) 实现，
   * 此处理器覆盖框架层可能抛出的 403 异常场景 — HTTP 200 + Result.error(403, msg)
   * 注: 项目仅引入 spring-security-crypto 子模块（不含 AccessDeniedException），故用异常类名判断替代
   */
  @ExceptionHandler(jakarta.servlet.ServletException.class)
  public ResponseEntity<Result<?>> handleServletException(jakarta.servlet.ServletException e) {
    // 基于异常类名判断权限（比字符串消息匹配更稳定，不依赖 locale）
    String className = e.getClass().getSimpleName();
    if (className.contains("AccessDenied") || className.contains("Forbidden")) {
      log.warn("权限异常({}): {}", className, e.getMessage());
      return new ResponseEntity<>(Result.error(HTTP_FORBIDDEN, "无权限访问"), HttpStatus.OK);
    }
    log.warn("Servlet 请求处理异常: {}", e.getMessage());
    return new ResponseEntity<>(Result.error(HTTP_INTERNAL_ERROR, "请求处理异常"), HttpStatus.OK);
  }

  /**
   * 资源不存在 — Spring 6.x 对未映射 URL 返回 NoResourceFoundException（替代旧版 NoHandlerFoundException）
   * 覆盖父类以使用统一 Result 格式 — HTTP 200 + Result.error(404, msg)
   */
  @Override
  protected ResponseEntity<Object> handleNoResourceFoundException(
      NoResourceFoundException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    log.warn("资源不存在: {}", e.getResourcePath());
    return new ResponseEntity<>(Result.error(HTTP_NOT_FOUND, "资源不存在: " + e.getResourcePath()), HttpStatus.OK);
  }

  /**
   * 空指针异常 — 通常指示代码缺陷而非基础设施问题，单独处理便于监控区分
   */
  @ExceptionHandler(NullPointerException.class)
  public ResponseEntity<Result<?>> handleNullPointerException(NullPointerException e) {
    log.error("空指针异常(代码缺陷)", e);
    return new ResponseEntity<>(Result.error(HTTP_INTERNAL_ERROR, "内部数据异常"), HttpStatus.OK);
  }

  /**
   * 全局异常兜底 — HTTP 200 + Result.error(500, msg)
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Result<?>> handleException(Exception e) {
    log.error("服务器内部错误", e);
    return new ResponseEntity<>(Result.error(HTTP_INTERNAL_ERROR, "服务器内部错误"), HttpStatus.OK);
  }
}