package com.pingan.banzu.system.dto;

import java.time.LocalDate;
import java.util.List;

public record SystemTeamResponse(
    Long id,
    String code,
    String name,
    Long companyOrgId,
    String companyName,
    Long workshopOrgId,
    String workshopOrgName,
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
