package com.pingan.banzu.dto;

/** Explicit user acknowledgement required before the existing APPROVE path may apply an Agent draft. */
public record QuickShotAgentConfirmation(
    String runId,
    String assessmentId,
    Integer decisionSetVersion,
    String previewDigest,
    Boolean confirmation) {}
