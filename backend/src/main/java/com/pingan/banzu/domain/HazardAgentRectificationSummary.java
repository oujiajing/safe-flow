package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("hazard_agent_rectification_summary")
public class HazardAgentRectificationSummary {
  @TableId public Long id;
  public Long orderId;
  public Integer orderVersion;
  public Integer summaryVersion;
  public String status;
  public String sourceDigest;
  public String generatedJson;
  public String confirmedJson;
  public Long createdBy;
  public Long confirmedBy;
  public LocalDateTime createdAt;
  public LocalDateTime confirmedAt;
  public LocalDateTime updatedAt;
}
