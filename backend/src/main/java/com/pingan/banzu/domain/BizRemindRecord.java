package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("biz_remind_record")
public class BizRemindRecord {
  @TableId public Long id;
  public String bizType;
  public Long bizId;
  public Long recipientUserId;
  public String remindChannel;
  public String content;
  public String sendStatus;
  public Long createdBy;
  public LocalDateTime createdAt;
}
