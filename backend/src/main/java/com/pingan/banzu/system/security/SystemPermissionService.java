package com.pingan.banzu.system.security;

import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import java.util.Arrays;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SystemPermissionService {

  private final JdbcTemplate jdbcTemplate;

  public SystemPermissionService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public boolean hasPermission(String permissionCode) {
    CurrentUser user = CurrentUserContext.require();
    if (user.isAdmin()) {
      return true;
    }
    return hasDirectPermission(user.userId(), permissionCode)
        || hasDescendantPermission(user.userId(), permissionCode);
  }

  private boolean hasDirectPermission(Long userId, String permissionCode) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from sys_user_role ur
            join sys_role_menu rm on rm.role_id = ur.role_id
            join sys_menu m on m.id = rm.menu_id
            where ur.user_id = ?
              and m.permission_code = ?
              and m.status = 'ACTIVE'
              and m.deleted = 0
            """,
            Integer.class,
            userId,
            permissionCode);
    return count != null && count > 0;
  }

  private boolean hasDescendantPermission(Long userId, String ancestorPermissionCode) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            with recursive permission_tree(id) as (
              select id
              from sys_menu
              where permission_code = ?
                and status = 'ACTIVE'
                and deleted = 0
              union all
              select child.id
              from sys_menu child
              join permission_tree parent on child.parent_id = parent.id
              where child.status = 'ACTIVE'
                and child.deleted = 0
            )
            select count(*)
            from sys_user_role ur
            join sys_role_menu rm on rm.role_id = ur.role_id
            join permission_tree tree on tree.id = rm.menu_id
            where ur.user_id = ?
            """,
            Integer.class,
            ancestorPermissionCode,
            userId);
    return count != null && count > 0;
  }

  public boolean hasAnyPermission(String... permissionCodes) {
    return Arrays.stream(permissionCodes).anyMatch(this::hasPermission);
  }

  public void assertHasPermission(String permissionCode) {
    if (!hasPermission(permissionCode)) {
      throw new SecurityException("无权执行该操作");
    }
  }

  public void assertHasAnyPermission(String... permissionCodes) {
    if (!hasAnyPermission(permissionCodes)) {
      throw new SecurityException("无权执行该操作");
    }
  }
}
