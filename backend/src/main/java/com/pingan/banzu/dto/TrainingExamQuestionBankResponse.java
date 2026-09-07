package com.pingan.banzu.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TrainingExamQuestionBankResponse(
    Long id,
    Long companyId,
    String company,
    Long departmentId,
    String department,
    Long teamId,
    String team,
    String questionType,
    String questionTypeLabel,
    String questionText,
    String selectedOption,
    String allOptions,
    String answer,
    BigDecimal score,
    BigDecimal actualScore,
    List<TrainingExamQuestionOption> options,
    List<String> correctAnswers,
    String referenceAnswer,
    String answerExplanation,
    String caseMaterial,
    List<TrainingExamQuestionBankChild> children,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
