package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("training_exam_question_bank")
public class TrainingExamQuestionBank {
  @TableId public Long id;
  public Long companyId;
  public Long departmentId;
  public Long teamId;
  public String questionType;
  public String questionText;
  public String selectedOption;
  public String allOptions;
  public String answer;
  public BigDecimal score;
  public BigDecimal actualScore;
  public String optionsJson;
  public String answersJson;
  public String answerExplanation;
  public String caseMaterial;
  public String childrenJson;
  public Long createdBy;
  public Long updatedBy;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Integer deleted;
}
