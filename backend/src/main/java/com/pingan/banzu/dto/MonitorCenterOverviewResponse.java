package com.pingan.banzu.dto;

import java.time.LocalDate;
import java.util.List;

public record MonitorCenterOverviewResponse(
    LocalDate dateStart,
    LocalDate dateEnd,
    List<CompanyOption> companies,
    List<ScopeOption> scopeOptions,
    Summary summary,
    List<BarItem> dispatchBars,
    List<BarItem> dispatchScopeBars,
    List<BarItem> dispatchFinishedBars,
    List<SeriesItem> threeCheckSeries,
    List<RiskBarItem> riskControlBars,
    List<BarItem> learningBars,
    List<BarItem> learningTrend,
    List<DispatchTrendItem> dispatchTrend,
    List<BarItem> hazardStatusBars,
    List<BarItem> learningCategoryBars,
    List<BarItem> riskAccidentTypeBars,
    List<BarItem> riskCauseBars,
    List<BarItem> specialWorkTypeBars,
    List<HazardRecordItem> hazardRecordRows,
    List<PieItem> hazardRectificationPie,
    HazardRectificationOrderStats hazardRectificationOrders,
    LegacyHazardRectificationRecordStats legacyHazardRectificationRecords,
    List<HazardBarItem> hazardBars,
    TeamAnalysis teamAnalysis) {

  public record CompanyOption(Long id, String name) {}

  public record ScopeOption(Long id, String name, String orgType, List<ScopeOption> children) {}

  public record Summary(
      long teamCount,
      long personCount,
      long dispatchTotal,
      long dispatchFinished,
      long riskTotal,
      long learningTotal,
      long learningToday,
      long learningCompletedCount,
      long learningExpectedCount,
      String learningCompletionRate,
      long hazardTotal,
      long hazardDone,
      long hazardOpen,
      String hazardFixedRate,
      ThreeCheckRates threeCheckRates) {}

  public record ThreeCheckRates(String pre, String preInspection, String mid, String post) {}

  public record BarItem(String name, long value) {}

  public record DispatchTrendItem(String name, long total, long finished) {}

  public record SeriesItem(String name, List<CompanyOption> companies, List<Long> data) {}

  public record RiskBarItem(String name, long major, long serious, long normal, long low) {}

  public record PieItem(String name, long value) {}

  public record HazardRectificationOrderStats(long total, long closed, long activeOpen, long cancelled) {}

  public record LegacyHazardRectificationRecordStats(long total) {}

  public record HazardBarItem(String name, long done, long open) {}

  public record HazardRecordItem(
      String date,
      String company,
      String team,
      String type,
      String detail,
      String status,
      String statusTone,
      String sourceModule) {}

  public record TeamAnalysis(
      TeamAnalysisSummary summary,
      MetricRankings metricRankings,
      List<HeatmapRow> heatmapRows,
      List<TeamDetailRow> detailRows) {}

  public record TeamAnalysisSummary(
      long teamCount,
      long normalTeams,
      long abnormalTeams,
      long unfinishedThreeChecks,
      long openHazards,
      String averageCompletionRate) {}

  public record MetricRankings(
      List<MetricRankingItem> threeCheckTopTeams, List<MetricRankingItem> openHazardTopTeams) {}

  public record MetricRankingItem(String name, Object value) {}

  public record HeatmapRow(String name, List<Long> values) {}

  public record TeamDetailRow(
      Long teamId,
      long rank,
      String name,
      String company,
      String workshop,
      String captain,
      String safety,
      String status,
      String dispatchCompletionRate,
      String preMeeting,
      String preCheck,
      String mid,
      String post,
      String threeCheckCompletionRate,
      long hazardTotal,
      long openHazards,
      long companyLearningCount,
      long unfinishedThreeChecks,
      long abnormalItems) {}
}
