package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("three_check_record")
public class ThreeCheckRecord {
  @TableId public Long id;
  public String moduleKey;
  public String recordNo;
  public Long taskId;
  public Long rootDispatchRecordId;
  public Long companyId;
  public Long departmentId;
  public Long teamId;
  public Long ownerUserId;
  public LocalDate businessDate;
  public String status;
  public String payloadJson;
  public String imageCheckStatus;
  public String videoCheckStatus;
  public Long submittedBy;
  public LocalDateTime submittedAt;
  public Long withdrawnBy;
  public LocalDateTime withdrawnAt;
  public String withdrawReason;
  public Integer reminderCount;
  public LocalDateTime lastRemindedAt;
  public Integer version;
  public Long createdBy;
  public Long updatedBy;
  public String sourceChannel;
  public String sourceRecordId;
  public String clientRequestId;
  public LocalDateTime clientUpdatedAt;
  public LocalDateTime lastSyncedAt;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Integer deleted;
}
