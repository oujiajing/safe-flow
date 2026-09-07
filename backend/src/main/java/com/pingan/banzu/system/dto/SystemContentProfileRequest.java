package com.pingan.banzu.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SystemContentProfileRequest(
    @NotNull(message = "组织不能为空") Long orgId,
    @NotBlank(message = "标题不能为空") String title,
    String subtitle,
    String description,
    String videoTitle,
    Integer videoSortOrder,
    String status) {}
