package com.pingan.banzu.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_company_profile")
public class SysCompanyProfile {
  @TableId public Long id;
  public Long orgId;
  public String shortName;
  public String description;
  public String address;
  public String companyType;
  public String level1Name;
  public String level2Name;
  public String level3Name;
  public String level4Name;
  public String safetyManagerUsername;
  public String reporterL1Usernames;
  public String reporterL2Usernames;
  public String reporterL3Usernames;
  public Integer reportL1Time;
  public Integer reportL2Time;
  public Integer reportL3Time;
  public String attachment1Url;
  public String attachment2Url;
  public String companyIntro;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Integer deleted;
}
