package com.pingan.banzu.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.dto.SafetyPointsIndividualRankingRow;
import com.pingan.banzu.dto.SafetyPointsRankBy;
import com.pingan.banzu.dto.SafetyPointsRankingChartsResponse;
import com.pingan.banzu.dto.SafetyPointsRankingChartsResponse.SourceSlice;
import com.pingan.banzu.dto.SafetyPointsRankingChartsResponse.TeamTopItem;
import com.pingan.banzu.dto.SafetyPointsRankingChartsResponse.TrendPoint;
import com.pingan.banzu.dto.SafetyPointsRankingOverviewResponse;
import com.pingan.banzu.dto.SafetyPointsRankingQuery;
import com.pingan.banzu.dto.SafetyPointsTeamRankingRow;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import com.pingan.banzu.system.security.SystemDataScopeService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SafetyPointsRankingService {

  private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {};

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final SystemDataScopeService dataScopeService;
  private final SafetyPointsPermissionPolicy permissionPolicy;
  private final SafetyPointsRankingSqlRepository sqlRepository;

  public SafetyPointsRankingService(
      JdbcTemplate jdbcTemplate,
      ObjectMapper objectMapper,
      SystemDataScopeService dataScopeService,
      SafetyPointsPermissionPolicy permissionPolicy,
      SafetyPointsRankingSqlRepository sqlRepository) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.dataScopeService = dataScopeService;
    this.permissionPolicy = permissionPolicy;
    this.sqlRepository = sqlRepository;
  }

  public SafetyPointsRankingOverviewResponse overview(SafetyPointsRankingQuery query) {
    permissionPolicy.assertCanViewRanking();
    if (sqlRepository.supportsDatabaseAggregation()) {
      return sqlRepository.overview(query);
    }
    List<FlowRecord> records = records(query);
    Map<String, RankedBucket> individualScores = aggregateIndividuals(records);
    int currentTotalScore = individualScores.values().stream().mapToInt(RankedBucket::currentScore).sum();
    int addScore = records.stream().mapToInt(record -> "加分".equals(record.pointsChange()) ? record.quantity() : 0).sum();
    int redeemScore = records.stream().mapToInt(record -> "兑换".equals(record.pointsChange()) ? record.quantity() : 0).sum();
    return new SafetyPointsRankingOverviewResponse(
        currentTotalScore, individualScores.size(), addScore, redeemScore);
  }

  public SafetyPointsRankingChartsResponse charts(SafetyPointsRankingQuery query) {
    permissionPolicy.assertCanViewRanking();
    if (sqlRepository.supportsDatabaseAggregation()) {
      return sqlRepository.charts(query);
    }
    List<FlowRecord> records = records(query);
    SafetyPointsRankBy rankBy = rankBy(query);
    List<RankedBucket> rankedBuckets = aggregateTeams(records, rankBy);
    List<FlowRecord> chartRecords =
        query.rankBy() == null ? records : recordsInTopRankedBucket(records, rankBy, rankedBuckets);

    List<Map.Entry<LocalDate, Integer>> dailyScores =
        chartRecords.stream()
            .collect(
                Collectors.groupingBy(
                    FlowRecord::businessDate,
                    LinkedHashMap::new,
                    Collectors.summingInt(FlowRecord::signedScore)))
            .entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .toList();
    List<TrendPoint> trend = new ArrayList<>();
    int runningScore = 0;
    for (Map.Entry<LocalDate, Integer> entry : dailyScores) {
      runningScore += entry.getValue();
      trend.add(new TrendPoint(entry.getKey().toString(), runningScore));
    }

    List<SourceSlice> sources =
        chartRecords.stream()
            .collect(
                Collectors.groupingBy(
                    FlowRecord::pointsReason,
                    HashMap::new,
                    Collectors.summingInt(FlowRecord::quantity)))
            .entrySet()
            .stream()
            .sorted(
                Map.Entry.<String, Integer>comparingByValue()
                    .reversed()
                    .thenComparing(Map.Entry.comparingByKey()))
            .map(entry -> new SourceSlice(entry.getKey(), entry.getValue()))
            .toList();

    List<TeamTopItem> teamTop5 =
        rankedBuckets.stream()
            .limit(5)
            .map(bucket -> new TeamTopItem(bucket.name(), bucket.currentScore()))
            .toList();

    return new SafetyPointsRankingChartsResponse(trend, sources, teamTop5);
  }

  public PageResult<SafetyPointsIndividualRankingRow> individual(SafetyPointsRankingQuery query) {
    permissionPolicy.assertCanViewRanking();
    if (sqlRepository.supportsDatabaseAggregation()) {
      return sqlRepository.individual(query);
    }
    List<RankedBucket> ranked =
        aggregateIndividuals(records(query)).values().stream()
            .sorted(scoreComparator())
            .toList();
    List<SafetyPointsIndividualRankingRow> rows = new ArrayList<>();
    for (int i = 0; i < ranked.size(); i++) {
      RankedBucket bucket = ranked.get(i);
      rows.add(
          new SafetyPointsIndividualRankingRow(
              i + 1,
              bucket.name(),
              bucket.company(),
              bucket.department(),
              bucket.team(),
              bucket.currentScore(),
              bucket.addScore(),
              bucket.deductScore(),
              bucket.redeemScore(),
              bucket.recordCount(),
              dateText(bucket.lastBusinessDate())));
    }
    return page(rows, query);
  }

  public PageResult<SafetyPointsTeamRankingRow> team(SafetyPointsRankingQuery query) {
    permissionPolicy.assertCanViewRanking();
    if (sqlRepository.supportsDatabaseAggregation()) {
      return sqlRepository.team(query);
    }
    SafetyPointsRankBy rankBy = rankBy(query);
    List<RankedBucket> ranked = aggregateTeams(records(query), rankBy);
    List<SafetyPointsTeamRankingRow> rows = new ArrayList<>();
    for (int i = 0; i < ranked.size(); i++) {
      RankedBucket bucket = ranked.get(i);
      rows.add(
          new SafetyPointsTeamRankingRow(
              i + 1,
              rankBy,
              bucket.id(),
              bucket.name(),
              bucket.currentScore(),
              bucket.addScore(),
              bucket.deductScore(),
              bucket.redeemScore(),
              bucket.memberNames().size(),
              bucket.recordCount(),
              dateText(bucket.lastBusinessDate())));
    }
    return page(rows, query);
  }

  private SafetyPointsRankBy rankBy(SafetyPointsRankingQuery query) {
    return query.rankBy() == null ? SafetyPointsRankBy.TEAM : query.rankBy();
  }

  private List<FlowRecord> records(SafetyPointsRankingQuery query) {
    CurrentUser currentUser = CurrentUserContext.require();
    List<Object> params = new ArrayList<>();
    StringBuilder sql =
        new StringBuilder(
            """
            select r.id, r.company_id, r.department_id, r.team_id, r.owner_user_id,
                   r.business_date, r.payload_json,
                   c.org_name as company_name, d.org_name as department_name, t.org_name as team_name,
                   u.real_name as owner_name
            from three_check_record r
            join sys_org c on c.id = r.company_id and c.deleted = 0
            join sys_org d on d.id = r.department_id and d.deleted = 0
            join sys_org t on t.id = r.team_id and t.deleted = 0
            left join sys_user u on u.id = r.owner_user_id and u.deleted = 0
            where r.deleted = 0
              and r.module_key = 'points-flow'
            """);

    appendEquals(sql, params, "r.company_id", query.companyId());
    appendEquals(sql, params, "r.department_id", query.departmentId());
    appendEquals(sql, params, "r.team_id", query.teamId());
    if (query.dateStart() != null) {
      sql.append(" and r.business_date >= ?");
      params.add(query.dateStart());
    }
    if (query.dateEnd() != null) {
      sql.append(" and r.business_date <= ?");
      params.add(query.dateEnd());
    }
    if (!isBlank(query.keyword())) {
      String keyword = "%" + query.keyword().trim() + "%";
      sql.append(
          """
           and (
             r.payload_json like ?
             or c.org_name like ?
             or d.org_name like ?
             or t.org_name like ?
             or u.real_name like ?
           )
          """);
      params.add(keyword);
      params.add(keyword);
      params.add(keyword);
      params.add(keyword);
      params.add(keyword);
    }
    String dataScope = dataScopeService.currentDataScope();
    if ("SELF".equals(dataScope)) {
      sql.append(" and r.owner_user_id = ?");
      params.add(currentUser.userId());
    } else if (!"ALL".equals(dataScope)) {
      List<Long> accessOrgIds = dataScopeService.accessibleOrgIds();
      if (accessOrgIds.isEmpty()) {
        sql.append(" and 1 = 0");
      } else {
        String placeholders = accessOrgIds.stream().map(_id -> "?").collect(Collectors.joining(","));
        sql.append(" and (r.company_id in (")
            .append(placeholders)
            .append(") or r.department_id in (")
            .append(placeholders)
            .append(") or r.team_id in (")
            .append(placeholders)
            .append("))");
        params.addAll(accessOrgIds);
        params.addAll(accessOrgIds);
        params.addAll(accessOrgIds);
      }
    }
    sql.append(" order by r.business_date asc, r.id asc");

    return jdbcTemplate.query(sql.toString(), this::toFlowRecord, params.toArray()).stream()
        .filter(Objects::nonNull)
        .toList();
  }

  private FlowRecord toFlowRecord(ResultSet rs, int rowNum) throws SQLException {
    Map<String, Object> payload = payloadMap(rs.getString("payload_json"));
    String pointsChange = text(payload.get("pointsChange"));
    String pointsReason = text(payload.get("pointsReason"));
    String userName = text(payload.get("user"));
    int quantity = quantity(payload.get("pointsQuantity"));
    if (isBlank(pointsChange) || isBlank(userName) || quantity <= 0) {
      return null;
    }
    return new FlowRecord(
        rs.getLong("id"),
        String.valueOf(rs.getLong("company_id")),
        String.valueOf(rs.getLong("department_id")),
        String.valueOf(rs.getLong("team_id")),
        userName,
        textOrDefault(pointsReason, "未分类"),
        pointsChange,
        quantity,
        rs.getDate("business_date").toLocalDate(),
        rs.getString("company_name"),
        rs.getString("department_name"),
        rs.getString("team_name"));
  }

  private Map<String, RankedBucket> aggregateIndividuals(List<FlowRecord> records) {
    Map<String, ScoreBucket> buckets = new LinkedHashMap<>();
    for (FlowRecord record : records) {
      buckets.computeIfAbsent(record.userName(), key -> new ScoreBucket()).accept(record);
    }
    return buckets.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().ranked(entry.getKey(), entry.getKey()),
                (left, right) -> left,
                LinkedHashMap::new));
  }

  private List<RankedBucket> aggregateTeams(List<FlowRecord> records, SafetyPointsRankBy rankBy) {
    Map<String, ScoreBucket> buckets = new LinkedHashMap<>();
    for (FlowRecord record : records) {
      String id = rankId(record, rankBy);
      String name = rankName(record, rankBy);
      buckets.computeIfAbsent(id, key -> new ScoreBucket()).accept(record, id, name);
    }
    return buckets.values().stream().map(ScoreBucket::ranked).sorted(scoreComparator()).toList();
  }

  private List<FlowRecord> recordsInTopRankedBucket(
      List<FlowRecord> records, SafetyPointsRankBy rankBy, List<RankedBucket> rankedBuckets) {
    if (rankedBuckets.isEmpty()) {
      return List.of();
    }
    String topRankId = rankedBuckets.get(0).id();
    return records.stream().filter(record -> topRankId.equals(rankId(record, rankBy))).toList();
  }

  private Comparator<RankedBucket> scoreComparator() {
    return Comparator.comparingInt(RankedBucket::currentScore)
        .reversed()
        .thenComparing(RankedBucket::name);
  }

  private String rankId(FlowRecord record, SafetyPointsRankBy rankBy) {
    return switch (rankBy) {
      case COMPANY -> record.companyId();
      case DEPARTMENT -> record.departmentId();
      case TEAM -> record.teamId();
    };
  }

  private String rankName(FlowRecord record, SafetyPointsRankBy rankBy) {
    return switch (rankBy) {
      case COMPANY -> record.company();
      case DEPARTMENT -> record.department();
      case TEAM -> record.team();
    };
  }

  private <T> PageResult<T> page(List<T> rows, SafetyPointsRankingQuery query) {
    int page = Math.max(1, query.page() == null ? 1 : query.page());
    int pageSize = Math.min(100, Math.max(1, query.pageSize() == null ? 10 : query.pageSize()));
    int from = Math.min(rows.size(), (page - 1) * pageSize);
    int to = Math.min(rows.size(), from + pageSize);
    return new PageResult<>(rows.subList(from, to), rows.size());
  }

  private Map<String, Object> payloadMap(String payloadJson) {
    if (isBlank(payloadJson)) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(payloadJson, PAYLOAD_TYPE);
    } catch (JsonProcessingException e) {
      return Map.of();
    }
  }

  private void appendEquals(StringBuilder sql, List<Object> params, String column, Object value) {
    if (value != null) {
      sql.append(" and ").append(column).append(" = ?");
      params.add(value);
    }
  }

  private int quantity(Object value) {
    if (value instanceof Number number) {
      return Math.max(0, number.intValue());
    }
    if (value == null) {
      return 0;
    }
    try {
      return Math.max(0, Integer.parseInt(String.valueOf(value)));
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private String text(Object value) {
    return value == null ? "" : String.valueOf(value).trim();
  }

  private String textOrDefault(String value, String defaultValue) {
    return isBlank(value) ? defaultValue : value;
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private String dateText(LocalDate date) {
    return date == null ? "" : date.toString();
  }

  private record FlowRecord(
      long id,
      String companyId,
      String departmentId,
      String teamId,
      String userName,
      String pointsReason,
      String pointsChange,
      int quantity,
      LocalDate businessDate,
      String company,
      String department,
      String team) {

    int signedScore() {
      return "加分".equals(pointsChange) ? quantity : -quantity;
    }
  }

  private static final class ScoreBucket {
    private String id;
    private String name;
    private String company;
    private String department;
    private String team;
    private int addScore;
    private int deductScore;
    private int redeemScore;
    private int recordCount;
    private LocalDate lastBusinessDate;
    private final Set<String> memberNames = new java.util.LinkedHashSet<>();

    void accept(FlowRecord record) {
      accept(record, record.userName(), record.userName());
    }

    void accept(FlowRecord record, String id, String name) {
      this.id = id;
      this.name = name;
      this.company = record.company();
      this.department = record.department();
      this.team = record.team();
      this.recordCount += 1;
      this.memberNames.add(record.userName());
      this.lastBusinessDate =
          this.lastBusinessDate == null || record.businessDate().isAfter(this.lastBusinessDate)
              ? record.businessDate()
              : this.lastBusinessDate;
      if ("加分".equals(record.pointsChange())) {
        this.addScore += record.quantity();
      } else if ("扣分".equals(record.pointsChange())) {
        this.deductScore += record.quantity();
      } else if ("兑换".equals(record.pointsChange())) {
        this.redeemScore += record.quantity();
      }
    }

    RankedBucket ranked() {
      return ranked(id, name);
    }

    RankedBucket ranked(String id, String name) {
      return new RankedBucket(
          id,
          name,
          company,
          department,
          team,
          addScore,
          deductScore,
          redeemScore,
          recordCount,
          lastBusinessDate,
          memberNames);
    }
  }

  private record RankedBucket(
      String id,
      String name,
      String company,
      String department,
      String team,
      int addScore,
      int deductScore,
      int redeemScore,
      int recordCount,
      LocalDate lastBusinessDate,
      Set<String> memberNames) {

    int currentScore() {
      return addScore - deductScore - redeemScore;
    }
  }
}
