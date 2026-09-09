package com.pingan.banzu.service;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationPermissionPolicy {

  private static final Map<String, String> ACTION_PERMISSIONS =
      Map.ofEntries(
          Map.entry("SPECIAL_WORK_APPROVAL_REQUESTED", "PINGAN_SPECIAL_WORK_APPROVE"),
          Map.entry("SPECIAL_WORK_ACCEPTANCE_REQUESTED", "PINGAN_SPECIAL_WORK_REVIEW"),
          Map.entry("HAZARD_SOURCE_REVIEW_REQUESTED", "PINGAN_HAZARD_QUICK_SHOT_REVIEW"),
          Map.entry("HAZARD_RECTIFICATION_OVERDUE", "PINGAN_NOTIFICATION_BUSINESS_WARNING"),
          Map.entry("EXAM_OVERDUE", "PINGAN_NOTIFICATION_BUSINESS_WARNING"),
          Map.entry("LEARNING_ASSIGNED", "PINGAN_TRAINING_SAFETY_LEARNING_VIEW"),
          Map.entry("RISK_CREATED", "PINGAN_RISK_VIEW"),
          Map.entry("RISK_LEVEL_CHANGED", "PINGAN_RISK_VIEW"),
          Map.entry("RISK_LIBRARY_REVIEW_REQUESTED", "PINGAN_RISK_MANAGE"),
          Map.entry("LEDGER_PUBLISHED", "PINGAN_LEDGER_VIEW"));

  public String requiredPermission(String eventType) {
    return ACTION_PERMISSIONS.get(eventType);
  }
}
