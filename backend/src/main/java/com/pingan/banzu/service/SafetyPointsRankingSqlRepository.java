package com.pingan.banzu.service;

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
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class SafetyPointsRankingSqlRepository {

  private final JdbcTemplate jdbcTemplate;
  private final SystemDataScopeService dataScopeService;
  private volatile Boolean postgres;

  SafetyPointsRankingSqlRepository(
      JdbcTemplate jdbcTemplate, SystemDataScopeService dataScopeService) {
    this.jdbcTemplate = jdbcTemplate;
    this.dataScopeService = dataScopeService;
  }

  boolean supportsDatabaseAggregation() {
    Boolean cached = postgres;
    if (cached != null) {
      return cached;
    }
    Boolean detected =
        jdbcTemplate.execute(
            (ConnectionCallback<Boolean>)
                connection -> {
              DatabaseMetaData metadata = connection.getMetaData();
              return metadata.getDatabaseProductName().toLowerCase().contains("postgresql");
            });
    postgres = Boolean.TRUE.equals(detected);
    return postgres;
  }

  SafetyPointsRankingOverviewResponse overview(SafetyPointsRankingQuery query) {
    FilteredSql filtered = filtered(query);
    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            filtered.cte()
                + """
                select
                  coalesce(sum(
                    case
                      when change_type = '加分' then quantity
                      when change_type in ('扣分', '兑换') then -quantity
                      else 0
                    end
                  ), 0) as current_score,
                  count(distinct user_name) as participant_count,
                  coalesce(sum(case when change_type = '加分' then quantity else 0 end), 0) as add_score,
                  coalesce(sum(case when change_type = '兑换' then quantity else 0 end), 0) as redeem_score
                from flows
                """,
            filtered.params().toArray());
    return new SafetyPointsRankingOverviewResponse(
        intValue(row.get("current_score")),
        intValue(row.get("participant_count")),
        intValue(row.get("add_score")),
        intValue(row.get("redeem_score")));
  }

  PageResult<SafetyPointsIndividualRankingRow> individual(SafetyPointsRankingQuery query) {
    FilteredSql filtered = filtered(query);
    Page page = page(query);
    List<Object> params = new ArrayList<>(filtered.params());
    params.add(page.size());
    params.add(page.offset());
    List<Map<String, Object>> rows =
        jdbcTemplate.queryForList(
            filtered.cte()
                + """
                , grouped as (
                  select
                    user_name,
                    (array_agg(company_name order by business_date desc, id desc))[1] as company_name,
                    (array_agg(department_name order by business_date desc, id desc))[1] as department_name,
                    (array_agg(team_name order by business_date desc, id desc))[1] as team_name,
                    sum(case when change_type = '加分' then quantity else 0 end) as add_score,
                    sum(case when change_type = '扣分' then quantity else 0 end) as deduct_score,
                    sum(case when change_type = '兑换' then quantity else 0 end) as redeem_score,
                    count(*) as record_count,
                    max(business_date) as last_business_date
                  from flows
                  group by user_name
                ),
                ranked as (
                  select
                    row_number() over (
                      order by (add_score - deduct_score - redeem_score) desc, user_name
                    ) as rank,
                    count(*) over () as total,
                    *
                  from grouped
                )
                select *
                from ranked
                order by rank
                limit ? offset ?
                """,
            params.toArray());
    List<SafetyPointsIndividualRankingRow> items =
        rows.stream()
            .map(
                row ->
                    new SafetyPointsIndividualRankingRow(
                        intValue(row.get("rank")),
                        text(row.get("user_name")),
                        text(row.get("company_name")),
                        text(row.get("department_name")),
                        text(row.get("team_name")),
                        intValue(row.get("add_score"))
                            - intValue(row.get("deduct_score"))
                            - intValue(row.get("redeem_score")),
                        intValue(row.get("add_score")),
                        intValue(row.get("deduct_score")),
                        intValue(row.get("redeem_score")),
                        intValue(row.get("record_count")),
                        text(row.get("last_business_date"))))
            .toList();
    long total =
        rows.isEmpty()
            ? jdbcTemplate.queryForObject(
                filtered.cte() + "select count(distinct user_name) from flows",
                Long.class,
                filtered.params().toArray())
            : longValue(rows.get(0).get("total"));
    return new PageResult<>(items, total);
  }

  PageResult<SafetyPointsTeamRankingRow> team(SafetyPointsRankingQuery query) {
    FilteredSql filtered = filtered(query);
    Page page = page(query);
    SafetyPointsRankBy rankBy = rankBy(query);
    Dimension dimension = dimension(rankBy);
    List<Object> params = new ArrayList<>(filtered.params());
    params.add(page.size());
    params.add(page.offset());
    List<Map<String, Object>> rows =
        jdbcTemplate.queryForList(
            filtered.cte()
                + """
                , grouped as (
                  select
                    %s as rank_id,
                    %s as rank_name,
                    sum(case when change_type = '加分' then quantity else 0 end) as add_score,
                    sum(case when change_type = '扣分' then quantity else 0 end) as deduct_score,
                    sum(case when change_type = '兑换' then quantity else 0 end) as redeem_score,
                    count(distinct user_name) as member_count,
                    count(*) as record_count,
                    max(business_date) as last_business_date
                  from flows
                  group by %s, %s
                ),
                ranked as (
                  select
                    row_number() over (
                      order by (add_score - deduct_score - redeem_score) desc, rank_name
                    ) as rank,
                    count(*) over () as total,
                    *
                  from grouped
                )
                select *
                from ranked
                order by rank
                limit ? offset ?
                """
                    .formatted(
                        dimension.idColumn(),
                        dimension.nameColumn(),
                        dimension.idColumn(),
                        dimension.nameColumn()),
            params.toArray());
    List<SafetyPointsTeamRankingRow> items =
        rows.stream()
            .map(
                row ->
                    new SafetyPointsTeamRankingRow(
                        intValue(row.get("rank")),
                        rankBy,
                        text(row.get("rank_id")),
                        text(row.get("rank_name")),
                        intValue(row.get("add_score"))
                            - intValue(row.get("deduct_score"))
                            - intValue(row.get("redeem_score")),
                        intValue(row.get("add_score")),
                        intValue(row.get("deduct_score")),
                        intValue(row.get("redeem_score")),
                        intValue(row.get("member_count")),
                        intValue(row.get("record_count")),
                        text(row.get("last_business_date"))))
            .toList();
    long total =
        rows.isEmpty()
            ? jdbcTemplate.queryForObject(
                filtered.cte()
                    + "select count(distinct "
                    + dimension.idColumn()
                    + ") from flows",
                Long.class,
                filtered.params().toArray())
            : longValue(rows.get(0).get("total"));
    return new PageResult<>(items, total);
  }

  SafetyPointsRankingChartsResponse charts(SafetyPointsRankingQuery query) {
    FilteredSql filtered = filtered(query);
    SafetyPointsRankBy rankBy = rankBy(query);
    Dimension dimension = dimension(rankBy);
    List<Map<String, Object>> ranked =
        jdbcTemplate.queryForList(
            filtered.cte()
                + """
                select
                  %s as rank_id,
                  %s as rank_name,
                  sum(
                    case
                      when change_type = '加分' then quantity
                      when change_type in ('扣分', '兑换') then -quantity
                      else 0
                    end
                  ) as score
                from flows
                group by %s, %s
                order by score desc, rank_name
                limit 5
                """
                    .formatted(
                        dimension.idColumn(),
                        dimension.nameColumn(),
                        dimension.idColumn(),
                        dimension.nameColumn()),
            filtered.params().toArray());
    List<TeamTopItem> top =
        ranked.stream()
            .map(row -> new TeamTopItem(text(row.get("rank_name")), intValue(row.get("score"))))
            .toList();
    String topRankId =
        query.rankBy() == null || ranked.isEmpty() ? null : text(ranked.get(0).get("rank_id"));

    QueryWithParams trendQuery =
        groupedChartQuery(
            filtered,
            dimension,
            topRankId,
            """
            select business_date,
              sum(
                case
                  when change_type = '加分' then quantity
                  else -quantity
                end
              ) as score
            from chart_flows
            group by business_date
            order by business_date
            """);
    List<Map<String, Object>> daily =
        jdbcTemplate.queryForList(trendQuery.sql(), trendQuery.params().toArray());
    List<TrendPoint> trend = new ArrayList<>();
    int running = 0;
    for (Map<String, Object> row : daily) {
      running += intValue(row.get("score"));
      trend.add(new TrendPoint(text(row.get("business_date")), running));
    }

    QueryWithParams sourceQuery =
        groupedChartQuery(
            filtered,
            dimension,
            topRankId,
            """
            select points_reason, sum(quantity) as score
            from chart_flows
            group by points_reason
            order by score desc, points_reason
            """);
    List<SourceSlice> sources =
        jdbcTemplate.queryForList(sourceQuery.sql(), sourceQuery.params().toArray()).stream()
            .map(row -> new SourceSlice(text(row.get("points_reason")), intValue(row.get("score"))))
            .toList();
    return new SafetyPointsRankingChartsResponse(trend, sources, top);
  }

  private QueryWithParams groupedChartQuery(
      FilteredSql filtered, Dimension dimension, String topRankId, String selectSql) {
    List<Object> params = new ArrayList<>(filtered.params());
    StringBuilder sql = new StringBuilder(filtered.cte()).append(", chart_flows as (select * from flows");
    if (topRankId != null) {
      sql.append(" where ").append(dimension.idColumn()).append(" = ?");
      params.add(topRankId);
    }
    sql.append(") ").append(selectSql);
    return new QueryWithParams(sql.toString(), params);
  }

  private FilteredSql filtered(SafetyPointsRankingQuery query) {
    CurrentUser currentUser = CurrentUserContext.require();
    List<Object> params = new ArrayList<>();
    StringBuilder where =
        new StringBuilder(
            """
            where r.deleted = 0
              and r.module_key = 'points-flow'
            """);
    appendEquals(where, params, "r.company_id", query.companyId());
    appendEquals(where, params, "r.department_id", query.departmentId());
    appendEquals(where, params, "r.team_id", query.teamId());
    if (query.dateStart() != null) {
      where.append(" and r.business_date >= ?");
      params.add(query.dateStart());
    }
    if (query.dateEnd() != null) {
      where.append(" and r.business_date <= ?");
      params.add(query.dateEnd());
    }
    if (!isBlank(query.keyword())) {
      String keyword = "%" + query.keyword().trim() + "%";
      where.append(
          """
           and (
             r.payload_json like ?
             or c.org_name like ?
             or d.org_name like ?
             or t.org_name like ?
             or u.real_name like ?
           )
          """);
      for (int index = 0; index < 5; index++) {
        params.add(keyword);
      }
    }
    String dataScope = dataScopeService.currentDataScope();
    if ("SELF".equals(dataScope)) {
      where.append(" and r.owner_user_id = ?");
      params.add(currentUser.userId());
    } else if (!"ALL".equals(dataScope)) {
      List<Long> accessOrgIds = dataScopeService.accessibleOrgIds();
      if (accessOrgIds.isEmpty()) {
        where.append(" and 1 = 0");
      } else {
        String placeholders = accessOrgIds.stream().map(ignored -> "?").collect(Collectors.joining(","));
        where.append(" and (r.company_id in (")
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

    String cte =
        """
        with source as (
          select
            r.id,
            r.company_id,
            r.department_id,
            r.team_id,
            r.business_date,
            c.org_name as company_name,
            d.org_name as department_name,
            t.org_name as team_name,
            case
              when r.payload_json is json then r.payload_json::jsonb
              else '{}'::jsonb
            end as payload
          from three_check_record r
          join sys_org c on c.id = r.company_id and c.deleted = 0
          join sys_org d on d.id = r.department_id and d.deleted = 0
          join sys_org t on t.id = r.team_id and t.deleted = 0
          left join sys_user u on u.id = r.owner_user_id and u.deleted = 0
        """
            + where
            + """
        ),
        parsed as (
          select
            *,
            trim(coalesce(payload ->> 'user', '')) as user_name,
            coalesce(nullif(trim(payload ->> 'pointsReason'), ''), '未分类') as points_reason,
            trim(coalesce(payload ->> 'pointsChange', '')) as change_type,
            case
              when coalesce(payload ->> 'pointsQuantity', '') ~ '^[0-9]+$'
                then (payload ->> 'pointsQuantity')::integer
              else 0
            end as quantity
          from source
        ),
        flows as (
          select *
          from parsed
          where user_name <> ''
            and change_type <> ''
            and quantity > 0
        )
        """;
    return new FilteredSql(cte, params);
  }

  private void appendEquals(
      StringBuilder sql, List<Object> params, String column, Object value) {
    if (value != null) {
      sql.append(" and ").append(column).append(" = ?");
      params.add(value);
    }
  }

  private SafetyPointsRankBy rankBy(SafetyPointsRankingQuery query) {
    return query.rankBy() == null ? SafetyPointsRankBy.TEAM : query.rankBy();
  }

  private Dimension dimension(SafetyPointsRankBy rankBy) {
    return switch (rankBy) {
      case COMPANY -> new Dimension("company_id::text", "company_name");
      case DEPARTMENT -> new Dimension("department_id::text", "department_name");
      case TEAM -> new Dimension("team_id::text", "team_name");
    };
  }

  private Page page(SafetyPointsRankingQuery query) {
    int number = Math.max(1, query.page() == null ? 1 : query.page());
    int size = Math.min(100, Math.max(1, query.pageSize() == null ? 10 : query.pageSize()));
    return new Page(size, (number - 1) * size);
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private int intValue(Object value) {
    return value == null ? 0 : ((Number) value).intValue();
  }

  private long longValue(Object value) {
    return value == null ? 0 : ((Number) value).longValue();
  }

  private String text(Object value) {
    return Objects.toString(value, "");
  }

  private record FilteredSql(String cte, List<Object> params) {}

  private record QueryWithParams(String sql, List<Object> params) {}

  private record Dimension(String idColumn, String nameColumn) {}

  private record Page(int size, int offset) {}
}
