package com.pingan.banzu.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TrainingExamTaskResponse(
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
    LocalDateTime updatedAt) {}
