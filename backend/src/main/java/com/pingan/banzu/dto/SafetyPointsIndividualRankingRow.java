package com.pingan.banzu.dto;

public record SafetyPointsIndividualRankingRow(
    int rank,
    String userName,
    String company,
    String department,
    String team,
    int currentScore,
    int addScore,
    int deductScore,
    int redeemScore,
    int recordCount,
    String lastBusinessDate) {}
