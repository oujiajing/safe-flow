package com.pingan.banzu.dto;

public record ThreeCheckRecordChangeHistoryItem(
    String id,
    String action,
    String fieldKey,
    String fieldLabel,
    String beforeValue,
    String afterValue,
    String valueType,
    String operatorId,
    String operatorName,
    String remark,
    int version,
    String createdAt) {}
