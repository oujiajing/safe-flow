package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("hazard_rectification_order")
public class HazardRectificationOrder {
  @TableId public Long id;
  public String orderNo;
  public String sourceType;
  public String sourceModuleKey;
  public Long sourceRecordId;
  public String sourceRecordNo;
  public Long rootDispatchRecordId;
  public Long companyId;
  public Long departmentId;
  public Long teamId;
  public LocalDate businessDate;
  public Integer hazardCount;
  public String status;
  public Long rectificationDepartmentId;
  public Long rectificationResponsibleUserId;
  public String rectificationRequirement;
  public LocalDateTime rectificationDeadline;
  public Long issuedBy;
  public LocalDateTime issuedAt;
  public Long rectifiedBy;
  public LocalDateTime rectifiedAt;
  public String rectificationDescription;
  public String rectificationAfterPhoto;
  public Long acceptanceUserId;
  public Long acceptanceDepartmentId;
  public LocalDateTime acceptanceAt;
  public String acceptanceResult;
  public String acceptanceRemark;
  public LocalDateTime closedAt;
  public Integer version;
  public Long createdBy;
  public Long updatedBy;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Integer deleted;
}
