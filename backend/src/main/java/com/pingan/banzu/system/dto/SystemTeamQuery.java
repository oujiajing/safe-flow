package com.pingan.banzu.system.dto;

public record SystemTeamQuery(Long companyOrgId, Long workshopOrgId, Long organizationId, String keyword, String status, Integer page, Integer pageSize) {}
