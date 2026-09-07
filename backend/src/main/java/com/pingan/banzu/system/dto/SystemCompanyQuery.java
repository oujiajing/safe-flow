package com.pingan.banzu.system.dto;

public record SystemCompanyQuery(Long organizationId, String keyword, String status, Integer page, Integer pageSize) {}
