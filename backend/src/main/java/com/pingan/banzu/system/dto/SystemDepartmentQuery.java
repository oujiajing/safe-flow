package com.pingan.banzu.system.dto;

public record SystemDepartmentQuery(Long companyOrgId, Long organizationId, String keyword, String status, Integer page, Integer pageSize) {}
