package com.pingan.banzu.dto;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record ThreeCheckRecordQuery(
    String company,
    Long companyId,
    String department,
    Long departmentId,
    String team,
    Long teamId,
    Long rootDispatchRecordId,
    String status,
    String pointsReason,
    Long organizationId,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateStart,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEnd,
    String sourceChannel,
    Boolean overdue,
    Integer page,
    Integer pageSize) {}
