package com.pingan.banzu.dto;

import java.math.BigDecimal;
import java.util.List;

public record TrainingExamQuestionBankRequest(
    Long companyId,
    Long departmentId,
    Long teamId,
    String questionType,
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
    List<TrainingExamQuestionBankChild> children) {}
