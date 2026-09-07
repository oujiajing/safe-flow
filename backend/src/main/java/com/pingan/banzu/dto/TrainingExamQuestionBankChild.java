package com.pingan.banzu.dto;

import java.math.BigDecimal;
import java.util.List;

public record TrainingExamQuestionBankChild(
    String questionType,
    String questionTypeLabel,
    String questionText,
    List<TrainingExamQuestionOption> options,
    List<String> correctAnswers,
    String referenceAnswer,
    String answerExplanation,
    BigDecimal score) {}
