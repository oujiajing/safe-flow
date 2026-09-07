package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("training_exam_result")
public class TrainingExamResult {
  @TableId public Long id;
  public String code;
  public Long taskId;
  public Long companyId;
  public Long departmentId;
  public Long examPersonUserId;
  public String examPersonName;
  public BigDecimal score;
  public LocalDate examDate;
  public String status;
  public String remark;
  public String submissionRequestId;
  public LocalDateTime submittedAt;
  public LocalDateTime startedAt;
  public Integer currentQuestionIndex;
  public Integer remainingSeconds;
  public Long createdBy;
  public Long updatedBy;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Integer deleted;
}
