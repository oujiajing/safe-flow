package com.pingan.banzu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.domain.BizAttachment;
import com.pingan.banzu.dto.AttachmentResponse;
import com.pingan.banzu.dto.MineOverviewResponse;
import com.pingan.banzu.dto.MineRecordListItem;
import com.pingan.banzu.mapper.BizAttachmentMapper;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MineService {

  private static final String AVATAR_BIZ_TYPE = "USER_AVATAR";
  private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {};
  private static final Set<String> THREE_CHECK_MODULES =
      Set.of(
          "pre-shift-meeting",
          "pre-shift-inspection",
          "mid-shift-inspection",
          "post-shift-inspection");

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final BizAttachmentMapper attachmentMapper;
  private final AttachmentUrlResolver attachmentUrlResolver;
  private final FileStorageService fileStorageService;

  public MineService(
      JdbcTemplate jdbcTemplate,
      ObjectMapper objectMapper,
      BizAttachmentMapper attachmentMapper,
      AttachmentUrlResolver attachmentUrlResolver,
      FileStorageService fileStorageService) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.attachmentMapper = attachmentMapper;
    this.attachmentUrlResolver = attachmentUrlResolver;
    this.fileStorageService = fileStorageService;
  }

  public MineOverviewResponse overview() {
    CurrentUser user = CurrentUserContext.require();
    ProfileRow profile = profile(user.userId());
    PointsSummary pointsSummary = pointsSummary(user.userId());
    int passedExamCount =
        valueOrZero(
            jdbcTemplate.queryForObject(
                """
                select count(*)
                from training_exam_result
                where deleted = 0
                  and exam_person_user_id = ?
                  and status = 'EXAMED'
                  and score >= 60
                """,
                Integer.class,
                user.userId()));

    return new MineOverviewResponse(
        new MineOverviewResponse.Profile(
            user.userId(),
            text(user.username()),
            text(user.realName()),
            text(profile.employeeCode()),
            avatarUrl(user.userId()),
            profile.companyId(),
            orgName(profile.companyId()),
            profile.departmentId(),
            orgName(profile.departmentId()),
            profile.teamId(),
            orgName(profile.teamId()),
            text(profile.positionName()),
            roleNames(user.userId())),
        new MineOverviewResponse.SafetySummary(
            pointsSummary.points(), pointsSummary.learningContentIds().size(), passedExamCount),
        new MineOverviewResponse.Capabilities(true, true, true, true, true, true, true));
  }

  @Transactional
  public AttachmentResponse uploadAvatar(MultipartFile file) {
    CurrentUser user = CurrentUserContext.require();
    AttachmentResponse uploaded =
        fileStorageService.saveBusinessAttachment(
            AVATAR_BIZ_TYPE, user.userId(), "mini/mine/avatar", "IMAGE", file);
    attachmentMapper.update(
        null,
        new UpdateWrapper<BizAttachment>()
            .eq("biz_type", AVATAR_BIZ_TYPE)
            .eq("biz_id", user.userId())
            .ne("id", Long.valueOf(uploaded.id()))
            .eq("deleted", 0)
            .set("deleted", 1));
    return uploaded;
  }

  private String avatarUrl(Long userId) {
    List<BizAttachment> avatars =
        attachmentMapper.selectList(
            new QueryWrapper<BizAttachment>()
                .eq("biz_type", AVATAR_BIZ_TYPE)
                .eq("biz_id", userId)
                .eq("file_kind", "IMAGE")
                .eq("deleted", 0)
                .orderByDesc("uploaded_at", "id")
                .last("LIMIT 1"));
    return avatars.isEmpty() ? "" : attachmentUrlResolver.url(avatars.get(0));
  }

  public PageResult<MineRecordListItem> records(String type, Integer page, Integer pageSize) {
    CurrentUser user = CurrentUserContext.require();
    String normalizedType = normalizeType(type);
    List<MineRecordListItem> all =
        new ArrayList<>(switch (normalizedType) {
          case "dispatch" -> threeCheckRecords(user.userId(), normalizedType, Set.of("team-dispatch", "curtain-wall-team-dispatch"));
          case "three-check" -> threeCheckRecords(user.userId(), normalizedType, THREE_CHECK_MODULES);
          case "hazard-report" -> threeCheckRecords(user.userId(), normalizedType, Set.of("quick-shot", "safety-check"));
          case "rectification" -> rectificationRecords(user.userId());
          case "learning" -> pointRecords(user.userId(), true);
          case "exam" -> examRecords(user.userId());
          case "points" -> pointRecords(user.userId(), false);
          default -> throw new BusinessException("个人记录类型不合法");
        });
    all.sort(
        Comparator.comparing(MineRecordListItem::businessDate, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(MineRecordListItem::id, Comparator.reverseOrder()));
    return page(all, page, pageSize);
  }

  private ProfileRow profile(Long userId) {
    List<ProfileRow> rows =
        jdbcTemplate.query(
            """
            select employee_code, company_org_id, department_org_id, team_org_id, position_name
            from sys_user_profile
            where user_id = ? and deleted = 0
            """,
            (rs, rowNum) ->
                new ProfileRow(
                    rs.getString("employee_code"),
                    nullableLong(rs, "company_org_id"),
                    nullableLong(rs, "department_org_id"),
                    nullableLong(rs, "team_org_id"),
                    rs.getString("position_name")),
            userId);
    if (!rows.isEmpty()) {
      return rows.get(0);
    }
    CurrentUser user = CurrentUserContext.require();
    List<Long> path = orgPathIds(user.orgPath());
    Long companyId = firstOrgId(path, "COMPANY");
    Long departmentId = lastOrgId(path, "DEPARTMENT");
    Long teamId = lastOrgId(path, "TEAM");
    return new ProfileRow("", companyId, departmentId, teamId, "");
  }

  private PointsSummary pointsSummary(Long userId) {
    List<Map<String, Object>> rows =
        jdbcTemplate.queryForList(
            """
            select payload_json
            from three_check_record
            where deleted = 0 and module_key = 'points-flow' and owner_user_id = ?
            """,
            userId);
    int points = 0;
    Set<Long> learningContentIds = new LinkedHashSet<>();
    for (Map<String, Object> row : rows) {
      Map<String, Object> payload = payload(row.get("payload_json"));
      int quantity = intValue(payload.get("pointsQuantity"));
      String change = text(payload.get("pointsChange"));
      points += "加分".equals(change) ? quantity : -quantity;
      Long learningContentId = longValue(payload.get("learningContentId"));
      if (learningContentId != null) {
        learningContentIds.add(learningContentId);
      }
    }
    return new PointsSummary(points, learningContentIds);
  }

  private List<MineRecordListItem> threeCheckRecords(Long userId, String type, Set<String> moduleKeys) {
    String placeholders = String.join(",", moduleKeys.stream().map(key -> "?").toList());
    List<Object> params = new ArrayList<>();
    params.add(userId);
    params.addAll(moduleKeys);
    String sql =
        """
        select r.id, r.module_key, r.record_no, r.business_date, r.status, r.payload_json,
               coalesce(t.org_name, '') as team_name
        from three_check_record r
        left join sys_org t on t.id = r.team_id and t.deleted = 0
        where r.deleted = 0
          and r.owner_user_id = ?
          and r.module_key in (%s)
        """.formatted(placeholders);
    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> {
          String moduleKey = rs.getString("module_key");
          Map<String, Object> payload = payload(rs.getString("payload_json"));
          String recordNo = text(rs.getString("record_no"));
          String title = recordTitle(type, moduleKey, recordNo, payload);
          return new MineRecordListItem(
              rs.getLong("id"),
              type,
              moduleKey,
              rs.getLong("id"),
              null,
              title,
              text(rs.getString("team_name")),
              text(rs.getString("status")),
              statusLabel(rs.getString("status")),
              localDate(rs, "business_date"),
              null);
        },
        params.toArray());
  }

  private List<MineRecordListItem> rectificationRecords(Long userId) {
    return jdbcTemplate.query(
        """
        select o.id, o.order_no, o.business_date, o.status, o.hazard_count,
               coalesce(t.org_name, '') as team_name
        from hazard_rectification_order o
        left join sys_org t on t.id = o.team_id and t.deleted = 0
        where o.deleted = 0
          and (o.rectification_responsible_user_id = ? or o.created_by = ? or o.rectified_by = ?)
        """,
        (rs, rowNum) ->
            new MineRecordListItem(
                rs.getLong("id"),
                "rectification",
                "hazard-rectification",
                rs.getLong("id"),
                null,
                textOrDefault(rs.getString("order_no"), "隐患整改记录"),
                text(rs.getString("team_name")) + " · 隐患" + rs.getInt("hazard_count") + "项",
                text(rs.getString("status")),
                statusLabel(rs.getString("status")),
                localDate(rs, "business_date"),
                null),
        userId,
        userId,
        userId);
  }

  private List<MineRecordListItem> pointRecords(Long userId, boolean learningOnly) {
    List<MineRecordListItem> rows =
        jdbcTemplate.query(
            """
            select id, record_no, business_date, status, payload_json
            from three_check_record
            where deleted = 0 and module_key = 'points-flow' and owner_user_id = ?
            """,
            (rs, rowNum) -> {
              Map<String, Object> payload = payload(rs.getString("payload_json"));
              Long learningContentId = longValue(payload.get("learningContentId"));
              if (learningOnly && learningContentId == null) {
                return null;
              }
              int quantity = intValue(payload.get("pointsQuantity"));
              String change = text(payload.get("pointsChange"));
              int pointsDelta = "加分".equals(change) ? quantity : -quantity;
              String title =
                  learningOnly
                      ? textOrDefault(text(payload.get("learningTitle")), "安全学习")
                      : textOrDefault(text(payload.get("pointsReason")), rs.getString("record_no"));
              return new MineRecordListItem(
                  rs.getLong("id"),
                  learningOnly ? "learning" : "points",
                  learningOnly ? "safety-learning" : "points-flow",
                  rs.getLong("id"),
                  learningContentId,
                  title,
                  learningOnly ? "已完成学习" : change,
                  text(rs.getString("status")),
                  learningOnly ? "已学习" : statusLabel(rs.getString("status")),
                  localDate(rs, "business_date"),
                  pointsDelta);
            },
            userId);
    return rows.stream().filter(item -> item != null).toList();
  }

  private List<MineRecordListItem> examRecords(Long userId) {
    return jdbcTemplate.query(
        """
        select r.id, r.task_id, r.exam_date, r.status, r.score, t.exam
        from training_exam_result r
        join training_exam_task t on t.id = r.task_id and t.deleted = 0
        where r.deleted = 0 and r.exam_person_user_id = ?
        """,
        (rs, rowNum) -> {
          BigDecimal score = rs.getBigDecimal("score");
          String scoreText = score == null ? "--" : score.stripTrailingZeros().toPlainString();
          boolean passed = score != null && score.compareTo(new BigDecimal("60")) >= 0;
          return new MineRecordListItem(
              rs.getLong("id"),
              "exam",
              "safety-exam",
              rs.getLong("id"),
              rs.getLong("task_id"),
              textOrDefault(rs.getString("exam"), "安全考试"),
              "成绩 " + scoreText + " 分",
              text(rs.getString("status")),
              "EXAMED".equalsIgnoreCase(rs.getString("status")) ? (passed ? "已通过" : "未通过") : "待考试",
              localDate(rs, "exam_date"),
              null);
        },
        userId);
  }

  private <T> PageResult<T> page(List<T> rows, Integer page, Integer pageSize) {
    int safePage = Math.max(1, page == null ? 1 : page);
    int safePageSize = Math.min(100, Math.max(1, pageSize == null ? 20 : pageSize));
    int from = Math.min(rows.size(), (safePage - 1) * safePageSize);
    int to = Math.min(rows.size(), from + safePageSize);
    return new PageResult<>(rows.subList(from, to), rows.size());
  }

  private String normalizeType(String type) {
    return type == null ? "" : type.trim().toLowerCase(Locale.ROOT).replace('_', '-');
  }

  private String recordTitle(String type, String moduleKey, String recordNo, Map<String, Object> payload) {
    if ("dispatch".equals(type)) {
      return "派班记录 · " + recordNo;
    }
    if ("hazard-report".equals(type)) {
      String description = text(payload.get("hazardDescription"));
      return textOrDefault(description, moduleLabel(moduleKey) + " · " + recordNo);
    }
    return moduleLabel(moduleKey) + " · " + recordNo;
  }

  private String moduleLabel(String moduleKey) {
    return switch (text(moduleKey)) {
      case "pre-shift-meeting" -> "班前会";
      case "pre-shift-inspection" -> "班前检查";
      case "mid-shift-inspection" -> "班中检查";
      case "post-shift-inspection" -> "班后检查";
      case "quick-shot" -> "随手拍";
      case "safety-check" -> "安全检查";
      default -> "业务记录";
    };
  }

  private String statusLabel(String status) {
    return switch (text(status).toUpperCase(Locale.ROOT)) {
      case "DRAFT" -> "草稿";
      case "OPENED" -> "进行中";
      case "PENDING_REVIEW", "PENDING_APPROVAL" -> "待审核";
      case "REVIEWED", "APPROVED" -> "已审核";
      case "PENDING_RECTIFICATION" -> "待整改";
      case "RECTIFIED" -> "已整改";
      case "PENDING_ACCEPTANCE" -> "待验收";
      case "ACCEPTED", "ARCHIVED", "CLOSED" -> "已完成";
      case "REJECTED" -> "已驳回";
      default -> textOrDefault(status, "--");
    };
  }

  private List<String> roleNames(Long userId) {
    return jdbcTemplate.queryForList(
        """
        select r.role_name
        from sys_user_role ur
        join sys_role r on r.id = ur.role_id and r.deleted = 0
        where ur.user_id = ?
        order by r.id
        """,
        String.class,
        userId);
  }

  private String orgName(Long orgId) {
    if (orgId == null) {
      return "";
    }
    List<String> names =
        jdbcTemplate.queryForList(
            "select org_name from sys_org where id = ? and deleted = 0", String.class, orgId);
    return names.isEmpty() ? "" : text(names.get(0));
  }

  private List<Long> orgPathIds(String orgPath) {
    if (orgPath == null || orgPath.isBlank()) {
      return List.of();
    }
    return java.util.Arrays.stream(orgPath.split("/"))
        .filter(value -> !value.isBlank())
        .map(this::longValue)
        .filter(value -> value != null)
        .toList();
  }

  private Long firstOrgId(List<Long> ids, String orgType) {
    for (Long id : ids) {
      if (orgType.equals(orgType(id))) {
        return id;
      }
    }
    return null;
  }

  private Long lastOrgId(List<Long> ids, String orgType) {
    for (int i = ids.size() - 1; i >= 0; i--) {
      if (orgType.equals(orgType(ids.get(i)))) {
        return ids.get(i);
      }
    }
    return null;
  }

  private String orgType(Long orgId) {
    List<String> types =
        jdbcTemplate.queryForList(
            "select org_type from sys_org where id = ? and deleted = 0", String.class, orgId);
    return types.isEmpty() ? "" : text(types.get(0));
  }

  private Map<String, Object> payload(Object payloadJson) {
    if (payloadJson == null || String.valueOf(payloadJson).isBlank()) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(String.valueOf(payloadJson), PAYLOAD_TYPE);
    } catch (JsonProcessingException ignored) {
      return Map.of();
    }
  }

  private LocalDate localDate(ResultSet rs, String column) throws SQLException {
    java.sql.Date value = rs.getDate(column);
    return value == null ? null : value.toLocalDate();
  }

  private Long nullableLong(ResultSet rs, String column) throws SQLException {
    Object value = rs.getObject(column);
    return value instanceof Number number ? number.longValue() : null;
  }

  private Long longValue(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value == null || String.valueOf(value).isBlank()) {
      return null;
    }
    try {
      return Long.valueOf(String.valueOf(value));
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private int intValue(Object value) {
    if (value instanceof Number number) {
      return Math.max(0, number.intValue());
    }
    try {
      return Math.max(0, Integer.parseInt(String.valueOf(value)));
    } catch (RuntimeException ignored) {
      return 0;
    }
  }

  private int valueOrZero(Integer value) {
    return value == null ? 0 : value;
  }

  private String text(Object value) {
    return value == null ? "" : String.valueOf(value).trim();
  }

  private String textOrDefault(String value, String defaultValue) {
    return value == null || value.isBlank() ? text(defaultValue) : value.trim();
  }

  private record ProfileRow(
      String employeeCode,
      Long companyId,
      Long departmentId,
      Long teamId,
      String positionName) {}

  private record PointsSummary(int points, Set<Long> learningContentIds) {}
}
