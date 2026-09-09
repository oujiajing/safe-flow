package com.pingan.banzu.dto;

import java.util.List;

public record TrainingExamQuestionBankImportResult(
    int successRows, List<String> errors, List<TrainingExamQuestionBankResponse> questions) {}
