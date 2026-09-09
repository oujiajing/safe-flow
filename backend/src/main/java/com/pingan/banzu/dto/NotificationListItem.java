package com.pingan.banzu.dto;

import java.time.LocalDateTime;

public record NotificationListItem(
    String id,
    String eventType,
    String groupType,
    String moduleKey,
    String moduleName,
    String title,
    String summary,
    String teamName,
    String responsibleName,
    String bizType,
    String bizId,
    String severity,
    String actionKey,
    String actionLabel,
    String category,
    String actionStatus,
    boolean actionAvailable,
    String actionUnavailableReason,
    String recipientReason,
    String routeKey,
    java.util.Map<String, String> routeParams,
    String organizationName,
    LocalDateTime expiresAt,
    LocalDateTime deadline,
    LocalDateTime createdAt,
    boolean read,
    String handlingStatus,
    boolean overdue) {}
