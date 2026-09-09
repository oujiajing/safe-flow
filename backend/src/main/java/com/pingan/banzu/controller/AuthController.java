package com.pingan.banzu.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.dto.LoginRequest;
import com.pingan.banzu.dto.LoginResponse;
import com.pingan.banzu.service.AuthService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ApiResponse.ok(authService.login(request));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout() {
    authService.logout();
    return ApiResponse.ok();
  }

  @PostMapping("/refresh")
  public ApiResponse<String> refresh() {
    return ApiResponse.ok("");
  }

  @GetMapping("/codes")
  public ApiResponse<List<String>> codes() {
    return ApiResponse.ok(authService.accessCodes());
  }
}
