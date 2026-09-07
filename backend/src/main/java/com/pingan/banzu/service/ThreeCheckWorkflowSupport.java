package com.pingan.banzu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.common.SourceChannel;
import com.pingan.banzu.common.ThreeCheckStatus;
import com.pingan.banzu.domain.BizRemindRecord;
import com.pingan.banzu.domain.BizStatusLog;
import com.pingan.banzu.domain.SysOrg;
import com.pingan.banzu.mapper.BizRemindRecordMapper;
import com.pingan.banzu.mapper.BizStatusLogMapper;
import com.pingan.banzu.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ThreeCheckWorkflowSupport {

  private final BizRemindRecordMapper remindRecordMapper;
  private final BizStatusLogMapper statusLogMapper;

  public ThreeCheckWorkflowSupport(
      BizRemindRecordMapper remindRecordMapper, BizStatusLogMapper statusLogMapper) {
    this.remindRecordMapper = remindRecordMapper;
    this.statusLogMapper = statusLogMapper;
  }

  public void requireSubmittable(String status) {
    if (!ThreeCheckStatus.EDITABLE.contains(status)) {
      throw new BusinessException("只有待完善或已撤回的班前会可以提交");
    }
  }

  public void requireWithdrawable(String status) {
    if (!ThreeCheckStatus.WITHDRAWABLE.contains(status)) {
      throw new BusinessException("只有已开班会或已归档的班前会可以撤回");
    }
  }

  public boolean canSubmit(String status) {
    return ThreeCheckStatus.EDITABLE.contains(status);
  }

  public boolean canWithdraw(String status) {
    return ThreeCheckStatus.WITHDRAWABLE.contains(status);
  }

  public boolean canAccess(List<Long> orgIds, CurrentUser user, Map<Long, SysOrg> orgs) {
    if (user.isAdmin()) {
      return true;
    }
    return orgIds.stream()
        .map(orgs::get)
        .anyMatch(org -> org != null && org.orgPath.startsWith(user.orgPath()));
  }

  public String resolveSourceChannel(String requestedSourceChannel) {
    if (requestedSourceChannel == null || requestedSourceChannel.isBlank()) {
      return SourceChannel.PC;
    }
    String normalized = requestedSourceChannel.trim().toUpperCase(Locale.ROOT);
    if (!SourceChannel.supports(normalized)) {
      throw new BusinessException("来源渠道不合法");
    }
    return normalized;
  }

  public void writeStatusLog(
      String bizType, Long bizId, String from, String to, String action, Long operatorId, String remark) {
    writeStatusLog(bizType, bizId, from, to, action, operatorId, remark, null);
  }

  public void writeStatusLog(
      String bizType,
      Long bizId,
      String from,
      String to,
      String action,
      Long operatorId,
      String remark,
      String payloadJson) {
    BizStatusLog log = new BizStatusLog();
    log.bizType = bizType;
    log.bizId = bizId;
    log.fromStatus = from;
    log.toStatus = to;
    log.action = action;
    log.operatorId = operatorId;
    log.remark = remark;
    log.payloadJson = payloadJson;
    log.createdAt = LocalDateTime.now();
    statusLogMapper.insert(log);
  }

  public List<BizStatusLog> statusLogsFor(String bizType, Long bizId) {
    return statusLogMapper.selectList(
        new QueryWrapper<BizStatusLog>()
            .eq("biz_type", bizType)
            .eq("biz_id", bizId)
            .orderByAsc("created_at", "id"));
  }

  public void writeRemindRecord(
      String bizType, Long bizId, Long recipientUserId, String content, Long operatorId) {
    BizRemindRecord record = new BizRemindRecord();
    record.bizType = bizType;
    record.bizId = bizId;
    record.recipientUserId = recipientUserId;
    record.remindChannel = "IN_APP";
    record.content = content;
    record.sendStatus = "SENT";
    record.createdBy = operatorId;
    record.createdAt = LocalDateTime.now();
    remindRecordMapper.insert(record);
  }
}
