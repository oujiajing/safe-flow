package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("training_safety_learning_content")
public class TrainingSafetyLearningContent {
  @TableId public Long id;
  public Long companyId;
  public String category;
  public String title;
  public String content;
  public String coverImage;
  public Long coverImageId;
  public String video;
  public Long videoId;
  public LocalDate learningDate;
  public String durationText;
  public String code;
  public Long attachmentId;
  public String attachmentText;
  public String htmlExtract;
  public String draft;
  public String status;
  public Long createdBy;
  public Long updatedBy;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Integer deleted;
}
