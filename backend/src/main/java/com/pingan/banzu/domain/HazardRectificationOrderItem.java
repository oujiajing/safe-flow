package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("hazard_rectification_order_item")
public class HazardRectificationOrderItem {
  @TableId public Long id;
  public Long orderId;
  public String sourceLineId;
  public Integer sourceLineIndex;
  public Long libraryItemId;
  public String riskType;
  public String checkItem;
  public String hazardDescription;
  public String beforePhoto;
  public String beforeVideo;
  public String defaultFollowUpPlan;
  public String sourceSnapshotJson;
  public String rectificationStatus;
  public LocalDateTime closedAt;
  public Integer sortOrder;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Integer deleted;
}
