package com.pingan.banzu.dto;

import java.util.List;
import java.util.Map;

public record ThreeCheckRecordStatisticsResponse(
    long total,
    Map<String, Long> statusCounts,
    AttachmentCounts attachmentCounts,
    List<DateCount> dateCounts,
    List<OrganizationCount> companyCounts,
    List<OrganizationCount> departmentCounts,
    List<OrganizationCount> teamCounts) {

  public record AttachmentCounts(
      long imageUploaded, long imageMissing, long videoUploaded, long videoMissing) {}

  public record DateCount(String date, long count) {}

  public record OrganizationCount(
      Long organizationId, String organizationName, String organizationType, long count) {}
}
