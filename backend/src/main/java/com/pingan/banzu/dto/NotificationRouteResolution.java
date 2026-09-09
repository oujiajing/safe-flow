package com.pingan.banzu.dto;

import java.util.Map;

public record NotificationRouteResolution(
    boolean actionAvailable,
    String actionUnavailableReason,
    String routeKey,
    Map<String, String> routeParams) {}

