package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("biz_change_history")
public class BizChangeHistory {
  @TableId public Long id;
  public String bizType;
  public Long bizId;
  public String recordNo;
  public String moduleKey;
  public Integer version;
  public String action;
  public String fieldKey;
  public String fieldLabel;
  public String beforeValue;
  public String afterValue;
  public String valueType;
  public Long operatorId;
  public String remark;
  public LocalDateTime createdAt;
}
