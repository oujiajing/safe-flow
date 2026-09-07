package com.pingan.banzu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pingan.banzu.domain.BizChangeHistory;
import com.pingan.banzu.domain.SysUser;
import com.pingan.banzu.domain.ThreeCheckRecord;
import com.pingan.banzu.dto.ThreeCheckRecordChangeHistoryItem;
import com.pingan.banzu.mapper.BizChangeHistoryMapper;
import com.pingan.banzu.mapper.SysUserMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
class ThreeCheckRecordHistoryService {

  private static final DateTimeFormatter DISPLAY_DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final BizChangeHistoryMapper changeHistoryMapper;
  private final SysUserMapper userMapper;

  ThreeCheckRecordHistoryService(
      BizChangeHistoryMapper changeHistoryMapper, SysUserMapper userMapper) {
    this.changeHistoryMapper = changeHistoryMapper;
    this.userMapper = userMapper;
  }

  List<ThreeCheckRecordChangeHistoryItem> list(String bizType, Long recordId) {
    Map<Long, SysUser> users =
        userMapper.selectList(null).stream().collect(Collectors.toMap(user -> user.id, user -> user));
    return changeHistoryMapper
        .selectList(
            new QueryWrapper<BizChangeHistory>()
                .eq("biz_type", bizType)
                .eq("biz_id", recordId)
                .orderByDesc("created_at", "id"))
        .stream()
        .map(history -> toItem(history, users))
        .toList();
  }

  void recordUpdate(
      String moduleKey,
      String bizType,
      ThreeCheckRecord before,
      Map<String, Object> beforePayload,
      ThreeCheckRecord after,
      Map<String, Object> afterPayload,
      Long operatorId,
      BiFunction<String, String, String> statusLabeler) {
    Map<String, Object> beforeHistoryPayload = sanitizePayload(beforePayload);
    Map<String, Object> afterHistoryPayload = sanitizePayload(afterPayload);
    addIfChanged(
        moduleKey,
        bizType,
        after,
        "UPDATE",
        "status",
        "状态",
        statusLabeler.apply(moduleKey, before.status),
        statusLabeler.apply(moduleKey, after.status),
        "STATUS",
        operatorId,
        "更新记录");
    addIfChanged(
        moduleKey,
        bizType,
        after,
        "UPDATE",
        "businessDate",
        "检查日期",
        stringValue(before.businessDate),
        stringValue(after.businessDate),
        "TEXT",
        operatorId,
        "更新记录");
    addIfChanged(
        moduleKey,
        bizType,
        after,
        "UPDATE",
        "ownerUserId",
        "创建人",
        stringValue(before.ownerUserId),
        stringValue(after.ownerUserId),
        "TEXT",
        operatorId,
        "更新记录");

    LinkedHashSet<String> keys = new LinkedHashSet<>();
    keys.addAll(beforeHistoryPayload.keySet());
    keys.addAll(afterHistoryPayload.keySet());
    for (String key : keys) {
      if (!"statusLabel".equals(key)) {
        addIfChanged(
            moduleKey,
            bizType,
            after,
            "UPDATE",
            key,
            fieldLabel(key),
            stringValue(beforeHistoryPayload.get(key)),
            stringValue(afterHistoryPayload.get(key)),
            valueType(key),
            operatorId,
            "更新记录");
      }
    }
  }

  void record(
      String moduleKey,
      String bizType,
      ThreeCheckRecord record,
      String action,
      String fieldKey,
      String fieldLabel,
      String beforeValue,
      String afterValue,
      String valueType,
      Long operatorId,
      String remark) {
    BizChangeHistory history = new BizChangeHistory();
    history.bizType = bizType;
    history.bizId = record.id;
    history.recordNo = record.recordNo;
    history.moduleKey = moduleKey;
    history.version = record.version;
    history.action = action;
    history.fieldKey = fieldKey;
    history.fieldLabel = fieldLabel;
    history.beforeValue = blankToNull(beforeValue);
    history.afterValue = blankToNull(afterValue);
    history.valueType = valueType;
    history.operatorId = operatorId;
    history.remark = blankToNull(remark);
    history.createdAt = LocalDateTime.now();
    changeHistoryMapper.insert(history);
  }

  ThreeCheckRecord copyOf(ThreeCheckRecord source) {
    ThreeCheckRecord copy = new ThreeCheckRecord();
    copy.id = source.id;
    copy.moduleKey = source.moduleKey;
    copy.recordNo = source.recordNo;
    copy.taskId = source.taskId;
    copy.companyId = source.companyId;
    copy.departmentId = source.departmentId;
    copy.teamId = source.teamId;
    copy.ownerUserId = source.ownerUserId;
    copy.businessDate = source.businessDate;
    copy.status = source.status;
    copy.payloadJson = source.payloadJson;
    copy.imageCheckStatus = source.imageCheckStatus;
    copy.videoCheckStatus = source.videoCheckStatus;
    copy.reminderCount = source.reminderCount;
    copy.version = source.version;
    return copy;
  }

  private void addIfChanged(
      String moduleKey,
      String bizType,
      ThreeCheckRecord record,
      String action,
      String fieldKey,
      String fieldLabel,
      String beforeValue,
      String afterValue,
      String valueType,
      Long operatorId,
      String remark) {
    if (!Objects.equals(blankToNull(beforeValue), blankToNull(afterValue))) {
      record(
          moduleKey,
          bizType,
          record,
          action,
          fieldKey,
          fieldLabel,
          beforeValue,
          afterValue,
          valueType,
          operatorId,
          remark);
    }
  }

  private Map<String, Object> sanitizePayload(Map<String, Object> payload) {
    Map<String, Object> sanitized = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : payload.entrySet()) {
      if (!isRectificationLinkageField(entry.getKey())) {
        sanitized.put(entry.getKey(), sanitizeValue(entry.getValue()));
      }
    }
    return sanitized;
  }

  private Object sanitizeValue(Object value) {
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> sanitized = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        String key = String.valueOf(entry.getKey());
        if (!isRectificationLinkageField(key)) {
          sanitized.put(key, sanitizeValue(entry.getValue()));
        }
      }
      return sanitized;
    }
    if (value instanceof List<?> list) {
      return list.stream().map(this::sanitizeValue).toList();
    }
    return value;
  }

  private boolean isRectificationLinkageField(String key) {
    return "rectificationOrderId".equals(key)
        || "rectificationOrderNo".equals(key)
        || "rectificationStatus".equals(key)
        || "rectificationStatusLabel".equals(key)
        || "rectificationClosedAt".equals(key)
        || "lastRectificationAction".equals(key)
        || "lastRectificationRemark".equals(key);
  }

  private ThreeCheckRecordChangeHistoryItem toItem(
      BizChangeHistory history, Map<Long, SysUser> users) {
    SysUser operator = history.operatorId == null ? null : users.get(history.operatorId);
    return new ThreeCheckRecordChangeHistoryItem(
        String.valueOf(history.id),
        history.action,
        history.fieldKey,
        history.fieldLabel,
        history.beforeValue,
        history.afterValue,
        history.valueType,
        history.operatorId == null ? null : String.valueOf(history.operatorId),
        operator == null ? "" : operator.realName,
        history.remark,
        history.version == null ? 0 : history.version,
        formatDateTime(history.createdAt));
  }

  private String fieldLabel(String key) {
    return switch (key) {
      case "aiEnabled" -> "是否启用AI";
      case "acceptanceDate" -> "验收日期";
      case "acceptancePerson" -> "验收人员";
      case "acceptanceResult", "rectificationAcceptanceResult" -> "整改验收是否通过";
      case "approvedBy" -> "批准人";
      case "attachments", "attachment" -> "附件";
      case "businessDate", "inspectionDate" -> "检查日期";
      case "checkUnit", "inspectionUnit" -> "检查单位";
      case "createdBy" -> "创建人";
      case "hazardDescription" -> "隐患描述";
      case "hazardLibrary" -> "隐患库选择";
      case "imageUpload", "image", "photo", "photoOne" -> "图片上传";
      case "inspectedUnit" -> "受检单位";
      case "inspectedUnitPerson" -> "受检单位人员";
      case "inspectedUnitPersonManual" -> "受检单位人员（手录）";
      case "inspectionType" -> "检查类型";
      case "inspectionTypeManual", "manualInspectionType" -> "检查类型（手录）";
      case "inspectionContent" -> "检查内容";
      case "inspectionMethod" -> "检查方式";
      case "inspectionPersonnel", "inspector", "checkPerson" -> "检查人员";
      case "inspectionPersonnelManual", "inspectorManual" -> "检查人员（手录）";
      case "inspectionTime" -> "检查时间";
      case "laborCompany" -> "劳务单位";
      case "lineCount", "detailCount" -> "明细计数";
      case "owner" -> "负责人";
      case "penaltyConclusion" -> "处罚结论";
      case "pointsChange" -> "积分变动";
      case "pointsQuantity" -> "积分数量";
      case "pointsReason" -> "积分变动原因";
      case "rectificationMeasures" -> "整改措施";
      case "rectificationResponsiblePerson", "responsiblePerson" -> "整改责任人";
      case "rectificationDeadline" -> "整改截止日期";
      case "reporter" -> "上报人";
      case "remarks" -> "备注";
      case "responsibleUnit" -> "责任单位";
      case "routineCheckType" -> "巡检记录表类型";
      case "status", "statusLabel" -> "状态";
      case "user" -> "用户";
      case "vendingMachine" -> "贩卖机";
      case "videoUpload", "video" -> "视频上传";
      default -> key;
    };
  }

  private String valueType(String key) {
    return key.toLowerCase(Locale.ROOT).contains("photo")
            || key.toLowerCase(Locale.ROOT).contains("image")
            || key.contains("图片")
        ? "IMAGE"
        : "TEXT";
  }

  private String stringValue(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Iterable<?> iterable) {
      List<String> values = new ArrayList<>();
      for (Object item : iterable) {
        if (item != null) {
          values.add(String.valueOf(item));
        }
      }
      return String.join("、", values);
    }
    return String.valueOf(value);
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String formatDateTime(LocalDateTime value) {
    return value == null ? null : value.format(DISPLAY_DATE_TIME_FORMATTER);
  }
}
