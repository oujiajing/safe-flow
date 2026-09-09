package com.pingan.banzu.dto;

import java.util.List;

public record MineOverviewResponse(
    Profile profile,
    SafetySummary safetySummary,
    Capabilities capabilities) {

  public record Profile(
      Long userId,
      String username,
      String realName,
      String employeeCode,
      String avatar,
      Long companyId,
      String companyName,
      Long departmentId,
      String departmentName,
      Long teamId,
      String teamName,
      String positionName,
      List<String> roleNames) {}

  public record SafetySummary(
      int points,
      int completedLearningCount,
      int passedExamCount) {}

  public record Capabilities(
      boolean dispatchRecords,
      boolean threeCheckRecords,
      boolean hazardReports,
      boolean rectificationRecords,
      boolean learningRecords,
      boolean examRecords,
      boolean pointFlow) {}
}
