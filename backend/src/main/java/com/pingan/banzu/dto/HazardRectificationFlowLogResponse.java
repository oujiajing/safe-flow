package com.pingan.banzu.dto;

import java.util.Map;

public record HazardRectificationFlowLogResponse(
    String id,
    String fromStatus,
    String fromStatusLabel,
    String toStatus,
    String toStatusLabel,
    String action,
    String actionLabel,
    String operatorId,
    String operatorName,
    String remark,
    Map<String, Object> payload,
    String createdAt) {}
