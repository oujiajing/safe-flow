package com.pingan.banzu.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SystemPersonnelRequest(
    @NotBlank(message = "员工编码不能为空") String employeeCode,
    @NotBlank(message = "姓名不能为空") String name,
    String username,
    @NotNull(message = "所属公司不能为空") Long companyOrgId,
    String companyShortName,
    Long departmentOrgId,
    Long teamOrgId,
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
