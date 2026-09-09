package com.pingan.banzu.common;

public enum TrainingExamQuestionType {
  SINGLE_CHOICE("单选题"),
  MULTIPLE_CHOICE("多选题"),
  SHORT_ANSWER("问答题"),
  CASE_ANALYSIS("案例分析题");

  private final String label;

  TrainingExamQuestionType(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public static TrainingExamQuestionType parse(String value) {
    if (value != null) {
      for (TrainingExamQuestionType type : values()) {
        if (type.name().equalsIgnoreCase(value.trim()) || type.label.equals(value.trim())) {
          return type;
        }
      }
    }
    throw new BusinessException("题型仅支持单选题、多选题、问答题或案例分析题");
  }
}
