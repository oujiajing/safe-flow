package com.pingan.banzu.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_team_check_template_item")
public class SysTeamCheckTemplateItem {
  @TableId public Long id;
  public Long templateId;
  public Long libraryItemId;
  public String riskType;
  public String checkItem;
  public String defaultCheckResult;
  public String defaultRectificationDescription;
  public String defaultFollowUpPlan;
  public Integer requireImage;
  public Integer requireVideo;
  public Integer sortOrder;
  public Long createdBy;
  public Long updatedBy;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Long deleted;
}
