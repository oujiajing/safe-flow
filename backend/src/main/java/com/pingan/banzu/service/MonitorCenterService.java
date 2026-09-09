package com.pingan.banzu.service;

import com.pingan.banzu.common.ThreeCheckStatus;
import com.pingan.banzu.dto.MonitorCenterOverviewResponse;
import com.pingan.banzu.dto.MonitorCenterOverviewResponse.BarItem;
import com.pingan.banzu.dto.MonitorCenterOverviewResponse.CompanyOption;
import com.pingan.banzu.dto.MonitorCenterOverviewResponse.DispatchTrendItem;
import com.pingan.banzu.dto.MonitorCenterOverviewResponse.HazardRecordItem;
import com.pingan.banzu.dto.MonitorCenterOverviewResponse.HazardBarItem;
import com.pingan.banzu.dto.MonitorCenterOverviewResponse.HazardRectificationOrderStats;
import com.pingan.banzu.dto.MonitorCenterOverviewResponse.LegacyHazardRectificationRecordStats;
import com.pingan.banzu.dto.MonitorCenterOverviewResponse.PieItem;
import com.pingan.banzu.dto.MonitorCenterOverviewResponse.RiskBarItem;
import com.pingan.banzu.dto.MonitorCenterOverviewResponse.SeriesItem;
import com.pingan.banzu.dto.MonitorCenterOverviewResponse.ScopeOption;
import com.pingan.banzu.dto.MonitorCenterOverviewResponse.Summary;
import com.pingan.banzu.dto.MonitorCenterOverviewResponse.HeatmapRow;
import com.pingan.banzu.dto.MonitorCenterOverviewResponse.MetricRankingItem;
import com.pingan.banzu.dto.MonitorCenterOverviewResponse.MetricRankings;
import com.pingan.banzu.dto.MonitorCenterOverviewResponse.TeamAnalysis;
import com.pingan.banzu.dto.MonitorCenterOverviewResponse.TeamAnalysisSummary;
import com.pingan.banzu.dto.MonitorCenterOverviewResponse.TeamDetailRow;
import com.pingan.banzu.dto.MonitorCenterOverviewResponse.ThreeCheckRates;
import com.pingan.banzu.dto.MonitorCenterQuery;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import com.pingan.banzu.system.security.SystemDataScopeService;
import com.pingan.banzu.system.security.SystemPermissionService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class MonitorCenterService {

  private static final String PERMISSION_MONITOR_CENTER_ENTRY = "PINGAN_MONITOR_CENTER_ENTRY";
  private static final String PERMISSION_MONITOR_CENTER_VIEW = "PINGAN_MONITOR_CENTER_VIEW";
  private static final String PERMISSION_MONITOR_CENTER_GLOBAL_VIEW =
      "PINGAN_MONITOR_CENTER_GLOBAL_VIEW";
  private static final List<CompanyOption> APPROVED_COMPANIES =
      List.of(new CompanyOption(100000000000L, "演示公司"));
  private static final Map<Long, String> APPROVED_COMPANY_NAMES =
      APPROVED_COMPANIES.stream().collect(Collectors.toMap(CompanyOption::id, CompanyOption::name));

  private static final List<String> DISPATCH_MODULES =
      List.of("team-dispatch", "curtain-wall-team-dispatch");
  private static final List<String> DASHBOARD_THREE_CHECK_MODULES =
      List.of(
          "team-dispatch",
          "curtain-wall-team-dispatch",
          "pre-shift-meeting",
          "pre-shift-inspection",
          "mid-shift-inspection",
          "post-shift-inspection");
  private static final List<String> FINISHED_STATUSES =
      List.of(ThreeCheckStatus.OPENED, ThreeCheckStatus.ARCHIVED);

  private final JdbcTemplate jdbcTemplate;
  private final SystemDataScopeService dataScopeService;
  private final SystemPermissionService permissionService;
  private final ThreeCheckAccessPolicyService accessPolicyService;

  public MonitorCenterService(
      JdbcTemplate jdbcTemplate,
      SystemDataScopeService dataScopeService,
      SystemPermissionService permissionService,
      ThreeCheckAccessPolicyService accessPolicyService) {
    this.jdbcTemplate = jdbcTemplate;
    this.dataScopeService = dataScopeService;
    this.permissionService = permissionService;
    this.accessPolicyService = accessPolicyService;
  }

  public MonitorCenterOverviewResponse overview(MonitorCenterQuery query) {
    permissionService.assertHasPermission(PERMISSION_MONITOR_CENTER_ENTRY);
    permissionService.assertHasPermission(PERMISSION_MONITOR_CENTER_VIEW);
    assertCanViewGlobalOverview(query);

    LocalDate today = LocalDate.now();
    LocalDate start = query == null || query.dateStart() == null ? today : query.dateStart();
    LocalDate end = query == null || query.dateEnd() == null ? start : query.dateEnd();
    if (start.isAfter(end)) {
      LocalDate swapped = start;
      start = end;
      end = swapped;
    }

    List<CompanyOption> companyOptions = visibleApprovedCompanies();
    List<OrgOption> orgOptions = visibleScopeOrgs(companyOptions);
    Map<Long, OrgOption> orgById =
        orgOptions.stream().collect(Collectors.toMap(OrgOption::id, org -> org, (left, right) -> left));
    List<ScopeOption> scopeOptions = scopeTree(orgOptions);
    if (companyOptions.isEmpty()) {
      return emptyResponse(start, end, List.of(), List.of());
    }

    ScopeSelection selection = selectedScope(companyOptions, orgById, query);
    if (selection.dimensions().isEmpty() || selection.companyIds().isEmpty()) {
      return emptyResponse(start, end, companyOptions, scopeOptions);
    }

    List<CompanyOption> dimensions = selection.dimensions();
    ThreeCheckAggregation threeChecks = aggregateThreeChecks(selection, start, end);
    Map<Long, Long> dispatchCounts = threeChecks.counts(DISPATCH_MODULES, null);
    Map<Long, Long> dispatchFinishedCounts =
        threeChecks.counts(DISPATCH_MODULES, FINISHED_STATUSES);
    Map<Long, Long> preMeetingCounts =
        threeChecks.counts(List.of("pre-shift-meeting"), FINISHED_STATUSES);
    Map<Long, Long> preInspectionCounts =
        threeChecks.counts(List.of("pre-shift-inspection"), FINISHED_STATUSES);
    Map<Long, Long> midInspectionCounts =
        threeChecks.counts(List.of("mid-shift-inspection"), FINISHED_STATUSES);
    Map<Long, Long> postInspectionCounts =
        threeChecks.counts(List.of("post-shift-inspection"), FINISHED_STATUSES);

    HazardAggregation hazards = aggregateHazards(selection, start, end);
    Map<Long, HazardCounts> hazardCounts = hazards.byDimension();
    RiskAggregation risks = aggregateRisks(selection);
    Map<Long, RiskCounts> riskCounts = risks.forDimensions(selection);
    LearningAggregation learning =
        aggregateLearning(selection.companyIds(), earlier(start, end.minusDays(6)), end);
    Map<Long, Long> learningCounts =
        learning.forDimensions(selection, start, end);
    Map<Long, Long> learningTodayCounts =
        learning.forDimensions(selection, end, end);
    List<BarItem> riskAccidentTypeBars = risks.accidentTypeBars();
    List<BarItem> riskCauseBars = risks.causeBars();
    List<BarItem> specialWorkTypeBars = countSpecialWorkTypeBars(selection, start, end);
    List<HazardRecordItem> hazardRecordRows = recentHazardRecords(selection, start, end);
    List<DispatchTrendItem> dispatchTrend = dispatchTrend(selection, end.minusDays(6), end);
    ScopeSelection dispatchChartSelection = dispatchChartSelection(selection, orgById, query);
    ThreeCheckAggregation dispatchChartChecks =
        dispatchChartSelection.equals(selection)
            ? threeChecks
            : aggregateThreeChecks(dispatchChartSelection, start, end);
    Map<Long, Long> dispatchScopeCounts =
        dispatchChartChecks.counts(DISPATCH_MODULES, null);
    Map<Long, Long> dispatchScopeFinishedCounts =
        dispatchChartChecks.counts(DISPATCH_MODULES, FINISHED_STATUSES);
    List<BarItem> dispatchScopeBars =
        bars(dispatchChartSelection.dimensions(), dispatchScopeCounts);
    List<BarItem> dispatchScopeFinishedBars =
        bars(dispatchChartSelection.dimensions(), dispatchScopeFinishedCounts);
    List<BarItem> hazardStatusBars = hazards.statusBars();
    List<BarItem> learningCategoryBars = learning.categoryBars(start, end);
    TeamAnalysis teamAnalysis =
        teamAnalysis(selection, orgById, start, end, learning.companyCounts(start, end));

    long dispatchTotal = sum(dispatchCounts);
    long dispatchFinished = sum(dispatchFinishedCounts);
    long personCount = countPersonnel(selection);
    long riskTotal = riskCounts.values().stream().mapToLong(RiskCounts::total).sum();
    long learningTotal = sum(learningCounts);
    long learningToday = sum(learningTodayCounts);
    long learningCompletedCount = countLearningCompletions(selection, start, end);
    long learningExpectedCount = countExpectedLearningCompletions(selection, start, end);
    long hazardDone = hazardCounts.values().stream().mapToLong(HazardCounts::done).sum();
    long hazardOpen = hazardCounts.values().stream().mapToLong(HazardCounts::open).sum();
    long hazardCancelled = hazardCounts.values().stream().mapToLong(HazardCounts::cancelled).sum();
    long hazardTotal = hazardDone + hazardOpen;
    HazardRectificationOrderStats hazardRectificationOrders =
        new HazardRectificationOrderStats(hazardTotal + hazardCancelled, hazardDone, hazardOpen, hazardCancelled);
    LegacyHazardRectificationRecordStats legacyHazardRectificationRecords =
        new LegacyHazardRectificationRecordStats(countLegacyHazardRecords(selection, start, end));

    Summary summary =
        new Summary(
            countTeams(selection, orgById),
            personCount,
            dispatchTotal,
            dispatchFinished,
            riskTotal,
            learningTotal,
            learningToday,
            learningCompletedCount,
            learningExpectedCount,
            percent(learningCompletedCount, learningExpectedCount),
            hazardTotal,
            hazardDone,
            hazardOpen,
            percent(hazardDone, hazardTotal),
            new ThreeCheckRates(
                percentCapped(sum(preMeetingCounts), dispatchFinished),
                percentCapped(sum(preInspectionCounts), dispatchFinished),
                percentCapped(sum(midInspectionCounts), dispatchFinished),
                percentCapped(sum(postInspectionCounts), dispatchFinished)));

    return new MonitorCenterOverviewResponse(
        start,
        end,
        companyOptions,
        scopeOptions,
        summary,
        dispatchScopeBars,
        dispatchScopeBars,
        dispatchScopeFinishedBars,
        List.of(
            series("班前会", dimensions, preMeetingCounts),
            series("班前检查", dimensions, preInspectionCounts),
            series("班中检查", dimensions, midInspectionCounts),
            series("班后检查", dimensions, postInspectionCounts)),
        riskBars(dimensions, riskCounts),
        bars(dimensions, learningCounts),
        learning.trend(end.minusDays(6), end),
        dispatchTrend,
        hazardStatusBars,
        learningCategoryBars,
        riskAccidentTypeBars,
        riskCauseBars,
        specialWorkTypeBars,
        hazardRecordRows,
        List.of(new PieItem("已整改隐患", hazardDone), new PieItem("未整改隐患", hazardOpen), new PieItem("已作废", hazardCancelled)),
        hazardRectificationOrders,
        legacyHazardRectificationRecords,
        hazardBars(dimensions, hazardCounts),
        teamAnalysis);
  }

  private MonitorCenterOverviewResponse emptyResponse(
      LocalDate start,
      LocalDate end,
      List<CompanyOption> companyOptions,
      List<ScopeOption> scopeOptions) {
    Summary summary =
        new Summary(
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            "0.00%",
            0,
            0,
            0,
            "0.00%",
            new ThreeCheckRates("0.00%", "0.00%", "0.00%", "0.00%"));
    return new MonitorCenterOverviewResponse(
        start,
        end,
        companyOptions,
        scopeOptions,
        summary,
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        new HazardRectificationOrderStats(0, 0, 0, 0),
        new LegacyHazardRectificationRecordStats(0),
        List.of(),
        emptyTeamAnalysis());
  }

  private List<CompanyOption> visibleApprovedCompanies() {
    CurrentUser user = CurrentUserContext.require();
    String dataScope = dataScopeService.currentDataScope();
    boolean curtainWallOnly = accessPolicyService.shouldApplyCurtainWallNarrowing(user, dataScope);
    if ("ALL".equals(dataScope)) {
      return filterCurtainWallCompanies(APPROVED_COMPANIES, curtainWallOnly);
    }
    List<Long> accessibleOrgIds = dataScopeService.accessibleOrgIds();
    List<OrgOption> allOrgs = activeOrgOptions();
    Map<Long, OrgOption> orgById =
        allOrgs.stream().collect(Collectors.toMap(OrgOption::id, org -> org, (left, right) -> left));
    List<CompanyOption> companies = APPROVED_COMPANIES.stream()
        .filter(company ->
            accessibleOrgIds.contains(company.id())
                || accessibleOrgIds.stream()
                    .map(orgById::get)
                    .filter(Objects::nonNull)
                    .anyMatch(org -> company.id().equals(ancestorCompanyId(org, orgById))))
        .toList();
    return filterCurtainWallCompanies(companies, curtainWallOnly);
  }

  private void assertCanViewGlobalOverview(MonitorCenterQuery query) {
    Long orgId = query == null ? null : query.orgId();
    if (orgId == null && "ALL".equals(dataScopeService.currentDataScope())) {
      permissionService.assertHasPermission(PERMISSION_MONITOR_CENTER_GLOBAL_VIEW);
    }
  }

  private List<CompanyOption> filterCurtainWallCompanies(
      List<CompanyOption> companies, boolean curtainWallOnly) {
    if (!curtainWallOnly) {
      return companies;
    }
    return companies.stream().filter(company -> company.name().contains("幕墙")).toList();
  }

  private TeamAnalysis teamAnalysis(
      ScopeSelection selection,
      Map<Long, OrgOption> orgById,
      LocalDate start,
      LocalDate end,
      Map<Long, Long> learningByCompany) {
    List<TeamDimension> teams = teamDimensions(selection, orgById);
    if (teams.isEmpty()) {
      return emptyTeamAnalysis();
    }

    List<Long> teamIds = teams.stream().map(TeamDimension::id).toList();
    ThreeCheckAggregation threeChecks =
        aggregateTeamThreeChecks(selection, teamIds, start, end);
    Map<Long, Long> dispatchCounts = threeChecks.counts(DISPATCH_MODULES, null);
    Map<Long, Long> dispatchFinishedCounts =
        threeChecks.counts(DISPATCH_MODULES, FINISHED_STATUSES);
    Map<Long, Long> preMeetingCounts =
        threeChecks.counts(List.of("pre-shift-meeting"), FINISHED_STATUSES);
    Map<Long, Long> preInspectionCounts =
        threeChecks.counts(List.of("pre-shift-inspection"), FINISHED_STATUSES);
    Map<Long, Long> midInspectionCounts =
        threeChecks.counts(List.of("mid-shift-inspection"), FINISHED_STATUSES);
    Map<Long, Long> postInspectionCounts =
        threeChecks.counts(List.of("post-shift-inspection"), FINISHED_STATUSES);
    Map<Long, HazardCounts> hazardCounts = countTeamHazards(selection, teamIds, start, end);

    List<TeamMetric> metrics =
        teams.stream()
            .map(team -> {
              long dispatchTotal = dispatchCounts.getOrDefault(team.id(), 0L);
              long dispatchFinished = dispatchFinishedCounts.getOrDefault(team.id(), 0L);
              long preMeeting = preMeetingCounts.getOrDefault(team.id(), 0L);
              long preInspection = preInspectionCounts.getOrDefault(team.id(), 0L);
              long midInspection = midInspectionCounts.getOrDefault(team.id(), 0L);
              long postInspection = postInspectionCounts.getOrDefault(team.id(), 0L);
              HazardCounts hazards = hazardCounts.getOrDefault(team.id(), new HazardCounts(0, 0, 0));
              long unfinishedThreeChecks =
                  Math.max(dispatchFinished * 4 - preMeeting - preInspection - midInspection - postInspection, 0);
              long completedUnits =
                  dispatchFinished
                      + Math.min(preMeeting, dispatchFinished)
                      + Math.min(preInspection, dispatchFinished)
                      + Math.min(midInspection, dispatchFinished)
                      + Math.min(postInspection, dispatchFinished);
              long expectedUnits = dispatchFinished * 5;
              long companyLearningCount = learningByCompany.getOrDefault(team.companyId(), 0L);
              return new TeamMetric(
                  team,
                  dispatchTotal,
                  dispatchFinished,
                  preMeeting,
                  preInspection,
                  midInspection,
                  postInspection,
                  hazards.done(),
                  hazards.open(),
                  unfinishedThreeChecks,
                  completedUnits,
                  expectedUnits,
                  companyLearningCount);
            })
            .toList();

    long normalTeams = metrics.stream().filter(TeamMetric::normal).count();
    long abnormalTeams = metrics.size() - normalTeams;
    long unfinishedThreeChecks = metrics.stream().mapToLong(TeamMetric::unfinishedThreeChecks).sum();
    long openHazards = metrics.stream().mapToLong(TeamMetric::openHazards).sum();
    long completedUnits = metrics.stream().mapToLong(TeamMetric::completedUnits).sum();
    long expectedUnits = metrics.stream().mapToLong(TeamMetric::expectedUnits).sum();

    TeamAnalysisSummary summary =
        new TeamAnalysisSummary(
            metrics.size(),
            normalTeams,
            abnormalTeams,
            unfinishedThreeChecks,
            openHazards,
            percent(completedUnits, expectedUnits));

    List<TeamDetailRow> detailRows =
        metrics.stream()
            .sorted(
                Comparator.comparing(TeamMetric::normal)
                    .thenComparing(Comparator.comparingLong(TeamMetric::openHazards).reversed())
                    .thenComparing(Comparator.comparingLong(TeamMetric::unfinishedThreeChecks).reversed())
                    .thenComparing(metric -> metric.team().name()))
            .map(new RowRanker()::toRow)
            .toList();

    MetricRankings rankings =
        new MetricRankings(
            metrics.stream()
                .sorted(
                    Comparator.comparingDouble(TeamMetric::threeCheckRateValue)
                        .reversed()
                        .thenComparing(metric -> metric.team().name()))
                .limit(5)
                .map(metric -> new MetricRankingItem(metric.team().name(), metric.threeCheckCompletionRate()))
                .toList(),
            metrics.stream()
                .filter(metric -> metric.openHazards() > 0)
                .sorted(
                    Comparator.comparingLong(TeamMetric::openHazards)
                        .reversed()
                        .thenComparing(metric -> metric.team().name()))
                .limit(5)
                .map(metric -> new MetricRankingItem(metric.team().name(), metric.openHazards()))
                .toList());

    List<HeatmapRow> heatmapRows =
        metrics.stream()
            .sorted(Comparator.comparing(metric -> metric.team().name()))
            .map(metric ->
                new HeatmapRow(
                    metric.team().name(),
                    List.of(
                        anomalyRate(metric.dispatchFinished(), metric.dispatchTotal()),
                        anomalyRate(metric.preMeeting(), metric.dispatchFinished()),
                        anomalyRate(metric.preInspection(), metric.dispatchFinished()),
                        anomalyRate(metric.midInspection(), metric.dispatchFinished()),
                        anomalyRate(metric.postInspection(), metric.dispatchFinished()),
                        metric.openHazards() > 0 ? 100L : 0L,
                        metric.companyLearningCount() > 0 ? 0L : 100L)))
            .toList();

    return new TeamAnalysis(summary, rankings, heatmapRows, detailRows);
  }

  private TeamAnalysis emptyTeamAnalysis() {
    return new TeamAnalysis(
        new TeamAnalysisSummary(0, 0, 0, 0, 0, "0.00%"),
        new MetricRankings(List.of(), List.of()),
        List.of(),
        List.of());
  }

  private List<TeamDimension> teamDimensions(ScopeSelection selection, Map<Long, OrgOption> orgById) {
    Map<Long, TeamProfile> profiles = teamProfiles();
    return orgById.values().stream()
        .filter(org -> "TEAM".equals(org.orgType()))
        .filter(org -> selection.teamId() == null || selection.teamId().equals(org.id()))
        .filter(org -> selection.departmentId() == null || org.orgPath().contains("/" + selection.departmentId() + "/"))
        .map(org -> toTeamDimension(org, orgById, profiles.get(org.id())))
        .filter(Objects::nonNull)
        .filter(team -> selection.companyIds().contains(team.companyId()))
        .sorted(Comparator.comparing(TeamDimension::companyId).thenComparing(TeamDimension::id))
        .toList();
  }

  private TeamDimension toTeamDimension(OrgOption org, Map<Long, OrgOption> orgById, TeamProfile profile) {
    Long companyId = profile == null ? ancestorCompanyId(org, orgById) : profile.companyId();
    if (companyId == null) {
      return null;
    }
    OrgOption workshop = ancestorDepartment(org, orgById);
    String workshopName =
        profile != null && !profile.workshopName().isBlank()
            ? profile.workshopName()
            : workshop == null ? "" : workshop.name();
    return new TeamDimension(
        org.id(),
        org.name(),
        companyId,
        displayOrgName(companyId, orgById.get(companyId) == null ? "" : orgById.get(companyId).name()),
        workshopName,
        profile == null ? "" : profile.leader(),
        profile == null ? "" : profile.safety());
  }

  private Map<Long, TeamProfile> teamProfiles() {
    Map<Long, TeamProfile> profiles = new LinkedHashMap<>();
    jdbcTemplate
        .queryForList(
            """
            select org_id, company_org_id, workshop_name, leader_username, safety_officer_username
            from sys_team_profile
            where deleted = 0
            """)
        .forEach(row ->
            profiles.put(
                longValue(row.get("org_id")),
                new TeamProfile(
                    longValue(row.get("company_org_id")),
                    Objects.toString(row.get("workshop_name"), ""),
                    Objects.toString(row.get("leader_username"), ""),
                    Objects.toString(row.get("safety_officer_username"), ""))));
    return profiles;
  }

  private ThreeCheckAggregation aggregateTeamThreeChecks(
      ScopeSelection selection, List<Long> teamIds, LocalDate start, LocalDate end) {
    if (teamIds.isEmpty()) {
      return ThreeCheckAggregation.empty();
    }
    List<Object> params = new ArrayList<>();
    StringBuilder sql =
        new StringBuilder(
            """
            select r.team_id as dimension_id, r.module_key, r.status, count(*) as count
            from three_check_record r
            join sys_team_profile p on p.org_id = r.team_id
              and p.company_org_id = r.company_id
              and p.deleted = 0
            where r.deleted = 0
              and r.company_id in (
            """);
    appendPlaceholders(sql, params, selection.companyIds());
    sql.append(") and r.team_id in (");
    appendPlaceholders(sql, params, teamIds);
    sql.append(") and r.module_key in (");
    appendPlaceholders(sql, params, DASHBOARD_THREE_CHECK_MODULES);
    sql.append(") and r.business_date >= ? and r.business_date <= ?");
    params.add(start);
    params.add(end);
    appendAliasedLowerScopeFilter(sql, params, selection, "r");
    sql.append(" group by r.team_id, r.module_key, r.status");
    return threeCheckAggregation(sql.toString(), params);
  }

  private Map<Long, HazardCounts> countTeamHazards(
      ScopeSelection selection, List<Long> teamIds, LocalDate start, LocalDate end) {
    if (teamIds.isEmpty()) {
      return Map.of();
    }
    List<Object> params = new ArrayList<>();
    StringBuilder sql =
        new StringBuilder(
            """
            select
              o.team_id as dimension_id,
              sum(case when o.status = 'CLOSED' then 1 else 0 end) as done,
              sum(case when o.status not in ('CLOSED', 'CANCELLED') then 1 else 0 end) as open,
              sum(case when o.status = 'CANCELLED' then 1 else 0 end) as cancelled
            from hazard_rectification_order o
            where o.deleted = 0
              and o.company_id in (
            """);
    appendPlaceholders(sql, params, selection.companyIds());
    sql.append(") and o.team_id in (");
    appendPlaceholders(sql, params, teamIds);
    sql.append(") and o.business_date >= ? and o.business_date <= ?");
    params.add(start);
    params.add(end);
    appendAliasedLowerScopeFilter(sql, params, selection, "o");
    sql.append(" group by o.team_id");

    Map<Long, HazardCounts> counts = new LinkedHashMap<>();
    jdbcTemplate.queryForList(sql.toString(), params.toArray()).forEach(row -> {
      counts.put(
          longValue(row.get("dimension_id")),
          new HazardCounts(longValue(row.get("done")), longValue(row.get("open")), longValue(row.get("cancelled"))));
    });
    return counts;
  }

  private OrgOption ancestorDepartment(OrgOption org, Map<Long, OrgOption> orgById) {
    OrgOption nearestDepartment = null;
    for (String pathId : org.orgPath().split("/")) {
      if (pathId.isBlank()) {
        continue;
      }
      OrgOption pathOrg = orgById.get(Long.valueOf(pathId));
      if (pathOrg != null && "DEPARTMENT".equals(pathOrg.orgType())) {
        nearestDepartment = pathOrg;
      }
    }
    return nearestDepartment;
  }

  private long anomalyRate(long completed, long total) {
    if (total <= 0) {
      return 0;
    }
    return Math.max(Math.round((total - Math.min(completed, total)) * 100.0 / total), 0);
  }

  private ScopeSelection selectedScope(
      List<CompanyOption> companyOptions, Map<Long, OrgOption> orgById, MonitorCenterQuery query) {
    Long orgId = query == null ? null : query.orgId();
    if (orgId == null) {
      return defaultScopeSelection(companyOptions, orgById);
    }

    OrgOption selectedOrg = orgById.get(orgId);
    if (selectedOrg == null) {
      return ScopeSelection.empty();
    }

    String orgType = selectedOrg.orgType();
    if ("DEPARTMENT".equals(orgType)) {
      Long companyId = ancestorCompanyId(selectedOrg, orgById);
      if (companyId == null) {
        return ScopeSelection.empty();
      }
      return new ScopeSelection(List.of(toDimension(selectedOrg)), List.of(companyId), selectedOrg.id(), null, "department_id");
    }
    if ("TEAM".equals(orgType)) {
      Long companyId = ancestorCompanyId(selectedOrg, orgById);
      if (companyId == null) {
        return ScopeSelection.empty();
      }
      return new ScopeSelection(List.of(toDimension(selectedOrg)), List.of(companyId), null, selectedOrg.id(), "team_id");
    }
    if ("COMPANY".equals(orgType)) {
      return new ScopeSelection(List.of(toDimension(selectedOrg)), List.of(selectedOrg.id()), null, null, "company_id");
    }

    List<CompanyOption> descendants =
        companyOptions.stream()
            .filter(company -> {
              OrgOption companyOrg = orgById.get(company.id());
              return companyOrg != null && companyOrg.orgPath().startsWith(selectedOrg.orgPath());
            })
            .toList();
    return new ScopeSelection(descendants, descendants.stream().map(CompanyOption::id).toList(), null, null, "company_id");
  }

  private ScopeSelection defaultScopeSelection(
      List<CompanyOption> companyOptions, Map<Long, OrgOption> orgById) {
    List<Long> companyIds = companyOptions.stream().map(CompanyOption::id).toList();
    if ("ALL".equals(dataScopeService.currentDataScope())) {
      return new ScopeSelection(companyOptions, companyIds, null, null, "company_id");
    }

    OrgOption userOrg = orgById.get(CurrentUserContext.require().orgId());
    if (userOrg == null) {
      return new ScopeSelection(companyOptions, companyIds, null, null, "company_id");
    }
    if ("DEPARTMENT".equals(userOrg.orgType())) {
      return new ScopeSelection(companyOptions, companyIds, userOrg.id(), null, "company_id");
    }
    if ("TEAM".equals(userOrg.orgType())) {
      return new ScopeSelection(companyOptions, companyIds, null, userOrg.id(), "company_id");
    }
    return new ScopeSelection(companyOptions, companyIds, null, null, "company_id");
  }

  private ScopeSelection dispatchChartSelection(
      ScopeSelection selection, Map<Long, OrgOption> orgById, MonitorCenterQuery query) {
    if ("ALL".equals(dataScopeService.currentDataScope())
        && (query == null || query.orgId() == null)) {
      return selection;
    }

    Long anchorId = query == null ? null : query.orgId();
    if (anchorId == null) {
      anchorId = CurrentUserContext.require().orgId();
    }
    OrgOption anchor = orgById.get(anchorId);
    if (anchor == null && selection.departmentId() != null) {
      anchor = orgById.get(selection.departmentId());
    }
    if (anchor == null && selection.teamId() != null) {
      anchor = orgById.get(selection.teamId());
    }
    if (anchor == null) {
      return selection;
    }

    if ("COMPANY".equals(anchor.orgType())) {
      List<CompanyOption> departments =
          descendantDimensions(anchor, orgById, "DEPARTMENT");
      return departments.isEmpty()
          ? selection
          : new ScopeSelection(
              departments, selection.companyIds(), null, null, "department_id");
    }
    if ("DEPARTMENT".equals(anchor.orgType())) {
      List<CompanyOption> teams = descendantDimensions(anchor, orgById, "TEAM");
      return teams.isEmpty()
          ? selection
          : new ScopeSelection(
              teams, selection.companyIds(), anchor.id(), null, "team_id");
    }
    if ("TEAM".equals(anchor.orgType())) {
      return new ScopeSelection(
          List.of(toDimension(anchor)),
          selection.companyIds(),
          selection.departmentId(),
          anchor.id(),
          "team_id");
    }
    return selection;
  }

  private List<CompanyOption> descendantDimensions(
      OrgOption anchor, Map<Long, OrgOption> orgById, String orgType) {
    return orgById.values().stream()
        .filter(org -> orgType.equals(org.orgType()))
        .filter(org -> org.orgPath().startsWith(anchor.orgPath()))
        .sorted(Comparator.comparing(OrgOption::sortOrder).thenComparing(OrgOption::id))
        .map(this::toDimension)
        .toList();
  }

  private ThreeCheckAggregation aggregateThreeChecks(
      ScopeSelection selection, LocalDate start, LocalDate end) {
    List<Object> params = new ArrayList<>();
    StringBuilder sql =
        new StringBuilder(
            String.format(
                """
            select %s as dimension_id, module_key, status, count(*) as count
            from three_check_record
            where deleted = 0
              and company_id in (
            """,
                selection.groupColumn()));
    appendPlaceholders(sql, params, selection.companyIds());
    sql.append(") and module_key in (");
    appendPlaceholders(sql, params, DASHBOARD_THREE_CHECK_MODULES);
    sql.append(") and business_date >= ? and business_date <= ?");
    params.add(start);
    params.add(end);
    appendLowerScopeFilter(sql, params, selection);
    sql.append(" group by ")
        .append(selection.groupColumn())
        .append(", module_key, status");
    return threeCheckAggregation(sql.toString(), params);
  }

  private ThreeCheckAggregation threeCheckAggregation(String sql, List<Object> params) {
    List<ThreeCheckCount> counts =
        jdbcTemplate.queryForList(sql, params.toArray()).stream()
            .map(
                row ->
                    new ThreeCheckCount(
                        longValue(row.get("dimension_id")),
                        Objects.toString(row.get("module_key"), ""),
                        Objects.toString(row.get("status"), ""),
                        longValue(row.get("count"))))
            .toList();
    return new ThreeCheckAggregation(counts);
  }

  private long countPersonnel(ScopeSelection selection) {
    List<Object> params = new ArrayList<>();
    StringBuilder sql =
        new StringBuilder(
            """
            select count(distinct p.user_id)
            from sys_user_profile p
            join sys_user u on u.id = p.user_id
            where p.deleted = 0
              and u.deleted = 0
              and u.status = 'ACTIVE'
              and p.company_org_id in (
            """);
    appendPlaceholders(sql, params, selection.companyIds());
    sql.append(")");
    if (selection.departmentId() != null) {
      sql.append(" and p.department_org_id = ?");
      params.add(selection.departmentId());
    }
    if (selection.teamId() != null) {
      sql.append(" and p.team_org_id = ?");
      params.add(selection.teamId());
    }
    Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
    return count == null ? 0 : count;
  }

  private long countLearningCompletions(
      ScopeSelection selection, LocalDate start, LocalDate end) {
    List<Object> params = new ArrayList<>();
    StringBuilder sql =
        new StringBuilder(
            """
            select count(*)
            from three_check_record r
            where r.deleted = 0
              and r.module_key = 'points-flow'
              and r.source_channel = 'MINI'
              and r.source_record_id is not null
              and r.company_id in (
            """);
    appendPlaceholders(sql, params, selection.companyIds());
    sql.append(") and r.business_date >= ? and r.business_date <= ?");
    params.add(start);
    params.add(end);
    appendAliasedLowerScopeFilter(sql, params, selection, "r");
    Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
    return count == null ? 0 : count;
  }

  private long countExpectedLearningCompletions(
      ScopeSelection selection, LocalDate start, LocalDate end) {
    List<Object> params = new ArrayList<>();
    StringBuilder sql =
        new StringBuilder(
            """
            select count(*)
            from sys_user_profile p
            join sys_user u on u.id = p.user_id
            join training_safety_learning_content c
              on c.company_id = p.company_org_id
              and c.deleted = 0
              and c.status = 'ACTIVE'
              and c.learning_date >= ?
              and c.learning_date <= ?
            where p.deleted = 0
              and u.deleted = 0
              and u.status = 'ACTIVE'
              and p.company_org_id in (
            """);
    params.add(start);
    params.add(end);
    appendPlaceholders(sql, params, selection.companyIds());
    sql.append(")");
    if (selection.departmentId() != null) {
      sql.append(" and p.department_org_id = ?");
      params.add(selection.departmentId());
    }
    if (selection.teamId() != null) {
      sql.append(" and p.team_org_id = ?");
      params.add(selection.teamId());
    }
    Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
    return count == null ? 0 : count;
  }

  private List<DispatchTrendItem> dispatchTrend(
      ScopeSelection selection, LocalDate start, LocalDate end) {
    List<Object> params = new ArrayList<>();
    StringBuilder sql =
        new StringBuilder(
            """
            select business_date,
              count(*) as total,
              sum(case when status in ('OPENED', 'ARCHIVED') then 1 else 0 end) as finished
            from three_check_record
            where deleted = 0
              and company_id in (
            """);
    appendPlaceholders(sql, params, selection.companyIds());
    sql.append(") and module_key in (");
    appendPlaceholders(sql, params, DISPATCH_MODULES);
    sql.append(") and business_date >= ? and business_date <= ?");
    params.add(start);
    params.add(end);
    appendLowerScopeFilter(sql, params, selection);
    sql.append(" group by business_date order by business_date");

    Map<LocalDate, long[]> counts = new LinkedHashMap<>();
    jdbcTemplate.queryForList(sql.toString(), params.toArray()).forEach(row -> {
      LocalDate date = LocalDate.parse(Objects.toString(row.get("business_date")));
      counts.put(date, new long[] {longValue(row.get("total")), longValue(row.get("finished"))});
    });

    List<DispatchTrendItem> trend = new ArrayList<>();
    for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
      long[] values = counts.getOrDefault(date, new long[] {0, 0});
      trend.add(new DispatchTrendItem(date.toString(), values[0], values[1]));
    }
    return trend;
  }

  private RiskAggregation aggregateRisks(ScopeSelection selection) {
    List<Object> params = new ArrayList<>();
    StringBuilder sql =
        new StringBuilder(
            """
            select h.company_id, h.risk_level, h.accident_type, h.risk_influence_factors,
                   count(*) as count
            from risk_control_hazard h
            join risk_control_library l on l.id = h.library_id and l.deleted = 0
            where h.deleted = 0
              and h.company_id in (
            """);
    appendPlaceholders(sql, params, selection.companyIds());
    sql.append(
        """
        )
        group by h.company_id, h.risk_level, h.accident_type, h.risk_influence_factors
        """);
    List<RiskGroup> groups =
        jdbcTemplate.queryForList(sql.toString(), params.toArray()).stream()
            .map(
                row ->
                    new RiskGroup(
                        longValue(row.get("company_id")),
                        Objects.toString(row.get("risk_level"), "").trim(),
                        Objects.toString(row.get("accident_type"), "").trim(),
                        Objects.toString(row.get("risk_influence_factors"), "").trim(),
                        longValue(row.get("count"))))
            .toList();
    return new RiskAggregation(groups);
  }

  private LearningAggregation aggregateLearning(
      List<Long> companyIds, LocalDate start, LocalDate end) {
    List<Object> params = new ArrayList<>();
    StringBuilder sql =
        new StringBuilder(
            """
            select company_id, learning_date, coalesce(category, '') as category, count(*) as count
            from training_safety_learning_content
            where deleted = 0
              and status = 'ACTIVE'
              and company_id in (
            """);
    appendPlaceholders(sql, params, companyIds);
    sql.append(
        """
        )
          and learning_date >= ?
          and learning_date <= ?
        group by company_id, learning_date, category
        """);
    params.add(start);
    params.add(end);
    List<LearningGroup> groups =
        jdbcTemplate.queryForList(sql.toString(), params.toArray()).stream()
            .map(
                row ->
                    new LearningGroup(
                        longValue(row.get("company_id")),
                        LocalDate.parse(Objects.toString(row.get("learning_date"))),
                        Objects.toString(row.get("category"), "").trim(),
                        longValue(row.get("count"))))
            .toList();
    return new LearningAggregation(groups);
  }

  private List<BarItem> countSpecialWorkTypeBars(ScopeSelection selection, LocalDate start, LocalDate end) {
    List<Object> params = new ArrayList<>();
    StringBuilder sql =
        new StringBuilder(
            """
            select work_type as name, count(*) as count
            from special_work_record
            where deleted = 0
              and company_id in (
            """);
    appendPlaceholders(sql, params, selection.companyIds());
    sql.append(") and application_time >= ? and application_time < ? ");
    params.add(start.atStartOfDay());
    params.add(end.plusDays(1).atStartOfDay());
    sql.append(
        """
          and work_type is not null
          and trim(work_type) <> ''
        group by work_type
        order by count(*) desc, work_type
        limit 8
        """);
    return jdbcTemplate.queryForList(sql.toString(), params.toArray()).stream()
        .map(row -> new BarItem(Objects.toString(row.get("name"), ""), longValue(row.get("count"))))
        .toList();
  }

  private List<HazardRecordItem> recentHazardRecords(ScopeSelection selection, LocalDate start, LocalDate end) {
    List<Object> params = new ArrayList<>();
    StringBuilder sql =
        new StringBuilder(
            """
            select o.business_date, o.source_module_key, o.status, o.order_no, o.rectification_requirement,
              coalesce(c.org_name, '') as company_name,
              coalesce(t.org_name, '') as team_name
            from hazard_rectification_order o
            left join sys_org c on c.id = o.company_id
            left join sys_org t on t.id = o.team_id
            where o.deleted = 0
              and o.company_id in (
            """);
    appendPlaceholders(sql, params, selection.companyIds());
    sql.append(") and o.business_date >= ? and o.business_date <= ?");
    params.add(start);
    params.add(end);
    appendAliasedLowerScopeFilter(sql, params, selection, "o");
    sql.append(" order by o.business_date desc, o.id desc limit 5");
    return jdbcTemplate.queryForList(sql.toString(), params.toArray()).stream()
        .map(row -> {
          String moduleKey = Objects.toString(row.get("source_module_key"), "");
          String status = Objects.toString(row.get("status"), "");
          String detail = Objects.toString(row.get("rectification_requirement"), "");
          return new HazardRecordItem(
              Objects.toString(row.get("business_date"), ""),
              Objects.toString(row.get("company_name"), ""),
              Objects.toString(row.get("team_name"), ""),
              hazardModuleLabel(moduleKey),
              detail.isBlank() ? Objects.toString(row.get("order_no"), "") : detail,
              hazardOrderStatusLabel(status),
              statusTone(status),
              hazardModuleLabel(moduleKey));
        })
        .toList();
  }

  private String hazardModuleLabel(String moduleKey) {
    return switch (moduleKey) {
      case "safety-check" -> "安全检查";
      case "hazard-rectification" -> "隐患整改";
      case "quick-shot" -> "随手拍";
      case "manual" -> "手工工单";
      case "pre-shift-inspection", "mid-shift-inspection", "post-shift-inspection" -> "一班三查";
      default -> "隐患排查";
    };
  }

  private String hazardOrderStatusLabel(String status) {
    return switch (status) {
      case "CLOSED" -> "已闭环";
      case "CANCELLED" -> "已作废";
      case "PENDING_ASSIGN" -> "待派发";
      case "PENDING_RECTIFY" -> "待整改";
      case "RECTIFIED" -> "已整改";
      case "PENDING_ACCEPTANCE" -> "待验收";
      default -> "处理中";
    };
  }

  private String statusTone(String status) {
    if ("CLOSED".equals(status) || "ARCHIVED".equals(status) || "OPENED".equals(status)) {
      return "green";
    }
    if ("CANCELLED".equals(status)) {
      return "gray";
    }
    if ("DRAFT".equals(status) || "RECTIFIED".equals(status) || "PENDING_ACCEPTANCE".equals(status)) {
      return "amber";
    }
    return "red";
  }

  private HazardAggregation aggregateHazards(
      ScopeSelection selection, LocalDate start, LocalDate end) {
    List<Object> params = new ArrayList<>();
    String dimensionColumn = "o." + selection.groupColumn();
    StringBuilder sql =
        new StringBuilder(
            String.format(
                """
                select %s as dimension_id, o.status, count(*) as count
                from hazard_rectification_order o
                where o.deleted = 0
                  and o.company_id in (
                """,
                dimensionColumn));
    appendPlaceholders(sql, params, selection.companyIds());
    sql.append(") and o.business_date >= ? and o.business_date <= ?");
    params.add(start);
    params.add(end);
    appendAliasedLowerScopeFilter(sql, params, selection, "o");
    sql.append(" group by ").append(dimensionColumn).append(", o.status");
    List<HazardGroup> groups =
        jdbcTemplate.queryForList(sql.toString(), params.toArray()).stream()
            .map(
                row ->
                    new HazardGroup(
                        longValue(row.get("dimension_id")),
                        Objects.toString(row.get("status"), ""),
                        longValue(row.get("count"))))
            .toList();
    return new HazardAggregation(groups);
  }

  private long countLegacyHazardRecords(ScopeSelection selection, LocalDate start, LocalDate end) {
    List<Object> params = new ArrayList<>();
    StringBuilder sql =
        new StringBuilder(
            """
            select count(*) as count
            from three_check_record r
            where r.deleted = 0
              and r.module_key in ('hazard-rectification', 'quick-shot')
              and r.company_id in (
            """);
    appendPlaceholders(sql, params, selection.companyIds());
    sql.append(") and r.business_date >= ? and r.business_date <= ?");
    params.add(start);
    params.add(end);
    appendAliasedLowerScopeFilter(sql, params, selection, "r");
    Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
    return count == null ? 0 : count;
  }

  private long countTeams(ScopeSelection selection, Map<Long, OrgOption> orgById) {
    if (selection.teamId() != null) {
      OrgOption team = orgById.get(selection.teamId());
      return team != null && "TEAM".equals(team.orgType()) ? 1 : 0;
    }
    OrgOption department =
        selection.departmentId() == null ? null : orgById.get(selection.departmentId());
    return orgById.values().stream()
        .filter(org -> "TEAM".equals(org.orgType()))
        .filter(
            org ->
                department == null
                    ? selection.companyIds().contains(ancestorCompanyId(org, orgById))
                    : org.orgPath().startsWith(department.orgPath()))
        .count();
  }

  private List<BarItem> bars(List<CompanyOption> companies, Map<Long, Long> values) {
    return companies.stream()
        .map(company -> new BarItem(company.name(), values.getOrDefault(company.id(), 0L)))
        .toList();
  }

  private SeriesItem series(String name, List<CompanyOption> companies, Map<Long, Long> values) {
    return new SeriesItem(
        name,
        companies,
        companies.stream().map(company -> values.getOrDefault(company.id(), 0L)).toList());
  }

  private List<RiskBarItem> riskBars(List<CompanyOption> companies, Map<Long, RiskCounts> values) {
    return companies.stream()
        .map(company -> {
          RiskCounts counts = values.getOrDefault(company.id(), new RiskCounts());
          return new RiskBarItem(company.name(), counts.major, counts.serious, counts.normal, counts.low);
        })
        .toList();
  }

  private List<HazardBarItem> hazardBars(List<CompanyOption> companies, Map<Long, HazardCounts> values) {
    return companies.stream()
        .map(company -> {
          HazardCounts counts = values.getOrDefault(company.id(), new HazardCounts(0, 0, 0));
          return new HazardBarItem(company.name(), counts.done, counts.open);
        })
        .toList();
  }

  private Map<Long, Long> longMap(String sql, List<Object> params) {
    Map<Long, Long> counts = new LinkedHashMap<>();
    jdbcTemplate.queryForList(sql, params.toArray()).forEach(row ->
        counts.put(longValue(row.get("dimension_id")), longValue(row.get("count"))));
    return counts;
  }

  private List<OrgOption> visibleScopeOrgs(List<CompanyOption> companyOptions) {
    if (companyOptions.isEmpty()) {
      return List.of();
    }
    Set<Long> companyIds = companyOptions.stream().map(CompanyOption::id).collect(Collectors.toSet());
    List<OrgOption> allOrgs = activeOrgOptions();
    Map<Long, OrgOption> orgById =
        allOrgs.stream().collect(Collectors.toMap(OrgOption::id, org -> org, (left, right) -> left));
    List<OrgOption> companyOrgs =
        allOrgs.stream()
        .filter(org -> companyIds.contains(ancestorCompanyId(org, orgById)))
        .toList();
    if ("ALL".equals(dataScopeService.currentDataScope())) {
      return companyOrgs;
    }

    Set<Long> accessibleOrgIds = new LinkedHashSet<>(dataScopeService.accessibleOrgIds());
    List<OrgOption> accessibleOrgs =
        accessibleOrgIds.stream().map(orgById::get).filter(Objects::nonNull).toList();
    return companyOrgs.stream()
        .filter(org ->
            accessibleOrgIds.contains(org.id())
                || accessibleOrgs.stream().anyMatch(accessible -> accessible.orgPath().startsWith(org.orgPath())))
        .toList();
  }

  private List<OrgOption> activeOrgOptions() {
    return jdbcTemplate
        .queryForList(
            """
            select id, parent_id, org_name, org_type, org_path, sort_order
            from sys_org
            where deleted = 0 and status = 'ACTIVE'
            order by org_path, sort_order, id
            """)
        .stream()
        .map(row ->
            new OrgOption(
                longValue(row.get("id")),
                nullableLongValue(row.get("parent_id")),
                displayOrgName(longValue(row.get("id")), Objects.toString(row.get("org_name"), "")),
                Objects.toString(row.get("org_type"), ""),
                Objects.toString(row.get("org_path"), ""),
                longValue(row.get("sort_order"))))
        .toList();
  }

  private List<ScopeOption> scopeTree(List<OrgOption> orgOptions) {
    Set<Long> includedIds = orgOptions.stream().map(OrgOption::id).collect(Collectors.toSet());
    Map<Long, List<OrgOption>> childrenByParent =
        orgOptions.stream()
            .filter(org -> org.parentId() != null)
            .collect(Collectors.groupingBy(OrgOption::parentId, LinkedHashMap::new, Collectors.toList()));
    return orgOptions.stream()
        .filter(org -> org.parentId() == null || !includedIds.contains(org.parentId()))
        .map(org -> toScopeOption(org, childrenByParent))
        .toList();
  }

  private ScopeOption toScopeOption(OrgOption org, Map<Long, List<OrgOption>> childrenByParent) {
    return new ScopeOption(
        org.id(),
        org.name(),
        org.orgType(),
        childrenByParent.getOrDefault(org.id(), List.of()).stream()
            .map(child -> toScopeOption(child, childrenByParent))
            .toList());
  }

  private CompanyOption toDimension(OrgOption org) {
    return new CompanyOption(org.id(), org.name());
  }

  private Long ancestorCompanyId(OrgOption org, Map<Long, OrgOption> orgById) {
    String[] pathIds = org.orgPath().split("/");
    Long nearestCompanyId = null;
    for (String pathId : pathIds) {
      if (pathId.isBlank()) {
        continue;
      }
      OrgOption pathOrg = orgById.get(Long.valueOf(pathId));
      if (pathOrg != null && "COMPANY".equals(pathOrg.orgType())) {
        nearestCompanyId = pathOrg.id();
      }
    }
    return nearestCompanyId;
  }

  private void appendLowerScopeFilter(StringBuilder sql, List<Object> params, ScopeSelection selection) {
    if (selection.departmentId() != null) {
      sql.append(" and department_id = ?");
      params.add(selection.departmentId());
    }
    if (selection.teamId() != null) {
      sql.append(" and team_id = ?");
      params.add(selection.teamId());
    }
  }

  private void appendAliasedLowerScopeFilter(
      StringBuilder sql, List<Object> params, ScopeSelection selection, String alias) {
    if (selection.departmentId() != null) {
      sql.append(" and ").append(alias).append(".department_id = ?");
      params.add(selection.departmentId());
    }
    if (selection.teamId() != null) {
      sql.append(" and ").append(alias).append(".team_id = ?");
      params.add(selection.teamId());
    }
  }

  private String displayOrgName(Long id, String name) {
    return APPROVED_COMPANY_NAMES.getOrDefault(id, name);
  }

  private void appendPlaceholders(StringBuilder sql, List<Object> params, List<?> values) {
    for (int index = 0; index < values.size(); index++) {
      if (index > 0) {
        sql.append(", ");
      }
      sql.append("?");
      params.add(values.get(index));
    }
  }

  private long sum(Map<Long, Long> values) {
    return values.values().stream().mapToLong(Long::longValue).sum();
  }

  private LocalDate earlier(LocalDate left, LocalDate right) {
    return left.isBefore(right) ? left : right;
  }

  private String percent(long numerator, long denominator) {
    if (denominator <= 0) {
      return "0.00%";
    }
    return String.format(Locale.ROOT, "%.2f%%", numerator * 100.0 / denominator);
  }

  private String percentCapped(long numerator, long denominator) {
    return percent(Math.min(numerator, denominator), denominator);
  }

  private Long longValue(Object value) {
    return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
  }

  private Long nullableLongValue(Object value) {
    return value == null ? null : longValue(value);
  }

  private record ThreeCheckCount(Long dimensionId, String moduleKey, String status, long count) {}

  private static final class ThreeCheckAggregation {
    private final List<ThreeCheckCount> groups;

    private ThreeCheckAggregation(List<ThreeCheckCount> groups) {
      this.groups = groups;
    }

    private static ThreeCheckAggregation empty() {
      return new ThreeCheckAggregation(List.of());
    }

    private Map<Long, Long> counts(List<String> moduleKeys, List<String> statuses) {
      Map<Long, Long> result = new LinkedHashMap<>();
      groups.stream()
          .filter(group -> moduleKeys.contains(group.moduleKey()))
          .filter(group -> statuses == null || statuses.contains(group.status()))
          .forEach(group -> result.merge(group.dimensionId(), group.count(), Long::sum));
      return result;
    }
  }

  private record RiskGroup(
      Long companyId, String riskLevel, String accidentType, String cause, long count) {}

  private final class RiskAggregation {
    private final List<RiskGroup> groups;

    private RiskAggregation(List<RiskGroup> groups) {
      this.groups = groups;
    }

    private Map<Long, RiskCounts> forDimensions(ScopeSelection selection) {
      Map<Long, RiskCounts> byCompany = new LinkedHashMap<>();
      groups.forEach(
          group -> {
            RiskCounts count =
                byCompany.computeIfAbsent(group.companyId(), ignored -> new RiskCounts());
            switch (group.riskLevel()) {
              case "重大风险" -> count.major += group.count();
              case "较大风险" -> count.serious += group.count();
              case "一般风险" -> count.normal += group.count();
              case "低风险" -> count.low += group.count();
              default -> {
              }
            }
          });
      if ("company_id".equals(selection.groupColumn())) {
        return byCompany;
      }
      RiskCounts total = new RiskCounts();
      byCompany.values().forEach(
          counts -> {
            total.major += counts.major;
            total.serious += counts.serious;
            total.normal += counts.normal;
            total.low += counts.low;
          });
      return selection.dimensions().isEmpty()
          ? Map.of()
          : Map.of(selection.dimensions().get(0).id(), total);
    }

    private List<BarItem> accidentTypeBars() {
      return textBars(true);
    }

    private List<BarItem> causeBars() {
      return textBars(false);
    }

    private List<BarItem> textBars(boolean accidentType) {
      Map<String, Long> counts = new LinkedHashMap<>();
      groups.forEach(
          group -> {
            String name = accidentType ? group.accidentType() : group.cause();
            if (!name.isBlank()) {
              counts.merge(name, group.count(), Long::sum);
            }
          });
      return counts.entrySet().stream()
          .sorted(
              Map.Entry.<String, Long>comparingByValue()
                  .reversed()
                  .thenComparing(Map.Entry.comparingByKey()))
          .limit(8)
          .map(entry -> new BarItem(entry.getKey(), entry.getValue()))
          .toList();
    }
  }

  private record LearningGroup(Long companyId, LocalDate date, String category, long count) {}

  private final class LearningAggregation {
    private final List<LearningGroup> groups;

    private LearningAggregation(List<LearningGroup> groups) {
      this.groups = groups;
    }

    private Map<Long, Long> companyCounts(LocalDate start, LocalDate end) {
      Map<Long, Long> counts = new LinkedHashMap<>();
      groups.stream()
          .filter(group -> !group.date().isBefore(start) && !group.date().isAfter(end))
          .forEach(group -> counts.merge(group.companyId(), group.count(), Long::sum));
      return counts;
    }

    private Map<Long, Long> forDimensions(
        ScopeSelection selection, LocalDate start, LocalDate end) {
      Map<Long, Long> companyCounts = companyCounts(start, end);
      if ("company_id".equals(selection.groupColumn())) {
        return companyCounts;
      }
      return selection.dimensions().isEmpty()
          ? Map.of()
          : Map.of(selection.dimensions().get(0).id(), sum(companyCounts));
    }

    private List<BarItem> categoryBars(LocalDate start, LocalDate end) {
      Map<String, Long> counts = new LinkedHashMap<>();
      groups.stream()
          .filter(group -> !group.date().isBefore(start) && !group.date().isAfter(end))
          .filter(group -> !group.category().isBlank())
          .forEach(group -> counts.merge(group.category(), group.count(), Long::sum));
      return counts.entrySet().stream()
          .sorted(
              Map.Entry.<String, Long>comparingByValue()
                  .reversed()
                  .thenComparing(Map.Entry.comparingByKey()))
          .limit(5)
          .map(entry -> new BarItem(entry.getKey(), entry.getValue()))
          .toList();
    }

    private List<BarItem> trend(LocalDate start, LocalDate end) {
      Map<LocalDate, Long> counts = new LinkedHashMap<>();
      groups.stream()
          .filter(group -> !group.date().isBefore(start) && !group.date().isAfter(end))
          .forEach(group -> counts.merge(group.date(), group.count(), Long::sum));
      List<BarItem> trend = new ArrayList<>();
      for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
        trend.add(new BarItem(date.toString(), counts.getOrDefault(date, 0L)));
      }
      return trend;
    }
  }

  private record HazardGroup(Long dimensionId, String status, long count) {}

  private final class HazardAggregation {
    private final List<HazardGroup> groups;

    private HazardAggregation(List<HazardGroup> groups) {
      this.groups = groups;
    }

    private Map<Long, HazardCounts> byDimension() {
      Map<Long, long[]> values = new LinkedHashMap<>();
      groups.forEach(
          group -> {
            long[] counts = values.computeIfAbsent(group.dimensionId(), ignored -> new long[3]);
            if ("CLOSED".equals(group.status())) {
              counts[0] += group.count();
            } else if ("CANCELLED".equals(group.status())) {
              counts[2] += group.count();
            } else {
              counts[1] += group.count();
            }
          });
      Map<Long, HazardCounts> result = new LinkedHashMap<>();
      values.forEach(
          (dimensionId, counts) ->
              result.put(
                  dimensionId, new HazardCounts(counts[0], counts[1], counts[2])));
      return result;
    }

    private List<BarItem> statusBars() {
      Map<String, Long> counts = new LinkedHashMap<>();
      groups.forEach(group -> counts.merge(group.status(), group.count(), Long::sum));
      return List.of(
              "PENDING_ASSIGN",
              "PENDING_RECTIFY",
              "RECTIFIED",
              "PENDING_ACCEPTANCE",
              "CLOSED",
              "CANCELLED")
          .stream()
          .filter(status -> counts.getOrDefault(status, 0L) > 0)
          .map(status -> new BarItem(hazardOrderStatusLabel(status), counts.get(status)))
          .toList();
    }
  }

  private static final class RiskCounts {
    private long major;
    private long serious;
    private long normal;
    private long low;

    private long total() {
      return major + serious + normal + low;
    }
  }

  private record HazardCounts(long done, long open, long cancelled) {}

  private record OrgOption(Long id, Long parentId, String name, String orgType, String orgPath, Long sortOrder) {}

  private record TeamProfile(Long companyId, String workshopName, String leader, String safety) {}

  private record TeamDimension(
      Long id, String name, Long companyId, String companyName, String workshopName, String captain, String safety) {}

  private record TeamMetric(
      TeamDimension team,
      long dispatchTotal,
      long dispatchFinished,
      long preMeeting,
      long preInspection,
      long midInspection,
      long postInspection,
      long hazardDone,
      long openHazards,
      long unfinishedThreeChecks,
      long completedUnits,
      long expectedUnits,
      long companyLearningCount) {
    private long hazardTotal() {
      return hazardDone + openHazards;
    }

    private boolean normal() {
      return openHazards == 0 && unfinishedThreeChecks == 0;
    }

    private String status() {
      return normal() ? "正常" : "异常";
    }

    private double threeCheckRateValue() {
      long expected = dispatchFinished * 4;
      if (expected <= 0) {
        return 0;
      }
      return Math.min(preMeeting + preInspection + midInspection + postInspection, expected) * 100.0 / expected;
    }

    private String dispatchCompletionRate() {
      return percentText(dispatchFinished, dispatchTotal);
    }

    private String preMeetingRate() {
      return percentTextCapped(preMeeting, dispatchFinished);
    }

    private String preInspectionRate() {
      return percentTextCapped(preInspection, dispatchFinished);
    }

    private String midInspectionRate() {
      return percentTextCapped(midInspection, dispatchFinished);
    }

    private String postInspectionRate() {
      return percentTextCapped(postInspection, dispatchFinished);
    }

    private String threeCheckCompletionRate() {
      return percentTextCapped(preMeeting + preInspection + midInspection + postInspection, dispatchFinished * 4);
    }

    private long abnormalItems() {
      long count = 0;
      if (dispatchTotal > dispatchFinished) count++;
      if (dispatchFinished > preMeeting) count++;
      if (dispatchFinished > preInspection) count++;
      if (dispatchFinished > midInspection) count++;
      if (dispatchFinished > postInspection) count++;
      if (openHazards > 0) count++;
      if (companyLearningCount <= 0) count++;
      return count;
    }

    private static String percentText(long numerator, long denominator) {
      if (denominator <= 0) {
        return "0.00%";
      }
      return String.format(Locale.ROOT, "%.2f%%", numerator * 100.0 / denominator);
    }

    private static String percentTextCapped(long numerator, long denominator) {
      return percentText(Math.min(numerator, denominator), denominator);
    }
  }

  private static final class RowRanker {
    private long rank = 0;

    private TeamDetailRow toRow(TeamMetric metric) {
      rank++;
      TeamDimension team = metric.team();
      return new TeamDetailRow(
          team.id(),
          rank,
          team.name(),
          team.companyName(),
          team.workshopName(),
          team.captain(),
          team.safety(),
          metric.status(),
          metric.dispatchCompletionRate(),
          metric.preMeetingRate(),
          metric.preInspectionRate(),
          metric.midInspectionRate(),
          metric.postInspectionRate(),
          metric.threeCheckCompletionRate(),
          metric.hazardTotal(),
          metric.openHazards(),
          metric.companyLearningCount(),
          metric.unfinishedThreeChecks(),
          metric.abnormalItems());
    }
  }

  private record ScopeSelection(
      List<CompanyOption> dimensions,
      List<Long> companyIds,
      Long departmentId,
      Long teamId,
      String groupColumn) {
    private static ScopeSelection empty() {
      return new ScopeSelection(List.of(), List.of(), null, null, "company_id");
    }
  }
}

