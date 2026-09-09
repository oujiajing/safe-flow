package com.pingan.banzu.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record SystemTeamRequest(
    @NotBlank(message = "班组编码不能为空") String code,
    @NotBlank(message = "班组名称不能为空") String name,
    @NotNull(message = "所属公司不能为空") Long companyOrgId,
    Long workshopOrgId,
    String groupName,
    String level1Unit,
    String level2Unit,
    String workshopName,
    String workTypeCode,
    String workTypeName,
    String status,
    String leaderUsername,
    List<String> teamMembers,
    String safetyOfficerUsername,
    LocalDate submitDate,
    String applicantName,
    Integer points,
    Boolean active) {}
