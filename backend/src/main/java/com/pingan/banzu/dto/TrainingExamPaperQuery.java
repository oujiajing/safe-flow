package com.pingan.banzu.dto;

public record TrainingExamPaperQuery(
    Long companyId,
    Long departmentId,
    Long teamId,
    String keyword,
    Integer page,
    Integer pageSize) {}
