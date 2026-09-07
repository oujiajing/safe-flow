package com.pingan.banzu.system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SystemTeamCheckTemplateRequest(
    @NotBlank(message = "模板名称不能为空") String name,
    @NotNull(message = "所属公司不能为空") Long companyOrgId,
    Long departmentOrgId,
    Long teamOrgId,
    @NotBlank(message = "检查阶段不能为空") String inspectionStage,
    String status,
    Integer version,
    @Valid List<SystemTeamCheckTemplateItemRequest> items) {}
