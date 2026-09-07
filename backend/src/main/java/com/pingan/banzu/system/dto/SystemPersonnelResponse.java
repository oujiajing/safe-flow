package com.pingan.banzu.system.dto;

import java.time.LocalDate;

public record SystemPersonnelResponse(
    Long id,
    String employeeCode,
    String name,
    String username,
    Long companyOrgId,
    String companyName,
    String companyShortName,
    Long departmentOrgId,
    String departmentName,
    Long teamOrgId,
    String teamName,
    Integer points,
    Integer receivedPoints,
    String employeeType,
    String positionName,
    String mobile,
    String status,
    String systemRoleCode,
    LocalDate submitDate,
    String applicantName,
    String remark,
    LocalDate certificateValidUntil,
    LocalDate joinDate,
    Integer departmentSortOrder,
    Integer managementWeight) {}
