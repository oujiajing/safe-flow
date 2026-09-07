package com.pingan.banzu.system.dto;

import jakarta.validation.constraints.NotBlank;

public record SystemCompanyRequest(
    @NotBlank(message = "公司编码不能为空") String code,
    Integer sortOrder,
    @NotBlank(message = "公司名称不能为空") String name,
    String shortName,
    String description,
    String status,
    String address,
    String companyType,
    String level1Name,
    String level2Name,
    String level3Name,
    String level4Name,
    String safetyManagerUsername,
    String reporterL1Usernames,
    String reporterL2Usernames,
    String reporterL3Usernames,
    Integer reportL1Time,
    Integer reportL2Time,
    Integer reportL3Time,
    String attachment1Url,
    String attachment2Url,
    String companyIntro) {}
