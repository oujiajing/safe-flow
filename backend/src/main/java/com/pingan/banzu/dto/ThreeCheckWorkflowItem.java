package com.pingan.banzu.dto;

import java.time.LocalDateTime;

public record ThreeCheckWorkflowItem(
    String id,
    String action,
    String actionLabel,
    String fromStatus,
    String fromStatusLabel,
    String toStatus,
    String toStatusLabel,
    Long operatorId,
    String operatorName,
    String remark,
    LocalDateTime occurredAt) {}
