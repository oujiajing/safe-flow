package com.pingan.banzu.dto;

import java.math.BigDecimal;
import java.util.List;

public record TrainingExamQuestionPayload(
    String questionType,
    String questionText,
    String selectedOption,
    String allOptions,
    String answer,
    BigDecimal score,
    BigDecimal actualScore,
    Integer sortOrder,
    List<TrainingExamQuestionOption> options,
    List<String> correctAnswers,
    String referenceAnswer,
    String answerExplanation,
    String caseMaterial,
    List<TrainingExamQuestionBankChild> children) {

  public TrainingExamQuestionPayload(
      String questionType,
      String questionText,
      String selectedOption,
      String allOptions,
      String answer,
      BigDecimal score,
      BigDecimal actualScore,
      Integer sortOrder) {
    this(
        questionType,
        questionText,
        selectedOption,
        allOptions,
        answer,
        score,
        actualScore,
        sortOrder,
        List.of(),
        List.of(),
        "",
        "",
        "",
        List.of());
  }
}
