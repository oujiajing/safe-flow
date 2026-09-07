package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("hazard_rectification_flow_log")
public class HazardRectificationFlowLog {
  @TableId public Long id;
  public Long orderId;
  public String fromStatus;
  public String toStatus;
  public String action;
  public String actionLabel;
  public Long operatorId;
  public String remark;
  public String payloadJson;
  public LocalDateTime createdAt;
}
