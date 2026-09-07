package com.pingan.banzu.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("sys_team_profile")
public class SysTeamProfile {
  @TableId public Long id;
  public Long orgId;
  public Long companyOrgId;
  public Long workshopOrgId;
  public String groupName;
  public String level1Unit;
  public String level2Unit;
  public String workshopName;
  public String workTypeCode;
  public String workTypeName;
  public String leaderUsername;
  public String safetyOfficerUsername;
  public LocalDate submitDate;
  public String applicantName;
  public Integer points;
  public Integer active;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Integer deleted;
}
