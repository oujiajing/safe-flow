package com.pingan.banzu.dto;

import java.util.List;

public record TrainingExamPdfConfirmResult(
    String target,
    int successRows,
    List<String> errors,
    List<TrainingExamQuestionBankResponse> questions,
    List<TrainingExamQuestionResponse> paperQuestions) {}
