package com.pingan.banzu.common;

public enum TrainingExamResultStatus {
  PENDING_EXAM("待考试"),
  PENDING_REVIEW("待评分"),
  EXAMED("已考试");

  private final String label;

  TrainingExamResultStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public static TrainingExamResultStatus parse(String value) {
    if (value == null || value.isBlank()) {
      return PENDING_EXAM;
    }
    for (TrainingExamResultStatus status : values()) {
      if (status.name().equalsIgnoreCase(value) || status.label.equals(value)) {
        return status;
      }
    }
    throw new BusinessException("考试成绩状态不合法");
  }
}
