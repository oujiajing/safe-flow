package com.pingan.banzu.system.dto;

public record SystemTeamCheckTemplateQuery(
    Long companyOrgId,
    Long departmentOrgId,
    Long teamOrgId,
    Long organizationId,
    String stage,
    String status,
    String keyword,
    Boolean exactScope,
    Integer page,
    Integer pageSize) {}
