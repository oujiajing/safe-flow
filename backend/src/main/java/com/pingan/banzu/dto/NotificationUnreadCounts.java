package com.pingan.banzu.dto;

import java.util.Map;

public record NotificationUnreadCounts(
    long pending,
    long unread,
    long overdue,
    Map<String, Long> groupUnreadCounts,
    Map<String, Long> moduleUnreadCounts,
    Map<String, String> visibleCategories) {}
