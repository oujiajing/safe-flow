package com.pingan.banzu.service;

import com.pingan.banzu.common.SpecialWorkStatus;
import com.pingan.banzu.domain.HazardRectificationOrder;
import com.pingan.banzu.domain.SpecialWorkRecord;
import com.pingan.banzu.service.NotificationService.NotificationCommand;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class NotificationEventPublisher {

  private static final Map<String, String> THREE_CHECK_NAMES =
      Map.of(
          "pre-shift-meeting", "班前会",
          "pre-shift-inspection", "班前检查",
          "mid-shift-inspection", "班中检查",
          "post-shift-inspection", "班后检查");

  private final NotificationService notificationService;
  private final NotificationOutboxService notificationOutboxService;

  public NotificationEventPublisher(
      NotificationService notificationService,
      NotificationOutboxService notificationOutboxService) {
    this.notificationService = notificationService;
    this.notificationOutboxService = notificationOutboxService;
  }

  public void teamDispatchChanged(
      String eventType,
      Long bizId,
      String recordNo,
      LocalDate businessDate,
      Long organizationId,
      List<Long> recipientUserIds,
      Long creatorUserId,
      Long operatorId,
      String eventSuffix) {
    notificationOutboxService.enqueue(
        new DomainNotificationEvent(
            "team-dispatch:" + eventType + ":" + bizId + ":" + eventSuffix,
            eventType,
            "THREE_CHECK_TEAM_DISPATCH",
            bizId,
            organizationId,
            recipientUserIds == null ? List.of() : recipientUserIds,
            List.of(),
            creatorUserId,
            operatorId,
            businessDate == null ? null : businessDate.atTime(LocalTime.MAX),
            Map.of(
                "summary",
                context(recordNo, businessDate))));
  }

  public void hazardSourceReviewRequested(
      Long bizId,
      String recordNo,
      LocalDate businessDate,
      Long organizationId,
      Long creatorUserId) {
    notificationOutboxService.enqueue(
        new DomainNotificationEvent(
            "hazard-source:review:" + bizId,
            "HAZARD_SOURCE_REVIEW_REQUESTED",
            "THREE_CHECK_QUICK_SHOT",
            bizId,
            organizationId,
            List.of(),
            List.of(creatorUserId),
            creatorUserId,
            creatorUserId,
            null,
            Map.of("summary", context(recordNo, businessDate))));
  }

  public void keySiteInspectionAssigned(
      Long bizId,
      String recordNo,
      LocalDate businessDate,
      Long organizationId,
      Long recipientUserId,
      Long creatorUserId,
      Long operatorUserId) {
    notificationOutboxService.enqueue(
        new DomainNotificationEvent(
            "key-site:assigned:" + bizId,
            "KEY_SITE_INSPECTION_ASSIGNED",
            "THREE_CHECK_KEY_SITES",
            bizId,
            organizationId,
            recipientUserId == null ? List.of() : List.of(recipientUserId),
            List.of(operatorUserId),
            creatorUserId,
            operatorUserId,
            businessDate == null ? null : businessDate.atTime(LocalTime.MAX),
            Map.of("summary", context(recordNo, businessDate))));
  }

  public void pointsChanged(
      Long bizId,
      String recordNo,
      LocalDate businessDate,
      Long organizationId,
      Long recipientUserId,
      Long creatorUserId,
      Long operatorUserId) {
    notificationOutboxService.enqueue(
        new DomainNotificationEvent(
            "safety-points:changed:" + bizId,
            "POINTS_CHANGED",
            "SAFETY_POINTS_FLOW",
            bizId,
            organizationId,
            recipientUserId == null ? List.of() : List.of(recipientUserId),
            List.of(),
            creatorUserId,
            operatorUserId,
            null,
            Map.of("summary", context(recordNo, businessDate))));
  }

  public void threeCheckAssigned(
      String moduleKey,
      String bizType,
      Long bizId,
      String recordNo,
      LocalDate businessDate,
      Long recipientUserId,
      Long operatorId) {
    String moduleName = THREE_CHECK_NAMES.getOrDefault(moduleKey, "一班三查");
    notificationService.publishToUser(
        new NotificationCommand(
            "THREE_CHECK_ASSIGNED",
            NotificationService.GROUP_ACTION,
            moduleKey,
            moduleName + "待执行",
            context(recordNo, businessDate),
            bizType,
            bizId,
            "IMPORTANT",
            "pre-shift-meeting".equals(moduleKey) ? "OPEN_MEETING" : "OPEN_INSPECTION",
            businessDate == null ? null : businessDate.atTime(LocalTime.of(23, 59, 59)),
            "three-check:assigned:" + bizType + ":" + bizId,
            operatorId),
        recipientUserId);
  }

  public void threeCheckReminded(
      String moduleKey,
      String bizType,
      Long bizId,
      String recordNo,
      LocalDate businessDate,
      int reminderCount,
      Long recipientUserId,
      Long operatorId) {
    String moduleName = THREE_CHECK_NAMES.getOrDefault(moduleKey, "一班三查");
    notificationService.publishToUser(
        new NotificationCommand(
            "THREE_CHECK_REMINDER",
            NotificationService.GROUP_ACTION,
            moduleKey,
            moduleName + "被催办，请尽快处理",
            context(recordNo, businessDate) + " · 第 " + reminderCount + " 次催办",
            bizType,
            bizId,
            "URGENT",
            "pre-shift-meeting".equals(moduleKey) ? "OPEN_MEETING" : "OPEN_INSPECTION",
            businessDate == null ? null : businessDate.atTime(LocalTime.of(23, 59, 59)),
            "three-check:remind:" + bizType + ":" + bizId + ":" + reminderCount,
            operatorId),
        recipientUserId);
  }

  public void threeCheckHandled(String bizType, Long bizId, Long ownerUserId) {
    notificationService.markBusinessHandled(bizType, bizId, ownerUserId);
  }

  public void hazardChanged(HazardRectificationOrder order, String action, Long operatorId) {
    if (order == null) return;
    switch (action) {
      case "ISSUE_RECTIFICATION" -> publishHazardAction(
          order, "HAZARD_RECTIFICATION_ASSIGNED", "隐患整改任务待处理", "RECTIFY",
          order.rectificationResponsibleUserId, "IMPORTANT", operatorId, "issue");
      case "REQUEST_ACCEPTANCE" -> {
        notificationService.markBusinessHandled(
            "HAZARD_RECTIFICATION_ORDER", order.id, order.rectificationResponsibleUserId);
        publishHazardAction(
            order, "HAZARD_ACCEPTANCE_REQUESTED", "隐患整改待你验收", "ACCEPT",
            order.acceptanceUserId, "IMPORTANT", operatorId, "acceptance");
      }
      case "ACCEPT" -> {
        notificationService.markBusinessHandled(
            "HAZARD_RECTIFICATION_ORDER", order.id, order.acceptanceUserId);
        publishHazardResult(order, "隐患整改已验收闭环", order.rectificationResponsibleUserId, operatorId, "accepted");
      }
      case "REJECT_ACCEPTANCE" -> {
        notificationService.markBusinessHandled(
            "HAZARD_RECTIFICATION_ORDER", order.id, order.acceptanceUserId);
        publishHazardAction(
            order, "HAZARD_ACCEPTANCE_REJECTED", "隐患验收未通过，请重新整改", "RECTIFY",
            order.rectificationResponsibleUserId, "URGENT", operatorId, "rejected");
      }
      case "CANCEL" -> {
        notificationService.markBusinessHandled(
            "HAZARD_RECTIFICATION_ORDER", order.id, order.rectificationResponsibleUserId);
        notificationService.markBusinessHandled(
            "HAZARD_RECTIFICATION_ORDER", order.id, order.acceptanceUserId);
        publishHazardResult(order, "隐患整改工单已作废", order.rectificationResponsibleUserId, operatorId, "cancelled");
      }
      default -> {
        // MARK_RECTIFIED only records the actor's progress; acceptance starts on REQUEST_ACCEPTANCE.
      }
    }
  }

  public void specialWorkChanged(
      SpecialWorkRecord record, String previousStatus, Long operatorId) {
    if (record == null || record.createdBy == null || record.status == null
        || record.status.equals(previousStatus)) {
      return;
    }
    SpecialWorkStatus status = SpecialWorkStatus.parse(record.status);
    notificationService.publishToUser(
        new NotificationCommand(
            "SPECIAL_WORK_STATUS_CHANGED",
            NotificationService.GROUP_BUSINESS,
            "special-work",
            "特殊作业状态更新：" + status.label(),
            record.project + " · " + record.workType,
            "SPECIAL_WORK",
            record.id,
            status == SpecialWorkStatus.PENDING_APPROVAL ? "IMPORTANT" : "NORMAL",
            "VIEW",
            record.implementationEndTime,
            "special-work:status:" + record.id + ":" + record.status,
            operatorId),
        record.createdBy);
  }

  private void publishHazardAction(
      HazardRectificationOrder order,
      String eventType,
      String title,
      String actionKey,
      Long recipient,
      String severity,
      Long operatorId,
      String suffix) {
    notificationService.publishToUser(
        new NotificationCommand(
            eventType,
            NotificationService.GROUP_ACTION,
            "hazard-rectification",
            title,
            hazardSummary(order),
            "HAZARD_RECTIFICATION_ORDER",
            order.id,
            severity,
            actionKey,
            order.rectificationDeadline,
            "hazard:" + order.id + ":" + suffix + ":" + order.version,
            operatorId),
        recipient);
  }

  private void publishHazardResult(
      HazardRectificationOrder order,
      String title,
      Long recipient,
      Long operatorId,
      String suffix) {
    notificationService.publishToUser(
        new NotificationCommand(
            "HAZARD_STATUS_CHANGED",
            NotificationService.GROUP_BUSINESS,
            "hazard-rectification",
            title,
            hazardSummary(order),
            "HAZARD_RECTIFICATION_ORDER",
            order.id,
            "NORMAL",
            "VIEW",
            order.rectificationDeadline,
            "hazard:" + order.id + ":" + suffix + ":" + order.version,
            operatorId),
        recipient);
  }

  private String hazardSummary(HazardRectificationOrder order) {
    return order.orderNo + " · 共 " + (order.hazardCount == null ? 0 : order.hazardCount) + " 项隐患";
  }

  private String context(String recordNo, LocalDate businessDate) {
    String date = businessDate == null ? "" : businessDate.toString();
    if (recordNo == null || recordNo.isBlank()) return date;
    if (date.isBlank()) return recordNo;
    return date + " · " + recordNo;
  }
}
