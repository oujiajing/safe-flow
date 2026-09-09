package com.pingan.banzu.system.dto;

import jakarta.validation.constraints.NotBlank;

public record SystemCheckItemLibraryRequest(
    String riskType,
    @NotBlank(message = "检查项不能为空") String checkItem,
    @NotBlank(message = "适用阶段不能为空") String applicableStage,
    String defaultCheckResult,
    String defaultRectificationDescription,
    String defaultFollowUpPlan,
    Boolean requireImage,
    Boolean requireVideo,
    Integer sortOrder,
    String status) {}
