package com.pingan.banzu.dto;

import java.time.LocalDate;
import java.util.List;

public record ThreeCheckRecordSqlCriteria(
    String moduleKey,
    List<String> statuses,
    Long companyId,
    Long departmentId,
    Long teamId,
    Long rootDispatchRecordId,
    String sourceChannel,
    String pointsReason,
    LocalDate dateStart,
    LocalDate dateEnd,
    String companyKeyword,
    String departmentKeyword,
    String teamKeyword,
    Long selfOwnerUserId,
    List<Long> accessOrgIds,
    boolean accessDenied,
    List<Long> filterOrgIds,
    boolean filterOrgDenied,
    List<Long> visibleTeamIds,
    List<Long> visibleOwnerUserIds,
    boolean curtainWallOnly,
    Boolean overdue,
    LocalDate overdueBeforeDate) {}
