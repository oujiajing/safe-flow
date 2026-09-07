package com.pingan.banzu.system.dto;

public record SystemDepartmentResponse(
    Long id,
    String code,
    String name,
    Long companyOrgId,
    String companyName,
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
