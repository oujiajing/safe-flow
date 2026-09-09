package com.pingan.banzu.common;

public enum TrainingSafetyLearningStatus {
  DRAFT("草稿"),
  ACTIVE("激活"),
  INACTIVE("作废");

  private final String label;

  TrainingSafetyLearningStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public static TrainingSafetyLearningStatus parse(String value) {
    if (value == null || value.isBlank()) {
      return ACTIVE;
    }
    for (TrainingSafetyLearningStatus status : values()) {
      if (status.name().equalsIgnoreCase(value) || status.label.equals(value)) {
        return status;
      }
    }
    throw new BusinessException("安全学习状态不合法");
  }
}
