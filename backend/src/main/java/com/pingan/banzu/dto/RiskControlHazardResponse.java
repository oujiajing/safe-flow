package com.pingan.banzu.dto;

import java.time.LocalDateTime;

public record RiskControlHazardResponse(
    Long id,
    Long libraryId,
    Long companyId,
    String company,
    String riskPoint,
    String dangerSource,
    String riskInfluenceFactors,
    String accidentType,
    String likelihood,
    String exposureFrequency,
    String consequence,
    String riskValue,
    String riskLevel,
    String engineeringMeasures,
    String managementMeasures,
    String emergencyMeasures,
    String superiorResponsiblePerson,
    String responsibleDepartment,
    String responsibleContact,
    String possibleHazard,
    String rectificationMeasures,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
