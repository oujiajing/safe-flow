package com.pingan.banzu.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("sys_user_profile")
public class SysUserProfile {
  @TableId public Long id;
  public Long userId;
  public String employeeCode;
  public Long companyOrgId;
  public String companyShortName;
  public Long departmentOrgId;
  public Long teamOrgId;
  public Integer points;
  public Integer receivedPoints;
  public String employeeType;
  public String positionName;
  public String systemRoleCode;
  public LocalDate submitDate;
  public String applicantName;
  public String remark;
  public LocalDate certificateValidUntil;
  public LocalDate joinDate;
  public Integer departmentSortOrder;
  public Integer managementWeight;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Integer deleted;
}
