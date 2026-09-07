package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_org")
public class SysOrg {
  @TableId public Long id;
  public Long parentId;
  public String orgType;
  public String orgCode;
  public String orgName;
  public String orgPath;
  public Integer sortOrder;
  public String status;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Integer deleted;
}
