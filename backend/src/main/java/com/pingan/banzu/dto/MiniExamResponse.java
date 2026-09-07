package com.pingan.banzu.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MiniExamResponse(
    Long taskId,
    Long resultId,
    String code,
    String company,
    String department,
    String examPersonName,
    String exam,
    LocalDate examDate,
    Integer durationMinutes,
    Integer currentQuestionIndex,
    Integer remainingSeconds,
    Integer answeredCount,
    Integer correctCount,
    Integer incorrectCount,
    Integer elapsedSeconds,
    String status,
    String statusLabel,
    BigDecimal score,
    LocalDateTime submittedAt,
    List<MiniExamQuestionResponse> questions) {}
