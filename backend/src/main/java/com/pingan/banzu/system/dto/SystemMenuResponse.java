package com.pingan.banzu.system.dto;

import java.util.List;

public record SystemMenuResponse(
    Long id,
    Long parentId,
    String menuCode,
    String title,
    String routePath,
    String component,
    String icon,
    String permissionCode,
    Integer sortOrder,
    Boolean visible,
    String status,
    List<SystemMenuResponse> children) {}
