package com.pingan.banzu.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TrainingExamPaperResponse(
    Long id,
    Long companyId,
    String company,
    Long departmentId,
    String department,
    Long teamId,
    String team,
    String paperName,
    String description,
    Integer questionCount,
    BigDecimal totalScore,
    List<TrainingExamQuestionPayload> questions,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
