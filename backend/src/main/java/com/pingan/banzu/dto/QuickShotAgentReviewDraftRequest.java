package com.pingan.banzu.dto;

import java.util.List;

public record QuickShotAgentReviewDraftRequest(String reviewerNote, List<Item> items) {
  public record Item(
      String candidateId,
      String decision,
      String editedHazardType,
      String editedDescription,
      String editedRiskLevel,
      List<String> editedMeasures,
      String reviewerNote) {}
}
