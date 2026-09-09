package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("biz_attachment")
public class BizAttachment {
  @TableId public Long id;
  public String bizType;
  public Long bizId;
  public String fileKind;
  public String originalName;
  public String storagePath;
  public String storageProvider;
  public String bucketName;
  public String objectKey;
  public String etag;
  public String contentType;
  public Long fileSize;
  public Long uploadedBy;
  public LocalDateTime uploadedAt;
  public Integer deleted;
}
