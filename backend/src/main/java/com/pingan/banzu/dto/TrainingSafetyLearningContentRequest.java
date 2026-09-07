package com.pingan.banzu.dto;

import java.time.LocalDate;

public record TrainingSafetyLearningContentRequest(
    Long companyId,
    String category,
    String title,
    String content,
    String coverImage,
    String video,
    LocalDate learningDate,
    String durationText,
    String code,
    String attachmentText,
    String htmlExtract,
    String draft,
    String status) {}
