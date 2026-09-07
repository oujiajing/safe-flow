package com.pingan.banzu.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationRecipientResolver {

  private final JdbcTemplate jdbcTemplate;
  private final NotificationPermissionPolicy permissionPolicy;

  public NotificationRecipientResolver(
      JdbcTemplate jdbcTemplate, NotificationPermissionPolicy permissionPolicy) {
    this.jdbcTemplate = jdbcTemplate;
    this.permissionPolicy = permissionPolicy;
  }

  public Map<Long, String> resolve(DomainNotificationEvent event) {
    Map<Long, String> recipients = new LinkedHashMap<>();
    addRecipients(
        recipients,
        event.responsibleUserIds(),
        "ASSIGNEE",
        event.eventType() != null
            && (event.eventType().startsWith("ACCOUNT_")
                || event.eventType().equals("ROLE_PERMISSION_CHANGED")
                || event.eventType().equals("SESSION_REVOKED")));
    if (recipients.isEmpty() || event.eventType().endsWith("_OVERDUE")) {
      String permission = permissionPolicy.requiredPermission(event.eventType());
      if (permission != null && event.organizationId() != null) {
        resolvePermissionScope(permission, event.organizationId())
            .forEach(userId -> recipients.putIfAbsent(userId, "PERMISSION_SCOPE"));
      }
    }
    if (recipients.isEmpty() && isResultEvent(event.eventType()) && event.creatorUserId() != null) {
      addRecipients(recipients, List.of(event.creatorUserId()), "CREATOR", false);
    }
    if (event.excludedUserIds() != null) {
      event.excludedUserIds().forEach(recipients::remove);
    }
    return recipients;
  }

  private void addRecipients(
      Map<Long, String> target,
      Collection<Long> userIds,
      String reason,
      boolean includeInactive) {
    if (userIds == null || userIds.isEmpty()) return;
    String placeholders = String.join(",", java.util.Collections.nCopies(userIds.size(), "?"));
    jdbcTemplate
        .queryForList(
            "SELECT id FROM sys_user WHERE deleted = 0"
                + (includeInactive ? "" : " AND status = 'ACTIVE'")
                + " AND id IN (" + placeholders + ")",
            Long.class,
            userIds.toArray())
        .forEach(userId -> target.putIfAbsent(userId, reason));
  }

  private List<Long> resolvePermissionScope(String permission, Long organizationId) {
    return jdbcTemplate.queryForList(
        """
        SELECT DISTINCT u.id
        FROM sys_user u
        JOIN sys_org user_org ON user_org.id = u.org_id AND user_org.deleted = 0
        JOIN sys_user_role ur ON ur.user_id = u.id
        JOIN sys_role role ON role.id = ur.role_id AND role.deleted = 0
        JOIN sys_role_menu rm ON rm.role_id = role.id
        JOIN sys_menu menu ON menu.id = rm.menu_id
        JOIN sys_org business_org ON business_org.id = ?
        WHERE u.deleted = 0
          AND u.status = 'ACTIVE'
          AND menu.permission_code = ?
          AND menu.status = 'ACTIVE'
          AND menu.deleted = 0
          AND (
            role.data_scope = 'ALL'
            OR (role.data_scope = 'ORG_AND_CHILDREN' AND business_org.org_path LIKE CONCAT(user_org.org_path, '%'))
            OR (role.data_scope = 'SELF' AND u.org_id = business_org.id)
          )
        ORDER BY u.id
        """,
        Long.class,
        organizationId,
        permission);
  }

  private boolean isResultEvent(String eventType) {
    return eventType.endsWith("_REJECTED")
        || eventType.endsWith("_COMPLETED")
        || eventType.endsWith("_SUBMITTED")
        || eventType.endsWith("_PUBLISHED")
        || eventType.endsWith("_CANCELLED");
  }
}
