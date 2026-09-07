package com.pingan.banzu.common;

public record ApiResponse<T>(int code, T data, Object error, String message) {

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(0, data, null, "ok");
  }

  public static ApiResponse<Void> ok() {
    return ok(null);
  }

  public static ApiResponse<Void> error(String message) {
    return new ApiResponse<>(-1, null, message, message);
  }
}
