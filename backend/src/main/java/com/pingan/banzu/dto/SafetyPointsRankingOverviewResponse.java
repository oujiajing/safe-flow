package com.pingan.banzu.dto;

public record SafetyPointsRankingOverviewResponse(
    int currentTotalScore,
    int participantCount,
    int monthlyAddScore,
    int monthlyRedeemScore) {}
