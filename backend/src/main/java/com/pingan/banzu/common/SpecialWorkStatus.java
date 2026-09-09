package com.pingan.banzu.common;

import java.util.Arrays;

public enum SpecialWorkStatus {
  PENDING_APPROVAL("待审批"),
  IN_PROGRESS("作业中"),
  PENDING_ACCEPTANCE("待验收"),
  COMPLETED("已完成");

  private final String label;

  SpecialWorkStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public static SpecialWorkStatus parse(String value) {
    if (value == null || value.isBlank()) {
      return PENDING_APPROVAL;
    }
    String normalized = value.trim();
    return Arrays.stream(values())
        .filter(status -> status.name().equalsIgnoreCase(normalized) || status.label.equals(normalized))
        .findFirst()
        .orElseThrow(() -> new BusinessException("不支持的特殊作业状态"));
  }
}
