package com.pingan.banzu.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
    return ResponseEntity.badRequest().body(ApiResponse.error(exception.getMessage()));
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiResponse<Void>> handleConflictException(ConflictException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(exception.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationException(
      MethodArgumentNotValidException exception) {
    FieldError fieldError = exception.getBindingResult().getFieldError();
    String message = fieldError == null ? "请求参数不合法" : fieldError.getDefaultMessage();
    return ResponseEntity.badRequest().body(ApiResponse.error(message));
  }

  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<ApiResponse<Void>> handleSecurityException(SecurityException exception) {
    HttpStatus status = "未登录或登录已过期".equals(exception.getMessage()) ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
    return ResponseEntity.status(status).body(ApiResponse.error(exception.getMessage()));
  }
}
