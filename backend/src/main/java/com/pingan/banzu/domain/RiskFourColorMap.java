package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("risk_four_color_map")
public class RiskFourColorMap {
  @TableId public Long id;
  public String name;
  public Long companyId;
  public Long backgroundAttachmentId;
  public String remark;
  public Long createdBy;
  public Long updatedBy;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Integer deleted;
}
