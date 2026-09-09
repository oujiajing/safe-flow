package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("special_work_record")
public class SpecialWorkRecord {
  @TableId public Long id;
  public Long companyId;
  public String project;
  public String workType;
  public LocalDateTime applicationTime;
  public Long imageAttachmentId;
  public String workContent;
  public String workLocation;
  public String riskIdentificationResult;
  public LocalDateTime implementationStartTime;
  public LocalDateTime implementationEndTime;
  public String safetyDisclosurePerson;
  public String guardian;
  public String disclosureReceiver;
  public String completionAcceptor;
  public LocalDateTime completionAcceptanceTime;
  public String status;
  public Long createdBy;
  public Long updatedBy;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Integer deleted;
}
