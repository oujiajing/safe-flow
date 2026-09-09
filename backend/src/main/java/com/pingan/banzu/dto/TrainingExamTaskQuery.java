package com.pingan.banzu.dto;

import java.time.LocalDate;

public record TrainingExamTaskQuery(
    Long companyId,
    Long departmentId,
    LocalDate dateStart,
    LocalDate dateEnd,
    String status,
    Integer page,
    Integer pageSize) {}
