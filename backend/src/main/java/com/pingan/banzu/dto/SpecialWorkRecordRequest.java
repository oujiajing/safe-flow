package com.pingan.banzu.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record SpecialWorkRecordRequest(
    @NotNull(message = "公司不能为空") Long companyId,
    String project,
    String workType,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") @NotNull(message = "作业申请时间不能为空")
        LocalDateTime applicationTime,
    String workContent,
    String workLocation,
    String riskIdentificationResult,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime implementationStartTime,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime implementationEndTime,
    String safetyDisclosurePerson,
    String guardian,
    String disclosureReceiver,
    String completionAcceptor,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime completionAcceptanceTime,
    String status) {}
