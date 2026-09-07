package com.pingan.banzu.dto;

public record TrainingExamQuestionBankQuery(
    Long companyId,
    Long departmentId,
    Long teamId,
    Boolean applicable,
    String keyword,
    String questionType,
    Integer page,
    Integer pageSize) {}
