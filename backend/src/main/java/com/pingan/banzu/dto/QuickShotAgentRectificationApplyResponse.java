package com.pingan.banzu.dto;
import java.time.LocalDateTime;
public record QuickShotAgentRectificationApplyResponse(String runId, String status, String orderId, String errorCode, String errorMessage, LocalDateTime appliedAt) {}
