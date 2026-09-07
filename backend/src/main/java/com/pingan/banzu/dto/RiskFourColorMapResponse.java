package com.pingan.banzu.dto;

import java.time.LocalDateTime;

public record RiskFourColorMapResponse(
    Long id,
    String name,
    Long companyId,
    String company,
    AttachmentResponse backgroundAttachment,
    String remark,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
