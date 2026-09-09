package com.pingan.banzu.dto;

import java.util.List;

public record TrainingExamPdfConfirmRequest(
    String target,
    Long companyId,
    Long departmentId,
    Long teamId,
    List<TrainingExamPdfQuestionDraft> questions) {}
