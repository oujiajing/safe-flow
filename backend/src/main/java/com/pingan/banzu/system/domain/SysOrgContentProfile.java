package com.pingan.banzu.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_org_content_profile")
public class SysOrgContentProfile {
  @TableId public Long id;
  public Long orgId;
  public String title;
  public String subtitle;
  public String description;
  public Long imageAttachmentId;
  public Long videoAttachmentId;
  public String videoTitle;
  public Integer videoSortOrder;
  public String status;
  public Long createdBy;
  public Long updatedBy;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Long deleted;
}
