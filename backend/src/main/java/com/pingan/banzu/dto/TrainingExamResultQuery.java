package com.pingan.banzu.dto;

import java.time.LocalDate;

public record TrainingExamResultQuery(
    Long companyId,
    Long departmentId,
    Long taskId,
    LocalDate dateStart,
    LocalDate dateEnd,
    String status,
    Integer page,
    Integer pageSize) {}
