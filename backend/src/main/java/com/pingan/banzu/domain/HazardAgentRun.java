package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("hazard_agent_run")
public class HazardAgentRun {
  @TableId public Long id;
  public String runId;
  public String sourceModuleKey;
  public Long sourceRecordId;
  public Integer sourceRecordVersion;
  public Long actorUserId;
  public Long companyId;
  public Long departmentId;
  public Long teamId;
  public String inputText;
  public String inputHash;
  public String attachmentHashSet;
  public String model;
  public String promptVersion;
  public String workflowVersion;
  public String analysisId;
  public String assessmentId;
  public String status;
  public String knowledgeStatus;
  public String traceId;
  public String modelOutputJson;
  public String assessmentJson;
  public String errorCode;
  public String errorMessage;
  public LocalDateTime startedAt;
  public LocalDateTime completedAt;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Integer deleted;
}
