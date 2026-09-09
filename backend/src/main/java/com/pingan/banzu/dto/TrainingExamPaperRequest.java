package com.pingan.banzu.dto;

import java.util.List;

public record TrainingExamPaperRequest(
    Long companyId,
    Long departmentId,
    Long teamId,
    String paperName,
    String description,
    List<TrainingExamQuestionPayload> questions) {}
