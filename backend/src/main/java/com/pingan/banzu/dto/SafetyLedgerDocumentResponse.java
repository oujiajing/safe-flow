package com.pingan.banzu.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SafetyLedgerDocumentResponse(
    Long id,
    String ledgerKey,
    String name,
    String richText,
    Long companyId,
    String company,
    String department,
    String team,
    String type,
    LocalDate date,
    AttachmentResponse attachment,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
