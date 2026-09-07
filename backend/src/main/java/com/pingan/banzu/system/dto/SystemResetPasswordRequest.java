package com.pingan.banzu.system.dto;

import jakarta.validation.constraints.NotBlank;

public record SystemResetPasswordRequest(@NotBlank(message = "密码不能为空") String password) {}
