package com.pingan.banzu.dto;

import java.util.List;

public record TrainingExamQuestionImportResult(
    int successRows, List<String> errors, List<TrainingExamQuestionResponse> questions) {}
