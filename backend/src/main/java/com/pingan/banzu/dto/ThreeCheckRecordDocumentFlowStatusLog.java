package com.pingan.banzu.dto;

import java.util.Map;

public record ThreeCheckRecordDocumentFlowStatusLog(
    String id,
    String action,
    String fromStatus,
    String fromStatusLabel,
    String toStatus,
    String toStatusLabel,
    String operatorId,
    String operatorName,
    String remark,
    Map<String, Object> payload,
    String createdAt) {}
