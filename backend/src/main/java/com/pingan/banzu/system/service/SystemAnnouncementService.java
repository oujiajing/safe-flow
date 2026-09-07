package com.pingan.banzu.system.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.dto.AnnouncementRequest;
import com.pingan.banzu.dto.AnnouncementResponse;
import com.pingan.banzu.dto.NotificationDeliveryResponse;
import com.pingan.banzu.security.CurrentUserContext;
import com.pingan.banzu.service.NotificationService;
import com.pingan.banzu.service.NotificationService.NotificationCommand;
import com.pingan.banzu.system.security.SystemPermissionService;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemAnnouncementService {

  private static final Set<String> AUDIENCE_TYPES = Set.of("ALL", "ORG", "ROLE", "USER");
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final SystemPermissionService permissionService;
  private final NotificationService notificationService;

  public SystemAnnouncementService(
      JdbcTemplate jdbcTemplate,
      ObjectMapper objectMapper,
      SystemPermissionService permissionService,
      NotificationService notificationService) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.permissionService = permissionService;
    this.notificationService = notificationService;
  }

  public PageResult<AnnouncementResponse> list(String status, Integer page, Integer pageSize) {
    permissionService.assertHasPermission("PINGAN_NOTIFICATION_ANNOUNCEMENT_MANAGE");
    int size = Math.min(100, Math.max(1, pageSize == null ? 20 : pageSize));
    int pageNumber = Math.max(1, page == null ? 1 : page);
    String where = status == null || status.isBlank() ? "" : " WHERE status = ?";
    Object[] arguments = where.isEmpty()
        ? new Object[] {size, (pageNumber - 1) * size}
        : new Object[] {status.trim().toUpperCase(), size, (pageNumber - 1) * size};
    Long total = where.isEmpty()
        ? jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_announcement", Long.class)
        : jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM biz_announcement" + where,
            Long.class,
            status.trim().toUpperCase());
    List<AnnouncementResponse> items = jdbcTemplate.query(
        "SELECT * FROM biz_announcement" + where + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
        (rs, rowNum) -> response(rs.getLong("id")),
        arguments);
    return new PageResult<>(items, total == null ? 0L : total);
  }

  public AnnouncementResponse get(Long id) {
    permissionService.assertHasPermission("PINGAN_NOTIFICATION_ANNOUNCEMENT_MANAGE");
    requireExists(id);
    return response(id);
  }

  @Transactional
  public AnnouncementResponse create(AnnouncementRequest request) {
    permissionService.assertHasPermission("PINGAN_NOTIFICATION_ANNOUNCEMENT_MANAGE");
    validate(request);
    Long id = insertDraft(request, 1, null);
    return response(id);
  }

  @Transactional
  public AnnouncementResponse update(Long id, AnnouncementRequest request) {
    permissionService.assertHasPermission("PINGAN_NOTIFICATION_ANNOUNCEMENT_MANAGE");
    validate(request);
    Map<String, Object> current = requireExists(id);
    if (!"DRAFT".equals(current.get("status"))) {
      int version = ((Number) current.get("version_no")).intValue() + 1;
      return response(insertDraft(request, version, id));
    }
    jdbcTemplate.update(
        "UPDATE biz_announcement SET title = ?, content = ?, severity = ?, audience_type = ?,"
            + " audience_json = ?, effective_at = ?, expires_at = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
        request.title().trim(),
        request.content().trim(),
        normalizeSeverity(request.severity()),
        normalizeAudience(request.audienceType()),
        audienceJson(request),
        request.effectiveAt(),
        request.expiresAt(),
        id);
    return response(id);
  }

  @Transactional
  public AnnouncementResponse publish(Long id) {
    permissionService.assertHasPermission("PINGAN_NOTIFICATION_ANNOUNCEMENT_PUBLISH");
    Map<String, Object> row = requireExists(id);
    if (!"DRAFT".equals(row.get("status"))) {
      throw new BusinessException("只有草稿公告可以发布");
    }
    LocalDateTime now = LocalDateTime.now();
    List<Long> recipients = resolveAudience(
        String.valueOf(row.get("audience_type")),
        parseAudience(String.valueOf(row.get("audience_json"))));
    jdbcTemplate.update(
        "UPDATE biz_announcement SET status = 'PUBLISHED', published_by = ?, published_at = ?,"
            + " updated_at = ? WHERE id = ?",
        CurrentUserContext.require().userId(),
        now,
        now,
        id);
    for (Long userId : recipients) {
      notificationService.publishToUser(
          new NotificationCommand(
              "SYSTEM_ANNOUNCEMENT",
              NotificationService.GROUP_SYSTEM,
              "system-announcement",
              String.valueOf(row.get("title")),
              summarize(String.valueOf(row.get("content"))),
              "ANNOUNCEMENT",
              id,
              String.valueOf(row.get("severity")),
              "VIEW",
              null,
              "announcement:" + id,
              CurrentUserContext.require().userId(),
              "announcement:" + id,
              null,
              String.valueOf(row.get("audience_type")),
              null,
              timestamp(row.get("expires_at"))),
          userId,
          "ANNOUNCEMENT");
    }
    return response(id);
  }

  @Transactional
  public AnnouncementResponse withdraw(Long id) {
    permissionService.assertHasPermission("PINGAN_NOTIFICATION_ANNOUNCEMENT_PUBLISH");
    Map<String, Object> row = requireExists(id);
    if (!"PUBLISHED".equals(row.get("status"))) {
      throw new BusinessException("只有已发布公告可以撤回");
    }
    LocalDateTime now = LocalDateTime.now();
    jdbcTemplate.update(
        "UPDATE biz_announcement SET status = 'WITHDRAWN', withdrawn_by = ?, withdrawn_at = ?,"
            + " updated_at = ? WHERE id = ?",
        CurrentUserContext.require().userId(),
        now,
        now,
        id);
    jdbcTemplate.update(
        "UPDATE biz_notification SET withdrawn_at = ?, updated_at = ?"
            + " WHERE biz_type = 'ANNOUNCEMENT' AND biz_id = ?",
        now,
        now,
        id);
    notificationService.cancelBusinessActions("ANNOUNCEMENT", id, "公告已撤回");
    return response(id);
  }

  public PageResult<NotificationDeliveryResponse> deliveries(
      Long announcementId, Integer page, Integer pageSize) {
    permissionService.assertHasPermission("PINGAN_NOTIFICATION_ANNOUNCEMENT_MANAGE");
    int size = Math.min(100, Math.max(1, pageSize == null ? 20 : pageSize));
    int pageNumber = Math.max(1, page == null ? 1 : page);
    Long total = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM biz_notification n JOIN biz_notification_recipient r"
            + " ON r.notification_id = n.id WHERE n.biz_type = 'ANNOUNCEMENT' AND n.biz_id = ?",
        Long.class,
        announcementId);
    List<NotificationDeliveryResponse> items = jdbcTemplate.query(
        """
        SELECT n.id notification_id, n.biz_id announcement_id, r.recipient_user_id,
               COALESCE(u.real_name, u.username) recipient_name, o.org_name,
               r.recipient_reason, r.read_at, r.last_delivered_at
        FROM biz_notification n
        JOIN biz_notification_recipient r ON r.notification_id = n.id
        LEFT JOIN sys_user u ON u.id = r.recipient_user_id
        LEFT JOIN sys_org o ON o.id = u.org_id
        WHERE n.biz_type = 'ANNOUNCEMENT' AND n.biz_id = ?
        ORDER BY r.id LIMIT ? OFFSET ?
        """,
        (rs, rowNum) ->
            new NotificationDeliveryResponse(
                String.valueOf(rs.getLong("notification_id")),
                String.valueOf(rs.getLong("announcement_id")),
                String.valueOf(rs.getLong("recipient_user_id")),
                rs.getString("recipient_name"),
                rs.getString("org_name"),
                rs.getString("recipient_reason"),
                rs.getTimestamp("read_at") != null,
                localDateTime(rs.getTimestamp("read_at")),
                localDateTime(rs.getTimestamp("last_delivered_at"))),
        announcementId,
        size,
        (pageNumber - 1) * size);
    return new PageResult<>(items, total == null ? 0L : total);
  }

  private Long insertDraft(AnnouncementRequest request, int version, Long previousVersionId) {
    GeneratedKeyHolder keys = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement statement = connection.prepareStatement(
              "INSERT INTO biz_announcement"
                  + " (version_no, previous_version_id, title, content, severity, audience_type,"
                  + " audience_json, status, effective_at, expires_at, created_by, created_at, updated_at)"
                  + " VALUES (?, ?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
              new String[] {"id"});
          statement.setInt(1, version);
          statement.setObject(2, previousVersionId);
          statement.setString(3, request.title().trim());
          statement.setString(4, request.content().trim());
          statement.setString(5, normalizeSeverity(request.severity()));
          statement.setString(6, normalizeAudience(request.audienceType()));
          statement.setString(7, audienceJson(request));
          statement.setObject(8, request.effectiveAt());
          statement.setObject(9, request.expiresAt());
          statement.setLong(10, CurrentUserContext.require().userId());
          return statement;
        },
        keys);
    Number key = keys.getKey();
    if (key == null) throw new BusinessException("公告创建失败");
    return key.longValue();
  }

  private AnnouncementResponse response(Long id) {
    return jdbcTemplate.queryForObject(
        """
        SELECT a.*,
          (SELECT COUNT(*) FROM biz_notification n JOIN biz_notification_recipient r
             ON r.notification_id = n.id
           WHERE n.biz_type = 'ANNOUNCEMENT' AND n.biz_id = a.id) delivery_count,
          (SELECT COUNT(*) FROM biz_notification n JOIN biz_notification_recipient r
             ON r.notification_id = n.id
           WHERE n.biz_type = 'ANNOUNCEMENT' AND n.biz_id = a.id AND r.read_at IS NOT NULL) read_count
        FROM biz_announcement a WHERE a.id = ?
        """,
        (rs, rowNum) -> {
          Audience audience = parseAudience(rs.getString("audience_json"));
          return new AnnouncementResponse(
              String.valueOf(rs.getLong("id")),
              rs.getInt("version_no"),
              rs.getObject("previous_version_id") == null
                  ? null
                  : String.valueOf(rs.getLong("previous_version_id")),
              rs.getString("title"),
              rs.getString("content"),
              rs.getString("severity"),
              rs.getString("audience_type"),
              audience.organizationIds(),
              audience.roleIds(),
              audience.userIds(),
              rs.getString("status"),
              localDateTime(rs.getTimestamp("effective_at")),
              localDateTime(rs.getTimestamp("expires_at")),
              localDateTime(rs.getTimestamp("published_at")),
              localDateTime(rs.getTimestamp("withdrawn_at")),
              rs.getLong("delivery_count"),
              rs.getLong("read_count"),
              localDateTime(rs.getTimestamp("created_at")),
              localDateTime(rs.getTimestamp("updated_at")));
        },
        id);
  }

  private List<Long> resolveAudience(String type, Audience audience) {
    String sql = switch (type) {
      case "ALL" -> "SELECT id FROM sys_user WHERE deleted = 0 AND status = 'ACTIVE'";
      case "USER" -> inQuery("SELECT id FROM sys_user WHERE deleted = 0 AND status = 'ACTIVE' AND id IN ", audience.userIds());
      case "ROLE" -> inQuery(
          "SELECT DISTINCT u.id FROM sys_user u JOIN sys_user_role ur ON ur.user_id = u.id"
              + " WHERE u.deleted = 0 AND u.status = 'ACTIVE' AND ur.role_id IN ",
          audience.roleIds());
      case "ORG" -> inQuery(
          "SELECT DISTINCT u.id FROM sys_user u JOIN sys_org uo ON uo.id = u.org_id"
              + " JOIN sys_org selected ON uo.org_path LIKE CONCAT(selected.org_path, '%')"
              + " WHERE u.deleted = 0 AND u.status = 'ACTIVE' AND selected.id IN ",
          audience.organizationIds());
      default -> throw new BusinessException("公告受众类型不合法");
    };
    return jdbcTemplate.queryForList(sql, Long.class);
  }

  private String inQuery(String prefix, List<Long> ids) {
    if (ids == null || ids.isEmpty()) return "SELECT id FROM sys_user WHERE 1 = 0";
    return prefix + "(" + String.join(",", ids.stream().map(String::valueOf).toList()) + ")";
  }

  private Map<String, Object> requireExists(Long id) {
    List<Map<String, Object>> rows =
        jdbcTemplate.queryForList("SELECT * FROM biz_announcement WHERE id = ?", id);
    if (rows.isEmpty()) throw new BusinessException("公告不存在");
    Map<String, Object> normalized = new LinkedHashMap<>();
    rows.get(0).forEach((key, value) -> normalized.put(key.toLowerCase(), value));
    return normalized;
  }

  private void validate(AnnouncementRequest request) {
    if (request == null || request.title() == null || request.title().isBlank()
        || request.content() == null || request.content().isBlank()) {
      throw new BusinessException("公告标题和正文不能为空");
    }
    String audienceType = normalizeAudience(request.audienceType());
    if ("ORG".equals(audienceType) && empty(request.organizationIds())
        || "ROLE".equals(audienceType) && empty(request.roleIds())
        || "USER".equals(audienceType) && empty(request.userIds())) {
      throw new BusinessException("公告受众不能为空");
    }
    if (request.effectiveAt() != null && request.expiresAt() != null
        && !request.expiresAt().isAfter(request.effectiveAt())) {
      throw new BusinessException("公告失效时间必须晚于生效时间");
    }
  }

  private boolean empty(List<Long> values) {
    return values == null || values.isEmpty();
  }

  private String audienceJson(AnnouncementRequest request) {
    try {
      return objectMapper.writeValueAsString(new Audience(
          distinct(request.organizationIds()), distinct(request.roleIds()), distinct(request.userIds())));
    } catch (JsonProcessingException ex) {
      throw new BusinessException("公告受众保存失败");
    }
  }

  private Audience parseAudience(String json) {
    if (json == null || json.isBlank() || "null".equals(json)) return new Audience(List.of(), List.of(), List.of());
    try {
      return objectMapper.readValue(json, new TypeReference<Audience>() {});
    } catch (JsonProcessingException ex) {
      throw new BusinessException("公告受众数据损坏");
    }
  }

  private List<Long> distinct(List<Long> values) {
    return values == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(values));
  }

  private String normalizeAudience(String value) {
    String normalized = value == null ? "" : value.trim().toUpperCase();
    if (!AUDIENCE_TYPES.contains(normalized)) throw new BusinessException("公告受众类型不合法");
    return normalized;
  }

  private String normalizeSeverity(String value) {
    String normalized = value == null ? "NORMAL" : value.trim().toUpperCase();
    return Set.of("NORMAL", "IMPORTANT", "URGENT").contains(normalized) ? normalized : "NORMAL";
  }

  private String summarize(String content) {
    return content.length() <= 160 ? content : content.substring(0, 157) + "...";
  }

  private LocalDateTime timestamp(Object value) {
    return value instanceof Timestamp timestamp ? timestamp.toLocalDateTime() : null;
  }

  private LocalDateTime localDateTime(Timestamp value) {
    return value == null ? null : value.toLocalDateTime();
  }

  private record Audience(
      List<Long> organizationIds, List<Long> roleIds, List<Long> userIds) {}
}
