package com.pingan.banzu.common;

public enum TrainingExamTaskStatus {
  ACTIVE("已生效"),
  INACTIVE("未生效");

  private final String label;

  TrainingExamTaskStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public static TrainingExamTaskStatus parse(String value) {
    if (value == null || value.isBlank()) {
      return ACTIVE;
    }
    for (TrainingExamTaskStatus status : values()) {
      if (status.name().equalsIgnoreCase(value) || status.label.equals(value)) {
        return status;
      }
    }
    throw new BusinessException("考试任务状态不合法");
  }
}
