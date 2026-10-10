package com.pingan.banzu.dto;

import java.util.List;

public record RectificationAgentReviewRequest(int orderVersion, List<String> criteria,
    List<String> beforeEvidence, List<String> afterEvidence, String rectificationDescription,
    List<String> beforeImageData, List<String> afterImageData) {
  public RectificationAgentReviewRequest(int orderVersion, List<String> criteria,
      List<String> beforeEvidence, List<String> afterEvidence, String rectificationDescription) {
    this(orderVersion, criteria, beforeEvidence, afterEvidence, rectificationDescription, List.of(), List.of());
  }
}
