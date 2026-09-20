package com.pingan.banzu.dto;

import java.util.Map;

public record QuickShotWorkflowActionRequest(
    String action,
    Map<String, Object> payload,
    String remark,
    Integer version,
    QuickShotAgentConfirmation agentConfirmation) {
  public QuickShotWorkflowActionRequest(
      String action, Map<String, Object> payload, String remark, Integer version) {
    this(action, payload, remark, version, null);
  }
}
