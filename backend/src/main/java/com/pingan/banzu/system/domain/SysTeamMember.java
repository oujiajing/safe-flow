package com.pingan.banzu.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_team_member")
public class SysTeamMember {
  @TableId public Long id;
  public Long teamOrgId;
  public Long userId;
  public String username;
  public String memberName;
  public String memberRole;
  public LocalDateTime createdAt;
  public Integer deleted;
}
