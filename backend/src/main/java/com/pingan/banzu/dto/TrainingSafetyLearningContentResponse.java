package com.pingan.banzu.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TrainingSafetyLearningContentResponse(
    Long id,
    Long companyId,
    String company,
    String category,
    String title,
    String content,
    String coverImage,
    AttachmentResponse coverImageAttachment,
    String video,
    AttachmentResponse videoAttachment,
    LocalDate learningDate,
    String durationText,
    String code,
    AttachmentResponse attachment,
    String attachmentText,
    String htmlExtract,
    String draft,
    String status,
    String statusLabel,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
