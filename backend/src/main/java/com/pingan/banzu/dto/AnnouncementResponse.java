package com.pingan.banzu.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AnnouncementResponse(
    String id,
    int versionNo,
    String previousVersionId,
    String title,
    String content,
    String severity,
    String audienceType,
    List<Long> organizationIds,
    List<Long> roleIds,
    List<Long> userIds,
    String status,
    LocalDateTime effectiveAt,
    LocalDateTime expiresAt,
    LocalDateTime publishedAt,
    LocalDateTime withdrawnAt,
    long deliveryCount,
    long readCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}

