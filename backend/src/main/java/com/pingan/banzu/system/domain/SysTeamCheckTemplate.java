package com.pingan.banzu.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_team_check_template")
public class SysTeamCheckTemplate {
  @TableId public Long id;
  public String name;
  public Long companyOrgId;
  public Long departmentOrgId;
  public Long teamOrgId;
  public String inspectionStage;
  public String status;
  public Integer version;
  public Long createdBy;
  public Long updatedBy;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Long deleted;
}
