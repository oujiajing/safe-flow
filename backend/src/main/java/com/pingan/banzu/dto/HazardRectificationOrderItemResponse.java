package com.pingan.banzu.dto;

import java.util.Map;

public record HazardRectificationOrderItemResponse(
    String id,
    String sourceLineId,
    int sourceLineIndex,
    String libraryItemId,
    String riskType,
    String checkItem,
    String hazardDescription,
    String aiEnabled,
    String beforePhoto,
    String beforeVideo,
    String defaultFollowUpPlan,
    String rectificationStatus,
    String rectificationStatusLabel,
    String closedAt,
    int sortOrder,
    Map<String, Object> sourceSnapshot) {}
