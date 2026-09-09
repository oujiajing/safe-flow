package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("training_exam_task")
public class TrainingExamTask {
  @TableId public Long id;
  public String code;
  public Long companyId;
  public Long departmentId;
  public Long teamId;
  public String exam;
  public LocalDate examDate;
  public Integer durationMinutes;
  public String status;
  public String remark;
  public Long createdBy;
  public Long updatedBy;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Integer deleted;
}
