package com.pingan.banzu.service;

import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.common.ThreeCheckBizType;
import com.pingan.banzu.common.ThreeCheckStatus;
import com.pingan.banzu.domain.ThreeCheckRecord;
import com.pingan.banzu.dto.QuickShotWorkflowActionRequest;
import com.pingan.banzu.security.CurrentUser;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
class QuickShotWorkflowService {

  private static final String MODULE_KEY = "quick-shot";
  private static final String BIZ_TYPE = ThreeCheckBizType.HAZARD_QUICK_SHOT;
  private static final DateTimeFormatter DISPLAY_DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final ThreeCheckRecordConcurrentUpdater concurrentUpdater;
  private final ThreeCheckRecordPayloadCodec payloadCodec;
  private final ThreeCheckRecordHistoryService historyService;
  private final ThreeCheckWorkflowSupport workflowSupport;
  private final HazardPermissionPolicy permissionPolicy;
  private final HazardRectificationOrderService rectificationOrderService;

  QuickShotWorkflowService(
      ThreeCheckRecordConcurrentUpdater concurrentUpdater,
      ThreeCheckRecordPayloadCodec payloadCodec,
      ThreeCheckRecordHistoryService historyService,
      ThreeCheckWorkflowSupport workflowSupport,
      HazardPermissionPolicy permissionPolicy,
      HazardRectificationOrderService rectificationOrderService) {
    this.concurrentUpdater = concurrentUpdater;
    this.payloadCodec = payloadCodec;
    this.historyService = historyService;
    this.workflowSupport = workflowSupport;
    this.permissionPolicy = permissionPolicy;
    this.rectificationOrderService = rectificationOrderService;
  }

  void execute(
      ThreeCheckRecord record, QuickShotWorkflowActionRequest request, CurrentUser operator) {
    String action = normalizeAction(request == null ? null : request.action());
    assertPermission(action);
    requireVersionMatch(record, request == null ? null : request.version());
    if (isLegacyRectificationAction(action)) {
      throw new BusinessException(
          "该随手拍已迁入统一隐患整改工单，请在统一隐患整改工单中处理整改流程");
    }

    String from = record.status;
    String to = nextStatus(from, action);
    Map<String, Object> payload =
        applyPayload(
            payloadCodec.read(record.payloadJson),
            request == null ? null : request.payload(),
            action,
            to,
            operator);
    payload.put("statusLabel", statusLabel(to));
    record.status = to;
    record.payloadJson = payloadCodec.write(payload);
    record.updatedBy = operator.userId();
    record.updatedAt = LocalDateTime.now();
    concurrentUpdater.update(record);

    String remark =
        request == null || request.remark() == null || request.remark().isBlank()
            ? actionRemark(action)
            : request.remark().trim();
    workflowSupport.writeStatusLog(
        BIZ_TYPE,
        record.id,
        from,
        to,
        action,
        operator.userId(),
        remark,
        record.payloadJson);
    historyService.record(
        MODULE_KEY,
        BIZ_TYPE,
        record,
        action,
        "status",
        "状态",
        statusLabel(from),
        statusLabel(to),
        "STATUS",
        operator.userId(),
        remark);
    if ("APPROVE".equals(action)) {
      rectificationOrderService.createOrUpdateFromQuickShot(record.id, operator.userId());
    }
  }

  void assertCanExecute(QuickShotWorkflowActionRequest request) {
    assertPermission(normalizeAction(request == null ? null : request.action()));
  }

  private String normalizeAction(String action) {
    if (action == null || action.isBlank()) {
      throw new BusinessException("随手拍流程动作不能为空");
    }
    String normalized = action.trim().toUpperCase(Locale.ROOT);
    return switch (normalized) {
      case "APPROVE", "REJECT", "ISSUE_RECTIFICATION", "MARK_RECTIFIED", "REQUEST_ACCEPTANCE", "ACCEPT" ->
          normalized;
      default -> throw new BusinessException("随手拍流程动作不支持：" + action);
    };
  }

  private void assertPermission(String action) {
    switch (action) {
      case "APPROVE", "REJECT", "ACCEPT" -> permissionPolicy.assertCanReviewQuickShot();
      case "ISSUE_RECTIFICATION", "MARK_RECTIFIED", "REQUEST_ACCEPTANCE" ->
          permissionPolicy.assertCanRectifyOrder();
      default -> throw new BusinessException("随手拍流程动作不支持：" + action);
    }
  }

  private boolean isLegacyRectificationAction(String action) {
    return "ISSUE_RECTIFICATION".equals(action)
        || "MARK_RECTIFIED".equals(action)
        || "REQUEST_ACCEPTANCE".equals(action)
        || "ACCEPT".equals(action);
  }

  private String nextStatus(String currentStatus, String action) {
    String normalizedStatus = normalizeLegacyStatus(currentStatus);
    return switch (action) {
      case "APPROVE" ->
          requireTransition(
              normalizedStatus,
              ThreeCheckStatus.PENDING_REVIEW,
              ThreeCheckStatus.REVIEWED,
              action);
      case "REJECT" ->
          requireTransition(
              normalizedStatus,
              ThreeCheckStatus.PENDING_REVIEW,
              ThreeCheckStatus.REJECTED,
              action);
      case "ISSUE_RECTIFICATION" ->
          requireTransition(
              normalizedStatus,
              ThreeCheckStatus.REVIEWED,
              ThreeCheckStatus.PENDING_RECTIFICATION,
              action);
      case "MARK_RECTIFIED" ->
          requireTransition(
              normalizedStatus,
              ThreeCheckStatus.PENDING_RECTIFICATION,
              ThreeCheckStatus.RECTIFIED,
              action);
      case "REQUEST_ACCEPTANCE" ->
          requireTransition(
              normalizedStatus,
              ThreeCheckStatus.RECTIFIED,
              ThreeCheckStatus.PENDING_ACCEPTANCE,
              action);
      case "ACCEPT" ->
          requireTransition(
              normalizedStatus,
              ThreeCheckStatus.PENDING_ACCEPTANCE,
              ThreeCheckStatus.ACCEPTED,
              action);
      default -> throw new BusinessException("随手拍流程动作不支持：" + action);
    };
  }

  private String requireTransition(
      String currentStatus, String expectedStatus, String nextStatus, String action) {
    if (!expectedStatus.equals(currentStatus)) {
      throw new BusinessException(
          "随手拍流程动作"
              + actionRemark(action)
              + "不适用于当前状态："
              + statusLabel(currentStatus));
    }
    return nextStatus;
  }

  private Map<String, Object> applyPayload(
      Map<String, Object> existingPayload,
      Map<String, Object> actionPayload,
      String action,
      String toStatus,
      CurrentUser operator) {
    Map<String, Object> payload = new LinkedHashMap<>(existingPayload);
    if (actionPayload != null) {
      payload.putAll(actionPayload);
    }
    payload.put("status", statusLabel(toStatus));
    String now = LocalDateTime.now().format(DISPLAY_DATE_TIME_FORMATTER);
    String operatorName =
        operator.realName() == null || operator.realName().isBlank()
            ? operator.username()
            : operator.realName();
    switch (action) {
      case "APPROVE" -> {
        payload.put("reviewedBy", operatorName);
        payload.put("reviewedByUserId", operator.userId());
        payload.put("reviewedAt", now);
        payload.putIfAbsent("hazardReviewConclusion", "隐患存在");
      }
      case "REJECT" -> {
        requireText(payload, "rejectReason", "驳回原因");
        payload.put("rejectedBy", operatorName);
        payload.put("rejectedByUserId", operator.userId());
        payload.put("rejectedAt", now);
        payload.putIfAbsent("hazardReviewConclusion", "隐患不成立");
      }
      default -> {
        // 整改动作已由统一隐患整改工单接管，在进入本方法前会被拒绝。
      }
    }
    return payload;
  }

  private void requireVersionMatch(ThreeCheckRecord record, Integer requestVersion) {
    if (requestVersion != null && !requestVersion.equals(record.version)) {
      throw new com.pingan.banzu.common.ConflictException("记录已被其他端更新，请刷新后重试");
    }
  }

  private void requireText(Map<String, Object> payload, String key, String label) {
    Object value = payload.get(key);
    if (value == null || String.valueOf(value).trim().isBlank()) {
      throw new BusinessException(label + "不能为空");
    }
  }

  private String normalizeLegacyStatus(String status) {
    return switch (status) {
      case ThreeCheckStatus.DRAFT, ThreeCheckStatus.OPENED -> ThreeCheckStatus.PENDING_REVIEW;
      case ThreeCheckStatus.ARCHIVED -> ThreeCheckStatus.ACCEPTED;
      default -> status;
    };
  }

  private String statusLabel(String status) {
    return switch (normalizeLegacyStatus(status)) {
      case ThreeCheckStatus.REVIEWED -> "已审核";
      case ThreeCheckStatus.PENDING_RECTIFICATION -> "待整改";
      case ThreeCheckStatus.RECTIFIED -> "已整改";
      case ThreeCheckStatus.PENDING_ACCEPTANCE -> "待验收";
      case ThreeCheckStatus.ACCEPTED -> "已验收";
      case ThreeCheckStatus.REJECTED -> "已驳回";
      default -> "待审核";
    };
  }

  private String actionRemark(String action) {
    return switch (action) {
      case "APPROVE" -> "审核通过";
      case "REJECT" -> "驳回";
      case "ISSUE_RECTIFICATION" -> "下发整改";
      case "MARK_RECTIFIED" -> "整改完成";
      case "REQUEST_ACCEPTANCE" -> "提交验收";
      case "ACCEPT" -> "验收通过";
      default -> action;
    };
  }
}
