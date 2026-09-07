package com.pingan.banzu.dto;

import java.util.List;
import java.util.Map;

public record ThreeCheckRecordDetailResponse(
    String id,
    String moduleKey,
    String recordNo,
    String taskId,
    String rootDispatchRecordId,
    Long companyId,
    String company,
    Long departmentId,
    String department,
    Long teamId,
    String team,
    Long ownerUserId,
    String owner,
    String businessDate,
    String date,
    String imageCheck,
    String videoCheck,
    String status,
    String statusLabel,
    boolean overdue,
    int reminderCount,
    boolean canSubmit,
    boolean canWithdraw,
    boolean canRemind,
    boolean canCreateRectificationOrder,
    Map<String, Object> payload,
    List<AttachmentResponse> attachments,
    String sourceChannel,
    String sourceRecordId,
    String clientRequestId,
    String clientUpdatedAt,
    String lastSyncedAt,
    int version) {}
