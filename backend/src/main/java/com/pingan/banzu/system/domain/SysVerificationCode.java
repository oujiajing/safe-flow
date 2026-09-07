package com.pingan.banzu.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_verification_code")
public class SysVerificationCode {
  @TableId public Long id;
  public String username;
  public String mobile;
  public String codeHash;
  public String purpose;
  public String sendChannel;
  public String status;
  public LocalDateTime expiresAt;
  public LocalDateTime verifiedAt;
  public LocalDateTime createdAt;
}
