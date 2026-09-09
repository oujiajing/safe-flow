package com.pingan.banzu.dto;

import java.time.LocalDateTime;

public record SpecialWorkRecordResponse(
    Long id,
    Long companyId,
    String company,
    String project,
    String workType,
    LocalDateTime applicationTime,
    AttachmentResponse image,
    String workContent,
    String workLocation,
    String riskIdentificationResult,
    LocalDateTime implementationStartTime,
    LocalDateTime implementationEndTime,
    String safetyDisclosurePerson,
    String guardian,
    String disclosureReceiver,
    String completionAcceptor,
    LocalDateTime completionAcceptanceTime,
    String status,
    String statusLabel,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
