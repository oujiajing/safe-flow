package com.pingan.banzu.system.dto;

public record SystemContentProfileQuery(
    Long orgId,
    String keyword,
    String status,
    Integer page,
    Integer pageSize) {}
