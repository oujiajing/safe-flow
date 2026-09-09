package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("shift_task")
public class ShiftTask {
  @TableId public Long id;
  public String taskNo;
  public Long companyId;
  public Long departmentId;
  public Long teamId;
  public LocalDate shiftDate;
  public String shiftName;
  public Long leaderUserId;
  public String status;
  public String taskType;
  public String sourceChannel;
  public String sourceRecordId;
  public String clientRequestId;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Integer deleted;
}
