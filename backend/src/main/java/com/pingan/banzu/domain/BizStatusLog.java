package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("biz_status_log")
public class BizStatusLog {
  @TableId public Long id;
  public String bizType;
  public Long bizId;
  public String fromStatus;
  public String toStatus;
  public String action;
  public Long operatorId;
  public String remark;
  public String payloadJson;
  public LocalDateTime createdAt;
}
