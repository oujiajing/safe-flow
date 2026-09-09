package com.pingan.banzu.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AnnouncementRequest(
    String title,
    String content,
    String severity,
    String audienceType,
    List<Long> organizationIds,
    List<Long> roleIds,
    List<Long> userIds,
    LocalDateTime effectiveAt,
    LocalDateTime expiresAt) {}

