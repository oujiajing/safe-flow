package com.pingan.banzu.dto;

import java.time.LocalDate;

public record HazardRectificationOrderQuery(
    String sourceType,
    String sourceModuleKey,
    Long rootDispatchRecordId,
    Long sourceRecordId,
    String status,
    Long companyId,
    Long departmentId,
    Long teamId,
    Long responsibleUserId,
    LocalDate dateStart,
    LocalDate dateEnd,
    Integer page,
    Integer pageSize) {}
