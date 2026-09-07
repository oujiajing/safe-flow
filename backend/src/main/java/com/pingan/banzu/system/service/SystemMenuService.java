package com.pingan.banzu.system.service;

import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.system.audit.AuditLogService;
import com.pingan.banzu.system.common.SystemModule;
import com.pingan.banzu.system.domain.SysMenu;
import com.pingan.banzu.system.dto.SystemMenuRequest;
import com.pingan.banzu.system.dto.SystemMenuResponse;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemMenuService {

  private final JdbcTemplate jdbcTemplate;
  private final AuditLogService auditLogService;

  public SystemMenuService(JdbcTemplate jdbcTemplate, AuditLogService auditLogService) {
    this.jdbcTemplate = jdbcTemplate;
    this.auditLogService = auditLogService;
  }

  public List<SystemMenuResponse> list(String keyword, String status) {
    return activeMenus().stream()
        .filter(menu -> matchesStatus(menu.status, status))
        .filter(
            menu ->
                containsAny(
                    keyword,
                    menu.menuCode,
                    menu.title,
                    menu.routePath,
                    menu.component,
                    menu.permissionCode))
        .map(menu -> toResponse(menu, List.of()))
        .toList();
  }

  public List<SystemMenuResponse> tree() {
    List<SysMenu> menus = activeMenus();
    Map<Long, List<SysMenu>> byParent =
        menus.stream().collect(Collectors.groupingBy(menu -> menu.parentId == null ? 0L : menu.parentId));
    return toTree(0L, byParent);
  }

  public List<Map<String, Object>> vbenRouteTree() {
    List<SysMenu> menus = routeMenusForCurrentUser();
    Map<Long, List<SysMenu>> byParent =
        menus.stream().collect(Collectors.groupingBy(menu -> menu.parentId == null ? 0L : menu.parentId));
    return toRouteTree(0L, byParent);
  }

  @Transactional
  public SystemMenuResponse create(SystemMenuRequest request) {
    SysMenu menu = new SysMenu();
    apply(menu, request);
    menu.deleted = 0;
    jdbcTemplate.update(
        """
        insert into sys_menu
          (parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status, deleted)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
        """,
        menu.parentId,
        menu.menuCode,
        menu.title,
        menu.routePath,
        menu.component,
        menu.icon,
        menu.permissionCode,
        menu.sortOrder,
        menu.visible,
        menu.status);
    menu.id = jdbcTemplate.queryForObject("select max(id) from sys_menu where menu_code = ?", Long.class, menu.menuCode);
    auditLogService.record(SystemModule.MENU, "SYS_MENU", menu.id, "CREATE", "新增菜单 " + menu.title);
    return toResponse(menu, List.of());
  }

  @Transactional
  public SystemMenuResponse update(Long id, SystemMenuRequest request) {
    SysMenu menu = requireMenu(id);
    apply(menu, request);
    jdbcTemplate.update(
        """
        update sys_menu
        set parent_id = ?, menu_code = ?, title = ?, route_path = ?, component = ?, icon = ?,
            permission_code = ?, sort_order = ?, visible = ?, status = ?, updated_at = CURRENT_TIMESTAMP
        where id = ?
        """,
        menu.parentId,
        menu.menuCode,
        menu.title,
        menu.routePath,
        menu.component,
        menu.icon,
        menu.permissionCode,
        menu.sortOrder,
        menu.visible,
        menu.status,
        id);
    auditLogService.record(SystemModule.MENU, "SYS_MENU", id, "UPDATE", "更新菜单 " + menu.title);
    return toResponse(menu, List.of());
  }

  @Transactional
  public void delete(Long id) {
    SysMenu menu = requireMenu(id);
    menu.deleted = 1;
    jdbcTemplate.update("update sys_menu set deleted = 1, updated_at = CURRENT_TIMESTAMP where id = ?", id);
    auditLogService.record(SystemModule.MENU, "SYS_MENU", id, "DELETE", "删除菜单 " + menu.title);
  }

  private SysMenu requireMenu(Long id) {
    List<SysMenu> menus =
        jdbcTemplate.query("select * from sys_menu where id = ? and deleted = 0", this::mapMenu, id);
    SysMenu menu = menus.isEmpty() ? null : menus.get(0);
    if (menu == null || Integer.valueOf(1).equals(menu.deleted)) {
      throw new BusinessException("菜单不存在");
    }
    return menu;
  }

  private List<SysMenu> activeMenus() {
    return jdbcTemplate.query("select * from sys_menu where deleted = 0 order by sort_order, id", this::mapMenu);
  }

  private List<SysMenu> routeMenusForCurrentUser() {
    CurrentUser user = CurrentUserContext.require();
    if (user.isAdmin()) {
      return jdbcTemplate.query(
          """
          select *
          from sys_menu
          where deleted = 0 and status = 'ACTIVE' and visible = 1
          order by sort_order, id
          """,
          this::mapMenu);
    }
    return jdbcTemplate.query(
        """
        select distinct m.*
        from sys_menu m
        join sys_role_menu rm on rm.menu_id = m.id
        join sys_user_role ur on ur.role_id = rm.role_id
        where ur.user_id = ?
          and m.deleted = 0
          and m.status = 'ACTIVE'
          and m.visible = 1
        order by m.sort_order, m.id
        """,
        this::mapMenu,
        user.userId());
  }

  private SysMenu mapMenu(ResultSet rs, int rowNum) throws SQLException {
    SysMenu menu = new SysMenu();
    menu.id = rs.getLong("id");
    long parentId = rs.getLong("parent_id");
    menu.parentId = rs.wasNull() ? null : parentId;
    menu.menuCode = rs.getString("menu_code");
    menu.title = rs.getString("title");
    menu.routePath = rs.getString("route_path");
    menu.component = rs.getString("component");
    menu.icon = rs.getString("icon");
    menu.permissionCode = rs.getString("permission_code");
    menu.sortOrder = rs.getInt("sort_order");
    menu.visible = rs.getInt("visible");
    menu.status = rs.getString("status");
    menu.deleted = rs.getInt("deleted");
    return menu;
  }

  private void apply(SysMenu menu, SystemMenuRequest request) {
    menu.parentId = request.parentId();
    menu.menuCode = request.menuCode();
    menu.title = request.title();
    menu.routePath = request.routePath();
    menu.component = request.component();
    menu.icon = request.icon();
    menu.permissionCode = request.permissionCode();
    menu.sortOrder = request.sortOrder() == null ? 0 : request.sortOrder();
    menu.visible = Boolean.FALSE.equals(request.visible()) ? 0 : 1;
    menu.status = request.status() == null ? "ACTIVE" : request.status();
  }

  private List<SystemMenuResponse> toTree(Long parentId, Map<Long, List<SysMenu>> byParent) {
    return byParent.getOrDefault(parentId, List.of()).stream()
        .map(menu -> toResponse(menu, toTree(menu.id, byParent)))
        .toList();
  }

  private List<Map<String, Object>> toRouteTree(Long parentId, Map<Long, List<SysMenu>> byParent) {
    return byParent.getOrDefault(parentId, List.of()).stream()
        .map(menu -> toRoute(menu, toRouteTree(menu.id, byParent)))
        .toList();
  }

  private Map<String, Object> toRoute(SysMenu menu, List<Map<String, Object>> children) {
    Map<String, Object> route = new LinkedHashMap<>();
    route.put("name", routeName(menu.menuCode));
    route.put("path", menu.routePath);
    route.put("component", "LAYOUT".equalsIgnoreCase(menu.component) ? "BasicLayout" : menu.component);
    if (!children.isEmpty()) {
      route.put("redirect", children.get(0).get("path"));
      route.put("children", children);
    }
    Map<String, Object> meta = new LinkedHashMap<>();
    meta.put("title", menu.title);
    if (menu.icon != null && !menu.icon.isBlank()) {
      meta.put("icon", menu.icon);
    }
    meta.put("order", menu.sortOrder);
    route.put("meta", meta);
    return route;
  }

  private String routeName(String menuCode) {
    StringBuilder builder = new StringBuilder();
    for (String part : menuCode.toLowerCase().split("_")) {
      if (part.isBlank()) {
        continue;
      }
      builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
    }
    return builder.toString();
  }

  private boolean containsAny(String keyword, String... values) {
    if (keyword == null || keyword.isBlank()) {
      return true;
    }
    String normalized = keyword.trim();
    for (String value : values) {
      if (value != null && value.contains(normalized)) {
        return true;
      }
    }
    return false;
  }

  private boolean matchesStatus(String actual, String expected) {
    return expected == null || expected.isBlank() || "all".equals(expected) || expected.equals(actual);
  }

  private SystemMenuResponse toResponse(SysMenu menu, List<SystemMenuResponse> children) {
    return new SystemMenuResponse(
        menu.id,
        menu.parentId,
        menu.menuCode,
        menu.title,
        menu.routePath,
        menu.component,
        menu.icon,
        menu.permissionCode,
        menu.sortOrder,
        Integer.valueOf(1).equals(menu.visible),
        menu.status,
        children);
  }
}
