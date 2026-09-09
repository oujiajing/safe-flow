package com.pingan.banzu.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public record ThreeCheckRecordUpsertRequest(
    Long taskId,
    Long rootDispatchRecordId,
    @NotNull(message = "公司不能为空") Long companyId,
    @NotNull(message = "部门不能为空") Long departmentId,
    Long teamId,
    Long ownerUserId,
    @NotNull(message = "业务日期不能为空") LocalDate businessDate,
    String status,
    Map<String, Object> payload,
    String sourceChannel,
    String sourceRecordId,
    String clientRequestId,
    LocalDateTime clientUpdatedAt,
    Integer version) {}
