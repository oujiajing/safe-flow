package com.pingan.banzu.dto;

public record RectificationAgentSummaryEditRequest(
    int summaryVersion,
    int orderVersion,
    String editedJson,
    String editReason) {}
