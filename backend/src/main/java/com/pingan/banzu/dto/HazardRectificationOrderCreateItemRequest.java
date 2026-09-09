package com.pingan.banzu.dto;

public record HazardRectificationOrderCreateItemRequest(
    String riskType,
    String checkItem,
    String hazardDescription,
    String beforePhoto,
    String beforeVideo,
    String defaultFollowUpPlan) {}
