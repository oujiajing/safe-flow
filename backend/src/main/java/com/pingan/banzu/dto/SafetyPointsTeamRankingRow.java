package com.pingan.banzu.dto;

public record SafetyPointsTeamRankingRow(
    int rank,
    SafetyPointsRankBy rankBy,
    String rankId,
    String rankName,
    int currentScore,
    int addScore,
    int deductScore,
    int redeemScore,
    int memberCount,
    int recordCount,
    String lastBusinessDate) {}
