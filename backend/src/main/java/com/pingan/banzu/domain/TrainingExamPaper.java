package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("training_exam_paper")
public class TrainingExamPaper {
  @TableId public Long id;
  public Long companyId;
  public Long departmentId;
  public Long teamId;
  public String paperName;
  public String description;
  public String questionsJson;
  public Integer questionCount;
  public BigDecimal totalScore;
  public Long createdBy;
  public Long updatedBy;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Integer deleted;
}
