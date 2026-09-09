package com.pingan.banzu.dto;

import java.math.BigDecimal;
import java.util.List;

public record TrainingExamPdfQuestionDraft(
    String sourceLabel,
    String questionType,
    String questionTypeLabel,
    String questionText,
    List<TrainingExamQuestionOption> options,
    List<String> correctAnswers,
    String referenceAnswer,
    String answerExplanation,
    String caseMaterial,
    List<TrainingExamQuestionBankChild> children,
    BigDecimal score) {}
