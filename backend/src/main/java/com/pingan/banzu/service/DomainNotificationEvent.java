package com.pingan.banzu.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record DomainNotificationEvent(
    String sourceEventId,
    String eventType,
    String bizType,
    Long bizId,
    Long organizationId,
    List<Long> responsibleUserIds,
    List<Long> excludedUserIds,
    Long creatorUserId,
    Long operatorUserId,
    LocalDateTime deadline,
    Map<String, Object> snapshot) {}

