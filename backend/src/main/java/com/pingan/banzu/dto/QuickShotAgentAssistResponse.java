package com.pingan.banzu.dto;

import java.time.Instant;
import java.util.List;

public record QuickShotAgentAssistResponse(
    String recordId,
    int recordVersion,
    String analysisId,
    String scene,
    String model,
    Instant analyzedAt,
    List<Candidate> hazardCandidates) {

  public record Candidate(
      String candidateId,
      String hazardType,
      String operationObject,
      String description,
      List<String> visibleEvidence,
      String potentialRisk,
      String judgement,
      double confidence,
      boolean needsManualVerification) {}
}
