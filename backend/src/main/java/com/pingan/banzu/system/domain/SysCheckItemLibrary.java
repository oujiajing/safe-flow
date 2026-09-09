package com.pingan.banzu.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_check_item_library")
public class SysCheckItemLibrary {
  @TableId public Long id;
  public String riskType;
  public String checkItem;
  public String applicableStage;
  public String defaultCheckResult;
  public String defaultRectificationDescription;
  public String defaultFollowUpPlan;
  public Integer requireImage;
  public Integer requireVideo;
  public Integer sortOrder;
  public String status;
  public Long createdBy;
  public Long updatedBy;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Long deleted;
}
