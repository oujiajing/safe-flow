package com.pingan.banzu.dto;

import java.util.Map;

public record ThreeCheckRecordListItem(
    String id,
    String moduleKey,
    String recordNo,
    String taskId,
    String rootDispatchRecordId,
    String company,
    String department,
    String team,
    String owner,
    String date,
    String businessDate,
    String imageCheck,
    String imagePreviewUrl,
    String videoCheck,
    String videoPreviewUrl,
    String status,
    String statusLabel,
    boolean overdue,
    Map<String, Object> payload,
    boolean canSubmit,
    boolean canWithdraw,
    boolean canRemind,
    boolean canCreateRectificationOrder,
    String sourceChannel,
    int version) {}
