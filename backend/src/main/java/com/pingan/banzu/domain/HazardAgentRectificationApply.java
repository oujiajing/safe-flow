package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("hazard_agent_rectification_apply")
public class HazardAgentRectificationApply {
  @TableId public Long id;
  public String runId;
  public String assessmentId;
  public String sourceModuleKey;
  public Long sourceRecordId;
  public Integer sourceRecordVersion;
  public Integer decisionSetVersion;
  public String previewDigest;
  public String idempotencyKey;
  public String status;
  public Long orderId;
  public String snapshotJson;
  public Long actorUserId;
  public String errorCode;
  public String errorMessage;
  public LocalDateTime createdAt;
  public LocalDateTime appliedAt;
  public LocalDateTime updatedAt;
}
