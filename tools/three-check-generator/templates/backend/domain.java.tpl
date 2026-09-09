package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("{{tableName}}")
public class {{entityName}} {
  @TableId public Long id;
  public Long taskId;
  public String {{primaryNoField}};
  public LocalDate {{businessDateField}};
  public String status;
  public String sourceChannel;
  public String sourceRecordId;
  public String clientRequestId;
  public Integer version;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Integer deleted;
}
