package com.pingan.banzu.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SystemAccountRequest(
    @NotBlank(message = "用户名不能为空") String username,
    String password,
    @NotBlank(message = "姓名不能为空") String realName,
    String mobile,
    @NotNull(message = "组织不能为空") Long orgId,
    String status,
    List<Long> roleIds) {}
