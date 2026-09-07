package com.pingan.banzu.controller;

import com.pingan.banzu.common.ApiResponse;
import com.pingan.banzu.dto.UserInfoResponse;
import com.pingan.banzu.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

  private final AuthService authService;

  public UserController(AuthService authService) {
    this.authService = authService;
  }

  @GetMapping("/info")
  public ApiResponse<UserInfoResponse> info() {
    return ApiResponse.ok(authService.userInfo());
  }
}
