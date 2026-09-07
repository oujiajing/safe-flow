package com.pingan.banzu.dto;

import java.math.BigDecimal;
import java.util.List;

public record TrainingExamQuestionResponse(
    Long id,
    String questionType,
    String questionText,
    String selectedOption,
    String allOptions,
    String answer,
    BigDecimal score,
    BigDecimal actualScore,
    Integer sortOrder,
    String questionTypeLabel,
    List<TrainingExamQuestionOption> options,
    List<String> correctAnswers,
    String referenceAnswer,
    String answerExplanation,
    String caseMaterial,
    List<TrainingExamQuestionBankChild> children) {

  public TrainingExamQuestionResponse(
      Long id,
      String questionType,
      String questionText,
      String selectedOption,
      String allOptions,
      String answer,
      BigDecimal score,
      BigDecimal actualScore,
      Integer sortOrder) {
    this(
        id,
        questionType,
        questionText,
        selectedOption,
        allOptions,
        answer,
        score,
        actualScore,
        sortOrder,
        questionType,
        List.of(),
        List.of(),
        "",
        "",
        "",
        List.of());
  }
}
