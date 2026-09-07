package com.pingan.banzu.service;

import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.dto.NotificationListItem;
import com.pingan.banzu.dto.NotificationQuery;
import com.pingan.banzu.dto.NotificationRouteResolution;
import com.pingan.banzu.dto.NotificationUnreadCounts;
import com.pingan.banzu.security.CurrentUserContext;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

  private static final String NOTIFICATION_FROM =
      " FROM biz_notification n JOIN biz_notification_recipient r ON r.notification_id = n.id"
          + " LEFT JOIN sys_user recipient ON recipient.id = r.recipient_user_id AND recipient.deleted = 0"
          + " LEFT JOIN sys_org recipient_org ON recipient_org.id = recipient.org_id AND recipient_org.deleted = 0"
          + " LEFT JOIN sys_org notification_org ON notification_org.id = n.organization_id AND notification_org.deleted = 0";
  private static final String BUSINESS_CONTEXT_SELECT =
      ", COALESCE(CASE"
          + " WHEN n.biz_type = 'PRE_SHIFT_MEETING' THEN"
          + " (SELECT o.org_name FROM three_check_record b LEFT JOIN sys_org o ON o.id = b.team_id"
          + " WHERE b.id = n.biz_id AND b.module_key = 'pre-shift-meeting' AND b.deleted = 0)"
          + " WHEN n.biz_type IN ('PRE_SHIFT_INSPECTION', 'MID_SHIFT_INSPECTION', 'POST_SHIFT_MEETING',"
          + " 'THREE_CHECK_PRE_SHIFT_INSPECTION', 'THREE_CHECK_MID_SHIFT_INSPECTION',"
          + " 'THREE_CHECK_POST_SHIFT_INSPECTION') THEN"
          + " (SELECT o.org_name FROM three_check_record b LEFT JOIN sys_org o ON o.id = b.team_id"
          + " WHERE b.id = n.biz_id AND b.deleted = 0)"
          + " WHEN n.biz_type = 'HAZARD_RECTIFICATION_ORDER' THEN"
          + " (SELECT o.org_name FROM hazard_rectification_order b LEFT JOIN sys_org o ON o.id = b.team_id"
          + " WHERE b.id = n.biz_id AND b.deleted = 0)"
          + " WHEN n.biz_type = 'SPECIAL_WORK' THEN"
          + " (SELECT o.org_name FROM special_work_record b LEFT JOIN sys_org o ON o.id = b.company_id"
          + " WHERE b.id = n.biz_id AND b.deleted = 0)"
          + " END, recipient_org.org_name) AS team_name,"
          + " COALESCE(recipient.real_name, recipient.username) AS responsible_name";

  public static final String GROUP_ACTION = "ACTION";
  public static final String GROUP_BUSINESS = "BUSINESS";
  public static final String GROUP_SYSTEM = "SYSTEM";
  public static final String HANDLING_NONE = "NONE";
  public static final String HANDLING_PENDING = "PENDING";
  public static final String HANDLING_HANDLED = "HANDLED";

  private static final Set<String> GROUPS = Set.of(GROUP_ACTION, GROUP_BUSINESS, GROUP_SYSTEM);
  private static final Set<String> HANDLING_STATUSES = Set.of(HANDLING_NONE, HANDLING_PENDING, HANDLING_HANDLED);
  private static final Map<String, String> MODULE_NAMES =
      Map.ofEntries(
          Map.entry("pre-shift-meeting", "班前会"),
          Map.entry("pre-shift-inspection", "班前检查"),
          Map.entry("mid-shift-inspection", "班中检查"),
          Map.entry("post-shift-inspection", "班后检查"),
          Map.entry("team-dispatch", "班组派班"),
          Map.entry("hazard-source", "隐患来源"),
          Map.entry("hazard-rectification", "隐患整改"),
          Map.entry("special-work", "特殊作业"),
          Map.entry("safety-exam", "安全考试"),
          Map.entry("safety-learning", "安全学习"),
          Map.entry("risk-control", "风险管控"),
          Map.entry("key-site", "重点场所"),
          Map.entry("safety-points", "安全积分"),
          Map.entry("safety-ledger", "安全台账"),
          Map.entry("system-account", "账号安全"),
          Map.entry("system-import", "数据导入"),
          Map.entry("system-announcement", "系统公告"));
  private static final Map<String, String> ACTION_LABELS =
      Map.ofEntries(
          Map.entry("OPEN_MEETING", "去开会"),
          Map.entry("OPEN_INSPECTION", "去检查"),
          Map.entry("RECTIFY", "去整改"),
          Map.entry("ACCEPT", "去验收"),
          Map.entry("APPROVE", "去审批"),
          Map.entry("REVIEW", "去审核"),
          Map.entry("TAKE_EXAM", "去考试"),
          Map.entry("VIEW", "查看详情"));
  private static final Map<String, String> ROUTE_KEYS =
      Map.ofEntries(
          Map.entry("pre-shift-meeting", "THREE_CHECK_MEETING_DETAIL"),
          Map.entry("pre-shift-inspection", "THREE_CHECK_INSPECTION_DETAIL"),
          Map.entry("mid-shift-inspection", "THREE_CHECK_INSPECTION_DETAIL"),
          Map.entry("post-shift-inspection", "THREE_CHECK_INSPECTION_DETAIL"),
          Map.entry("team-dispatch", "TEAM_DISPATCH_DETAIL"),
          Map.entry("hazard-source", "HAZARD_SOURCE_DETAIL"),
          Map.entry("hazard-rectification", "HAZARD_RECTIFICATION_DETAIL"),
          Map.entry("special-work", "SPECIAL_WORK_DETAIL"),
          Map.entry("safety-exam", "SAFETY_EXAM_DETAIL"),
          Map.entry("safety-learning", "SAFETY_LEARNING_DETAIL"),
          Map.entry("risk-control", "RISK_CONTROL_DETAIL"),
          Map.entry("key-site", "KEY_SITE_DETAIL"),
          Map.entry("safety-points", "SAFETY_POINTS_FLOW"),
          Map.entry("safety-ledger", "SAFETY_LEDGER_DETAIL"),
          Map.entry("system-announcement", "ANNOUNCEMENT_DETAIL"));

  private final JdbcTemplate jdbcTemplate;
  private final NamedParameterJdbcTemplate namedJdbcTemplate;

  public NotificationService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
  }

  public PageResult<NotificationListItem> list(NotificationQuery query) {
    NotificationQuery safe = query == null
        ? new NotificationQuery(null, null, null, null, null, null, null, null, null, null)
        : query;
    Long userId = CurrentUserContext.require().userId();
    int page = Math.max(1, safe.page() == null ? 1 : safe.page());
    int pageSize = Math.min(100, Math.max(1, safe.pageSize() == null ? 20 : safe.pageSize()));
    StringBuilder where = new StringBuilder(" WHERE r.recipient_user_id = :userId AND r.archived = 0");
    MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
    appendFilters(where, params, safe);

    long total = namedJdbcTemplate.queryForObject("SELECT COUNT(*)" + NOTIFICATION_FROM + where, params, Long.class);
    params.addValue("limit", pageSize).addValue("offset", (page - 1) * pageSize);
    List<NotificationListItem> items =
        namedJdbcTemplate.query(
            "SELECT n.*, r.recipient_user_id, r.read_at, r.handling_status, r.action_status,"
                + " r.recipient_reason, r.cancel_reason,"
                + " notification_org.org_name AS organization_name" + BUSINESS_CONTEXT_SELECT + NOTIFICATION_FROM + where
                + " ORDER BY CASE n.severity WHEN 'URGENT' THEN 0 WHEN 'IMPORTANT' THEN 1 ELSE 2 END,"
                + " CASE WHEN r.handling_status = 'PENDING' AND n.deadline < CURRENT_TIMESTAMP THEN 0 ELSE 1 END,"
                + " n.created_at DESC, n.id DESC LIMIT :limit OFFSET :offset",
            params,
            this::mapItem);
    return new PageResult<>(items, total);
  }

  public NotificationListItem detail(Long id) {
    Long userId = CurrentUserContext.require().userId();
    try {
      return jdbcTemplate.queryForObject(
          "SELECT n.*, r.recipient_user_id, r.read_at, r.handling_status, r.action_status,"
              + " r.recipient_reason, r.cancel_reason,"
              + " notification_org.org_name AS organization_name" + BUSINESS_CONTEXT_SELECT + NOTIFICATION_FROM
              + " WHERE n.id = ? AND r.recipient_user_id = ? AND r.archived = 0",
          this::mapItem,
          id,
          userId);
    } catch (EmptyResultDataAccessException ex) {
      throw new BusinessException("消息不存在");
    }
  }

  public NotificationUnreadCounts unreadCounts() {
    Long userId = CurrentUserContext.require().userId();
    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "SELECT"
                + " SUM(CASE WHEN r.handling_status = 'PENDING' THEN 1 ELSE 0 END) pending_count,"
                + " SUM(CASE WHEN r.read_at IS NULL THEN 1 ELSE 0 END) unread_count,"
                + " SUM(CASE WHEN r.handling_status = 'PENDING' AND n.deadline < CURRENT_TIMESTAMP THEN 1 ELSE 0 END) overdue_count"
                + " FROM biz_notification n JOIN biz_notification_recipient r ON r.notification_id = n.id"
                + " WHERE r.recipient_user_id = ? AND r.archived = 0",
            userId);
    Map<String, Long> groups = new LinkedHashMap<>();
    groups.put(GROUP_ACTION, 0L);
    groups.put(GROUP_BUSINESS, 0L);
    groups.put(GROUP_SYSTEM, 0L);
    jdbcTemplate
        .queryForList(
            "SELECT n.group_type, COUNT(*) total FROM biz_notification n"
                + " JOIN biz_notification_recipient r ON r.notification_id = n.id"
                + " WHERE r.recipient_user_id = ? AND r.archived = 0 AND r.read_at IS NULL"
                + " GROUP BY n.group_type",
            userId)
        .forEach(
            group ->
                groups.put(
                    String.valueOf(group.get("group_type")), number(group.get("total"))));
    Map<String, Long> modules = new LinkedHashMap<>();
    Map<String, String> categories = new LinkedHashMap<>();
    jdbcTemplate
        .queryForList(
            "SELECT n.module_key, COUNT(*) total FROM biz_notification n"
                + " JOIN biz_notification_recipient r ON r.notification_id = n.id"
                + " WHERE r.recipient_user_id = ? AND r.archived = 0"
                + " GROUP BY n.module_key ORDER BY n.module_key",
            userId)
        .forEach(rowItem -> {
          String moduleKey = String.valueOf(rowItem.get("module_key"));
          modules.put(moduleKey, number(rowItem.get("total")));
          categories.put(moduleKey, MODULE_NAMES.getOrDefault(moduleKey, "业务消息"));
        });
    return new NotificationUnreadCounts(
        number(row.get("pending_count")),
        number(row.get("unread_count")),
        number(row.get("overdue_count")),
        groups,
        modules,
        categories);
  }

  @Transactional
  public void markRead(Long id) {
    updateReadState(id, true);
  }

  @Transactional
  public void markUnread(Long id) {
    updateReadState(id, false);
  }

  @Transactional
  public int markAllRead(String groupType, String moduleKey, LocalDate dateStart, LocalDate dateEnd) {
    Long userId = CurrentUserContext.require().userId();
    StringBuilder sql =
        new StringBuilder(
            "UPDATE biz_notification_recipient SET read_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP"
                + " WHERE recipient_user_id = :userId AND archived = 0 AND read_at IS NULL"
                + " AND notification_id IN (SELECT id FROM biz_notification WHERE 1 = 1");
    MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
    if (hasText(groupType)) {
      String normalized = normalizeGroup(groupType);
      sql.append(" AND group_type = :groupType");
      params.addValue("groupType", normalized);
    }
    if (hasText(moduleKey)) {
      sql.append(" AND module_key = :moduleKey");
      params.addValue("moduleKey", moduleKey.trim());
    }
    if (dateStart != null) {
      sql.append(" AND created_at >= :dateStart");
      params.addValue("dateStart", dateStart.atStartOfDay());
    }
    if (dateEnd != null) {
      sql.append(" AND created_at < :dateEndExclusive");
      params.addValue("dateEndExclusive", dateEnd.plusDays(1).atStartOfDay());
    }
    sql.append(")");
    return namedJdbcTemplate.update(sql.toString(), params);
  }

  @Transactional
  public Long publishToUser(NotificationCommand command, Long recipientUserId) {
    return publishToUser(command, recipientUserId, "ASSIGNEE");
  }

  @Transactional
  public Long publishToUser(NotificationCommand command, Long recipientUserId, String recipientReason) {
    if (recipientUserId == null) {
      return null;
    }
    validateCommand(command);
    Long notificationId = findNotificationId(command.dedupKey());
    LocalDateTime now = LocalDateTime.now();
    if (notificationId == null) {
      jdbcTemplate.update(
          "INSERT INTO biz_notification"
              + " (event_type, group_type, module_key, title, summary, biz_type, biz_id, severity, action_key,"
              + " deadline, dedup_key, source_event_id, organization_id, audience_type, snapshot_json, expires_at,"
              + " published_at, triggered_by, created_at, updated_at)"
              + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          command.eventType(),
          normalizeGroup(command.groupType()),
          command.moduleKey(),
          command.title(),
          command.summary(),
          command.bizType(),
          command.bizId(),
          normalizeSeverity(command.severity()),
          command.actionKey(),
          command.deadline(),
          command.dedupKey(),
          command.sourceEventId() == null ? command.dedupKey() : command.sourceEventId(),
          command.organizationId(),
          command.audienceType() == null ? "USER" : command.audienceType(),
          command.snapshotJson(),
          command.expiresAt(),
          now,
          command.triggeredBy(),
          now,
          now);
      notificationId = findNotificationId(command.dedupKey());
    }
    Integer exists =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM biz_notification_recipient WHERE notification_id = ? AND recipient_user_id = ?",
            Integer.class,
            notificationId,
            recipientUserId);
    if (exists != null && exists == 0) {
      String handling = GROUP_ACTION.equals(normalizeGroup(command.groupType())) ? HANDLING_PENDING : HANDLING_NONE;
      jdbcTemplate.update(
          "INSERT INTO biz_notification_recipient"
              + " (notification_id, recipient_user_id, handling_status, action_status, recipient_reason,"
              + " last_delivered_at, archived, created_at, updated_at)"
              + " VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?)",
          notificationId,
          recipientUserId,
          handling,
          GROUP_ACTION.equals(normalizeGroup(command.groupType())) ? "PENDING" : "HANDLED",
          hasText(recipientReason) ? recipientReason.trim().toUpperCase() : "ASSIGNEE",
          now,
          now,
          now);
    }
    return notificationId;
  }

  @Transactional
  public void markBusinessHandled(String bizType, Long bizId, Long recipientUserId) {
    if (recipientUserId == null) return;
    jdbcTemplate.update(
        "UPDATE biz_notification_recipient SET handling_status = 'HANDLED', handled_at = CURRENT_TIMESTAMP,"
            + " action_status = 'HANDLED',"
            + " updated_at = CURRENT_TIMESTAMP WHERE recipient_user_id = ? AND handling_status = 'PENDING'"
            + " AND notification_id IN (SELECT id FROM biz_notification WHERE biz_type = ? AND biz_id = ?)",
        recipientUserId,
        bizType,
        bizId);
  }

  @Transactional
  public void cancelBusinessActions(String bizType, Long bizId, String reason) {
    jdbcTemplate.update(
        "UPDATE biz_notification_recipient SET handling_status = 'HANDLED', action_status = 'CANCELLED',"
            + " cancelled_at = CURRENT_TIMESTAMP, cancel_reason = ?, updated_at = CURRENT_TIMESTAMP"
            + " WHERE action_status = 'PENDING' AND notification_id IN"
            + " (SELECT id FROM biz_notification WHERE biz_type = ? AND biz_id = ?)",
        reason == null ? "业务状态已变化" : reason,
        bizType,
        bizId);
  }

  public NotificationRouteResolution resolveAction(Long id) {
    NotificationListItem item = detail(id);
    return new NotificationRouteResolution(
        item.actionAvailable(),
        item.actionUnavailableReason(),
        item.actionAvailable() ? item.routeKey() : null,
        item.actionAvailable() ? item.routeParams() : Map.of());
  }

  private void appendFilters(
      StringBuilder where, MapSqlParameterSource params, NotificationQuery query) {
    if (hasText(query.groupType())) {
      where.append(" AND n.group_type = :groupType");
      params.addValue("groupType", normalizeGroup(query.groupType()));
    }
    if (hasText(query.moduleKey())) {
      where.append(" AND n.module_key = :moduleKey");
      params.addValue("moduleKey", query.moduleKey().trim());
    }
    if (hasText(query.category())) {
      where.append(" AND n.module_key = :category");
      params.addValue("category", query.category().trim());
    }
    if (Boolean.TRUE.equals(query.unread())) {
      where.append(" AND r.read_at IS NULL");
    } else if (Boolean.FALSE.equals(query.unread())) {
      where.append(" AND r.read_at IS NOT NULL");
    }
    if (hasText(query.handlingStatus())) {
      String normalized = query.handlingStatus().trim().toUpperCase();
      if (!HANDLING_STATUSES.contains(normalized)) {
        throw new BusinessException("不支持的消息处理状态");
      }
      where.append(" AND r.handling_status = :handlingStatus");
      params.addValue("handlingStatus", normalized);
    }
    if (hasText(query.actionStatus())) {
      String normalized = query.actionStatus().trim().toUpperCase();
      if (!Set.of("PENDING", "HANDLED", "CANCELLED", "EXPIRED").contains(normalized)) {
        throw new BusinessException("不支持的消息动作状态");
      }
      where.append(" AND r.action_status = :actionStatus");
      params.addValue("actionStatus", normalized);
    }
    if (query.dateStart() != null) {
      where.append(" AND n.created_at >= :dateStart");
      params.addValue("dateStart", query.dateStart().atStartOfDay());
    }
    if (query.dateEnd() != null) {
      where.append(" AND n.created_at < :dateEndExclusive");
      params.addValue("dateEndExclusive", query.dateEnd().plusDays(1).atStartOfDay());
    }
  }

  private void updateReadState(Long id, boolean read) {
    Long userId = CurrentUserContext.require().userId();
    int updated =
        jdbcTemplate.update(
            "UPDATE biz_notification_recipient SET read_at = "
                + (read ? "CURRENT_TIMESTAMP" : "NULL")
                + ", updated_at = CURRENT_TIMESTAMP WHERE notification_id = ? AND recipient_user_id = ? AND archived = 0",
            id,
            userId);
    if (updated == 0) {
      throw new BusinessException("消息不存在");
    }
  }

  private Long findNotificationId(String dedupKey) {
    List<Long> ids =
        jdbcTemplate.query(
            "SELECT id FROM biz_notification WHERE dedup_key = ?",
            (rs, rowNum) -> rs.getLong(1),
            dedupKey);
    return ids.isEmpty() ? null : ids.get(0);
  }

  private NotificationListItem mapItem(ResultSet rs, int rowNum) throws SQLException {
    LocalDateTime deadline = timestamp(rs, "deadline");
    String handling = rs.getString("handling_status");
    String moduleKey = rs.getString("module_key");
    String actionKey = rs.getString("action_key");
    String actionStatus = rs.getString("action_status");
    LocalDateTime expiresAt = timestamp(rs, "expires_at");
    LocalDateTime withdrawnAt = timestamp(rs, "withdrawn_at");
    boolean accountActive = rs.getObject("recipient_user_id") != null;
    String unavailableReason = null;
    if ("CANCELLED".equals(actionStatus)) {
      unavailableReason = valueOrDefault(rs.getString("cancel_reason"), "责任人或业务状态已变化");
    } else if ("HANDLED".equals(actionStatus) && GROUP_ACTION.equals(rs.getString("group_type"))) {
      unavailableReason = "该事项已处理";
    } else if ("EXPIRED".equals(actionStatus) || (expiresAt != null && expiresAt.isBefore(LocalDateTime.now()))) {
      unavailableReason = "该消息已过期";
    } else if (withdrawnAt != null) {
      unavailableReason = "该消息已撤回";
    } else if (!accountActive) {
      unavailableReason = "账号已停用或删除";
    }
    String routeKey = ROUTE_KEYS.get(moduleKey);
    boolean actionAvailable = actionKey != null && unavailableReason == null && routeKey != null;
    Map<String, String> routeParams = actionAvailable
        ? routeParams(moduleKey, String.valueOf(rs.getLong("biz_id")))
        : Map.of();
    return new NotificationListItem(
        String.valueOf(rs.getLong("id")),
        rs.getString("event_type"),
        rs.getString("group_type"),
        moduleKey,
        MODULE_NAMES.getOrDefault(moduleKey, "业务消息"),
        rs.getString("title"),
        rs.getString("summary"),
        rs.getString("team_name"),
        rs.getString("responsible_name"),
        rs.getString("biz_type"),
        String.valueOf(rs.getLong("biz_id")),
        rs.getString("severity"),
        actionKey,
        actionKey == null ? null : ACTION_LABELS.getOrDefault(actionKey, "去处理"),
        moduleKey,
        actionStatus,
        actionAvailable,
        unavailableReason,
        rs.getString("recipient_reason"),
        actionAvailable ? routeKey : null,
        routeParams,
        rs.getString("organization_name"),
        expiresAt,
        deadline,
        timestamp(rs, "created_at"),
        rs.getTimestamp("read_at") != null,
        handling,
        HANDLING_PENDING.equals(handling) && deadline != null && deadline.isBefore(LocalDateTime.now()));
  }

  private Map<String, String> routeParams(String moduleKey, String bizId) {
    Map<String, String> params = new LinkedHashMap<>();
    params.put("id", bizId);
    if (Set.of("pre-shift-inspection", "mid-shift-inspection", "post-shift-inspection").contains(moduleKey)) {
      params.put("moduleKey", moduleKey);
    }
    return params;
  }

  private String valueOrDefault(String value, String defaultValue) {
    return hasText(value) ? value : defaultValue;
  }

  private LocalDateTime timestamp(ResultSet rs, String column) throws SQLException {
    Timestamp value = rs.getTimestamp(column);
    return value == null ? null : value.toLocalDateTime();
  }

  private void validateCommand(NotificationCommand command) {
    if (command == null || !hasText(command.eventType()) || !hasText(command.moduleKey())
        || !hasText(command.title()) || !hasText(command.bizType()) || command.bizId() == null
        || !hasText(command.dedupKey())) {
      throw new IllegalArgumentException("消息事件参数不完整");
    }
    normalizeGroup(command.groupType());
  }

  private String normalizeGroup(String value) {
    String normalized = value == null ? "" : value.trim().toUpperCase();
    if (!GROUPS.contains(normalized)) {
      throw new BusinessException("不支持的消息分组");
    }
    return normalized;
  }

  private String normalizeSeverity(String value) {
    String normalized = value == null ? "NORMAL" : value.trim().toUpperCase();
    return Set.of("URGENT", "IMPORTANT", "NORMAL").contains(normalized) ? normalized : "NORMAL";
  }

  private long number(Object value) {
    return value instanceof Number number ? number.longValue() : 0L;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  public record NotificationCommand(
      String eventType,
      String groupType,
      String moduleKey,
      String title,
      String summary,
      String bizType,
      Long bizId,
      String severity,
      String actionKey,
      LocalDateTime deadline,
      String dedupKey,
      Long triggeredBy,
      String sourceEventId,
      Long organizationId,
      String audienceType,
      String snapshotJson,
      LocalDateTime expiresAt) {
    public NotificationCommand(
        String eventType,
        String groupType,
        String moduleKey,
        String title,
        String summary,
        String bizType,
        Long bizId,
        String severity,
        String actionKey,
        LocalDateTime deadline,
        String dedupKey,
        Long triggeredBy) {
      this(
          eventType, groupType, moduleKey, title, summary, bizType, bizId, severity, actionKey,
          deadline, dedupKey, triggeredBy, dedupKey, null, "USER", null, null);
    }
  }
}
