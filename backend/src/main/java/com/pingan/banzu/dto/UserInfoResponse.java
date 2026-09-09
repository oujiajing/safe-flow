package com.pingan.banzu.dto;

import java.util.List;

public record UserInfoResponse(
    String userId,
    String username,
    String realName,
    Long orgId,
    String orgPath,
    Long departmentId,
    String departmentName,
    String positionName,
    String avatar,
    List<String> roles,
    String desc,
    String homePath,
    String token) {}
