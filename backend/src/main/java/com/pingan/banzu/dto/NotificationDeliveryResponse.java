package com.pingan.banzu.dto;

import java.time.LocalDateTime;

public record NotificationDeliveryResponse(
    String notificationId,
    String announcementId,
    String recipientUserId,
    String recipientName,
    String organizationName,
    String recipientReason,
    boolean read,
    LocalDateTime readAt,
    LocalDateTime deliveredAt) {}

