package com.pingan.banzu.dto;

import java.math.BigDecimal;
import java.util.List;

public record MiniExamQuestionResponse(
    Long id,
    String questionKey,
    String questionType,
    String questionTypeLabel,
    String questionText,
    String allOptions,
    List<TrainingExamQuestionOption> options,
    BigDecimal score,
    Integer sortOrder,
    String caseTitle,
    String caseMaterial,
    String selectedOption,
    String correctAnswer,
    List<String> correctAnswers,
    String referenceAnswer,
    String answerExplanation,
    BigDecimal actualScore) {}
