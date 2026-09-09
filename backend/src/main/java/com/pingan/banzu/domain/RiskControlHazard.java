package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("risk_control_hazard")
public class RiskControlHazard {
  @TableId public Long id;
  public Long libraryId;
  public Long companyId;
  public String riskPoint;
  public String dangerSource;
  public String riskInfluenceFactors;
  public String accidentType;
  public String likelihood;
  public String exposureFrequency;
  public String consequence;
  public String riskValue;
  public String riskLevel;
  public String engineeringMeasures;
  public String managementMeasures;
  public String emergencyMeasures;
  public String superiorResponsiblePerson;
  public String responsibleDepartment;
  public String responsibleContact;
  public String possibleHazard;
  public String rectificationMeasures;
  public Long createdBy;
  public Long updatedBy;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Integer deleted;
}
