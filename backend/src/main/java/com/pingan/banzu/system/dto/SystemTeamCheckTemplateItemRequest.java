package com.pingan.banzu.system.dto;

import jakarta.validation.constraints.NotBlank;

public record SystemTeamCheckTemplateItemRequest(
    Long libraryItemId,
    String riskType,
    @NotBlank(message = "检查项不能为空") String checkItem,
    String defaultCheckResult,
    String defaultRectificationDescription,
    String defaultFollowUpPlan,
    Boolean requireImage,
    Boolean requireVideo,
    Integer sortOrder) {}
