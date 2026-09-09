package com.pingan.banzu.system.dto;

import java.time.LocalDateTime;

public record SystemCheckItemLibraryResponse(
    Long id,
    String riskType,
    String checkItem,
    String applicableStage,
    String applicableStageLabel,
    String defaultCheckResult,
    String defaultRectificationDescription,
    String defaultFollowUpPlan,
    Boolean requireImage,
    Boolean requireVideo,
    Integer sortOrder,
    String status,
    String statusLabel,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
