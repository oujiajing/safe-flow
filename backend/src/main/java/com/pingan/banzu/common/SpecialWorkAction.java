package com.pingan.banzu.common;

import java.util.Arrays;

public enum SpecialWorkAction {
  APPROVE_AND_START(SpecialWorkStatus.PENDING_APPROVAL, SpecialWorkStatus.IN_PROGRESS, "批准并开始作业"),
  SUBMIT_ACCEPTANCE(
      SpecialWorkStatus.IN_PROGRESS, SpecialWorkStatus.PENDING_ACCEPTANCE, "结束作业并提交验收"),
  COMPLETE_ACCEPTANCE(
      SpecialWorkStatus.PENDING_ACCEPTANCE, SpecialWorkStatus.COMPLETED, "确认验收完成");

  private final SpecialWorkStatus fromStatus;
  private final SpecialWorkStatus toStatus;
  private final String label;

  SpecialWorkAction(
      SpecialWorkStatus fromStatus, SpecialWorkStatus toStatus, String label) {
    this.fromStatus = fromStatus;
    this.toStatus = toStatus;
    this.label = label;
  }

  public SpecialWorkStatus fromStatus() {
    return fromStatus;
  }

  public SpecialWorkStatus toStatus() {
    return toStatus;
  }

  public String label() {
    return label;
  }

  public static SpecialWorkAction parse(String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessException("特殊作业动作不能为空");
    }
    return Arrays.stream(values())
        .filter(action -> action.name().equalsIgnoreCase(value.trim()))
        .findFirst()
        .orElseThrow(() -> new BusinessException("不支持的特殊作业动作"));
  }
}
