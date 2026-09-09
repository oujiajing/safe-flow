package com.pingan.banzu.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TrainingExamResultDetailResponse(
    Long id,
    String code,
    Long taskId,
    Long companyId,
    String company,
    Long departmentId,
    String department,
    Long examPersonUserId,
    String examPersonName,
    String exam,
    BigDecimal score,
    LocalDate examDate,
    String status,
    String statusLabel,
    String remark,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<TrainingExamQuestionResponse> questions) {}
