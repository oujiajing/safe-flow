package com.pingan.banzu.system.dto;

public record SystemPersonnelQuery(
    Long companyOrgId, Long departmentOrgId, Long teamOrgId, Long organizationId, String keyword, String status, Integer page, Integer pageSize) {}
