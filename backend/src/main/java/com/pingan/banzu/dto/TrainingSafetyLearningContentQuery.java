package com.pingan.banzu.dto;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record TrainingSafetyLearningContentQuery(
    Long companyId,
    String category,
    String keyword,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateStart,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEnd,
    String status,
    Integer page,
    Integer pageSize) {}
