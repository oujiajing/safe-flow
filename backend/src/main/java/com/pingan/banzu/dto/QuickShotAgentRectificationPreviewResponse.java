package com.pingan.banzu.dto;

import java.util.List;

/** Read-only projection of a saved Agent review draft. It is not an order-creation command. */
public record QuickShotAgentRectificationPreviewResponse(
    String runId,
    String assessmentId,
    String recordId,
    int recordVersion,
    int decisionSetVersion,
    String previewDigest,
    List<Item> items,
    List<String> missingFields,
    String existingOrderId,
    boolean canApply) {

  public record Item(
      String candidateId,
      List<String> sourceAttachmentIds,
      String checkItem,
      String hazardDescription,
      String riskType,
      List<String> rectificationMeasures,
      List<String> acceptanceCriteria,
      Object legalEvidence) {}
}
