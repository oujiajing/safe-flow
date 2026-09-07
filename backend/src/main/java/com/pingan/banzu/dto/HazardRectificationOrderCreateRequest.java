package com.pingan.banzu.dto;

import java.time.LocalDate;
import java.util.List;

public record HazardRectificationOrderCreateRequest(
    Long companyId,
    Long departmentId,
    Long teamId,
    LocalDate businessDate,
    List<HazardRectificationOrderCreateItemRequest> items) {}
