package com.pingan.banzu.dto;

import java.time.LocalDate;
import java.util.List;

public record TrainingExamTaskRequest(
    Long companyId,
    Long departmentId,
    Long teamId,
    String exam,
    LocalDate examDate,
    Integer durationMinutes,
    String status,
    String remark,
    List<Long> examPersonUserIds,
    List<TrainingExamQuestionPayload> questions) {}
