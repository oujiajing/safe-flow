package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_user")
public class SysUser {
  @TableId public Long id;
  public String username;
  public String passwordHash;
  public String realName;
  public String mobile;
  public Long orgId;
  public String status;
  public Long authVersion;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Integer deleted;
}
