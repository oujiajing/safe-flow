package com.pingan.banzu.dto;

import java.util.Map;

public record HazardRectificationOrderActionRequest(
    String action, Map<String, Object> payload, String remark, Integer version) {}
