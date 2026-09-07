package com.pingan.banzu.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_login_attempt")
public class SysLoginAttempt {
  @TableId public Long id;
  public String username;
  public Long userId;
  public Integer success;
  public String failureReason;
  public String ipAddress;
  public LocalDateTime attemptedAt;
  public LocalDateTime lockedUntil;
}
