package com.pingan.banzu.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_menu")
public class SysMenu {
  @TableId public Long id;
  public Long parentId;
  public String menuCode;
  public String title;
  public String routePath;
  public String component;
  public String icon;
  public String permissionCode;
  public Integer sortOrder;
  public Integer visible;
  public String status;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Integer deleted;
}
