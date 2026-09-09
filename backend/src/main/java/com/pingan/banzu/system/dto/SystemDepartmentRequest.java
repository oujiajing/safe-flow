package com.pingan.banzu.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SystemDepartmentRequest(
    @NotBlank(message = "部门编码不能为空") String code,
    @NotBlank(message = "部门名称不能为空") String name,
    @NotNull(message = "所属公司不能为空") Long companyOrgId,
    String departmentType,
    Integer childSortOrder,
    String leaderUsername,
    String description,
    String status,
    String topLevelName,
    String groupName,
    String level1Unit,
    String level2Unit,
    String leaderLevel,
    Integer companySortOrder) {}
