package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_user_role")
public class SysUserRole {
  @TableId public Long id;
  public Long userId;
  public Long roleId;
  public LocalDateTime createdAt;
}
