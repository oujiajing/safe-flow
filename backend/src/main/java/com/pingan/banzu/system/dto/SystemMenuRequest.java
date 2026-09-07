package com.pingan.banzu.system.dto;

import jakarta.validation.constraints.NotBlank;

public record SystemMenuRequest(
    Long parentId,
    @NotBlank(message = "菜单编码不能为空") String menuCode,
    @NotBlank(message = "菜单标题不能为空") String title,
    @NotBlank(message = "路由不能为空") String routePath,
    @NotBlank(message = "组件不能为空") String component,
    String icon,
    @NotBlank(message = "权限码不能为空") String permissionCode,
    Integer sortOrder,
    Boolean visible,
    String status) {}
