package com.pingan.banzu.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_role_menu")
public class SysRoleMenu {
  @TableId public Long id;
  public Long roleId;
  public Long menuId;
  public LocalDateTime createdAt;
}
