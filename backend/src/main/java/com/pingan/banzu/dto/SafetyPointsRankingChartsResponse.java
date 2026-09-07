package com.pingan.banzu.dto;

import java.util.List;

public record SafetyPointsRankingChartsResponse(
    List<TrendPoint> trend, List<SourceSlice> sources, List<TeamTopItem> teamTop5) {

  public record TrendPoint(String date, int score) {}

  public record SourceSlice(String name, int value) {}

  public record TeamTopItem(String name, int score) {}
}
