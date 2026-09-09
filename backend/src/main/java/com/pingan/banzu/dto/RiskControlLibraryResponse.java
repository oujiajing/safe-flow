package com.pingan.banzu.dto;

import java.time.LocalDateTime;

public record RiskControlLibraryResponse(
    Long id,
    String name,
    Long companyId,
    String company,
    long hazardCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
