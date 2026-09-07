package com.pingan.banzu.system.dto;

public record SystemTeamCheckTemplateItemResponse(
    Long id,
    Long libraryItemId,
    String riskType,
    String checkItem,
    String defaultCheckResult,
    String defaultRectificationDescription,
    String defaultFollowUpPlan,
    Boolean requireImage,
    Boolean requireVideo,
    Integer sortOrder) {}
