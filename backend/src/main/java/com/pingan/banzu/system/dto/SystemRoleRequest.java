package com.pingan.banzu.system.dto;

import jakarta.validation.constraints.NotBlank;

public record SystemRoleRequest(
    @NotBlank(message = "角色编码不能为空") String roleCode,
    @NotBlank(message = "角色名称不能为空") String roleName,
    @NotBlank(message = "数据范围不能为空") String dataScope) {}
