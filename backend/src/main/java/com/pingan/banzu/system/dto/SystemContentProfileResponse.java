package com.pingan.banzu.system.dto;

import com.pingan.banzu.dto.AttachmentResponse;
import java.time.LocalDateTime;

public record SystemContentProfileResponse(
    Long id,
    Long orgId,
    String orgName,
    String orgType,
    String title,
    String subtitle,
    String description,
    AttachmentResponse imageAttachment,
    AttachmentResponse videoAttachment,
    String videoTitle,
    Integer videoSortOrder,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
