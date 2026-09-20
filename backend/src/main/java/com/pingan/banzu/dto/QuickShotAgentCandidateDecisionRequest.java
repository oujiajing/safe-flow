package com.pingan.banzu.dto;

import java.util.List;

public record QuickShotAgentCandidateDecisionRequest(List<Item> decisions) {
  public record Item(
      String candidateId,
      String decision,
      String editedHazardType,
      String editedDescription,
      String reviewerNote) {}
}
