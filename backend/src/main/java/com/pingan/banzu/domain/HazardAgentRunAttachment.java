package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("hazard_agent_run_attachment")
public class HazardAgentRunAttachment {
  @TableId public Long id;
  public String runId;
  public Long attachmentId;
  public String attachmentSha256;
  public String mimeType;
  public Long fileSize;
  public Integer imageIndex;
  public LocalDateTime analyzedAt;
  public LocalDateTime createdAt;
}
