package com.pingan.banzu.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("safety_ledger_document")
public class SafetyLedgerDocument {
  @TableId public Long id;
  public String ledgerKey;
  public String name;
  public String richText;
  public Long companyId;
  public String department;
  public String team;
  public String documentType;
  public LocalDate documentDate;
  public Long attachmentId;
  public Long createdBy;
  public Long updatedBy;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Integer deleted;
}
