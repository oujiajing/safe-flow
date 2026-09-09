package com.pingan.banzu.system.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SystemTeamCheckTemplateResponse(
    Long id,
    String name,
    Long companyOrgId,
    String companyName,
    Long departmentOrgId,
    String departmentName,
    Long teamOrgId,
    String teamName,
    String scope,
    String scopeName,
    String inspectionStage,
    String inspectionStageLabel,
    String status,
    String statusLabel,
    Integer version,
    List<SystemTeamCheckTemplateItemResponse> items,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
