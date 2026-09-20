package com.pingan.banzu.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.List;

public record QuickShotAgentRunResponse(
    String runId,
    String recordId,
    int recordVersion,
    String status,
    String knowledgeStatus,
    String analysisId,
    String assessmentId,
    String model,
    String inputText,
    JsonNode modelOutput,
    JsonNode assessment,
    List<Attachment> attachments,
    List<Decision> decisions,
    String errorCode,
    String errorMessage,
    LocalDateTime startedAt,
    LocalDateTime completedAt) {
  public record Attachment(String attachmentId, String sha256, String mimeType, Long fileSize, int imageIndex) {}
  public record Decision(
      String candidateId, String decision, String editedHazardType, String editedDescription,
      String editedRiskLevel, JsonNode editedMeasures, String reviewerNote, int decisionVersion,
      LocalDateTime decidedAt) {}
}
