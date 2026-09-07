package com.pingan.banzu.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TrainingExamTaskDetailResponse(
    Long id,
    String code,
    Long companyId,
    String company,
    Long departmentId,
    String department,
    Long teamId,
    String team,
    String exam,
    LocalDate examDate,
    Integer durationMinutes,
    String status,
    String statusLabel,
    String remark,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<TrainingExamQuestionResponse> questions,
    List<TrainingExamResultResponse> results) {}
