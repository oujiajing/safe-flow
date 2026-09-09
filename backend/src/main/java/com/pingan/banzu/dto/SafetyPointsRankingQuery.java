package com.pingan.banzu.dto;

import java.time.LocalDate;

public record SafetyPointsRankingQuery(
    Long companyId,
    Long departmentId,
    Long teamId,
    LocalDate dateStart,
    LocalDate dateEnd,
    String keyword,
    Integer page,
    Integer pageSize,
    SafetyPointsRankBy rankBy) {}
