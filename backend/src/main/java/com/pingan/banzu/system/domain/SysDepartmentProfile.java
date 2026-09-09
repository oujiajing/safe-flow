package com.pingan.banzu.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_department_profile")
public class SysDepartmentProfile {
  @TableId public Long id;
  public Long orgId;
  public Long companyOrgId;
  public String departmentType;
  public Integer childSortOrder;
  public String leaderUsername;
  public String description;
  public String topLevelName;
  public String groupName;
  public String level1Unit;
  public String level2Unit;
  public String leaderLevel;
  public Integer companySortOrder;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Integer deleted;
}
