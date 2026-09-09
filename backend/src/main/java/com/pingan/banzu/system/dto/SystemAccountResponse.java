package com.pingan.banzu.system.dto;

import java.util.List;

public record SystemAccountResponse(
    Long id,
    String username,
    String realName,
    String mobile,
    Long orgId,
    String orgName,
    String status,
    List<Long> roleIds,
    List<SystemRoleResponse> roles) {}
