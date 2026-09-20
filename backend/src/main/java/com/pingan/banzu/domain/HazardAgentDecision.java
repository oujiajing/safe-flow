package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("hazard_agent_decision")
public class HazardAgentDecision {
  @TableId public Long id;
  public String runId;
  public String candidateId;
  public String modelOutputJson;
  public String decision;
  public String editedHazardType;
  public String editedDescription;
  public String editedRiskLevel;
  public String editedMeasuresJson;
  public String reviewerNote;
  public Long decidedBy;
  public LocalDateTime decidedAt;
  public Integer decisionVersion;
  public LocalDateTime createdAt;
}
