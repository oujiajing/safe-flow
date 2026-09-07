package com.pingan.banzu.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.domain.SysRole;
import com.pingan.banzu.mapper.SysRoleMapper;
import com.pingan.banzu.system.audit.AuditLogService;
import com.pingan.banzu.system.common.SystemModule;
import com.pingan.banzu.system.dto.SystemRoleRequest;
import com.pingan.banzu.system.dto.SystemRoleResponse;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemRoleService {

  private final SysRoleMapper roleMapper;
  private final JdbcTemplate jdbcTemplate;
  private final AuditLogService auditLogService;

  public SystemRoleService(SysRoleMapper roleMapper, JdbcTemplate jdbcTemplate, AuditLogService auditLogService) {
    this.roleMapper = roleMapper;
    this.jdbcTemplate = jdbcTemplate;
    this.auditLogService = auditLogService;
  }

  public List<SystemRoleResponse> list(String keyword) {
    return roleMapper.selectList(new QueryWrapper<SysRole>().eq("deleted", 0).orderByAsc("id")).stream()
        .filter(role -> containsAny(keyword, role.roleCode, role.roleName, role.dataScope))
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public SystemRoleResponse create(SystemRoleRequest request) {
    SysRole role = new SysRole();
    apply(role, request);
    role.deleted = 0;
    roleMapper.insert(role);
    auditLogService.record(SystemModule.ROLE, "SYS_ROLE", role.id, "CREATE", "新增角色 " + role.roleCode);
    return toResponse(role);
  }

  @Transactional
  public SystemRoleResponse update(Long id, SystemRoleRequest request) {
    SysRole role = requireRole(id);
    apply(role, request);
    roleMapper.updateById(role);
    auditLogService.record(SystemModule.ROLE, "SYS_ROLE", id, "UPDATE", "更新角色 " + role.roleCode);
    return toResponse(role);
  }

  @Transactional
  public void delete(Long id) {
    SysRole role = requireRole(id);
    role.deleted = 1;
    roleMapper.updateById(role);
    auditLogService.record(SystemModule.ROLE, "SYS_ROLE", id, "DELETE", "删除角色 " + role.roleCode);
  }

  public List<Long> menuIds(Long roleId) {
    requireRole(roleId);
    return jdbcTemplate.queryForList("select menu_id from sys_role_menu where role_id = ?", Long.class, roleId);
  }

  @Transactional
  public List<Long> updateMenus(Long roleId, List<Long> menuIds) {
    requireRole(roleId);
    List<Long> normalizedMenuIds = normalizeMenuIds(menuIds);
    validateMenuIds(normalizedMenuIds);
    List<Long> expandedMenuIds = expandMenuIdsWithDescendants(normalizedMenuIds);
    jdbcTemplate.update("delete from sys_role_menu where role_id = ?", roleId);
    for (Long menuId : expandedMenuIds) {
      jdbcTemplate.update("insert into sys_role_menu (role_id, menu_id) values (?, ?)", roleId, menuId);
    }
    auditLogService.record(SystemModule.ROLE, "SYS_ROLE", roleId, "ROLE_GRANT", "更新角色菜单授权");
    return menuIds(roleId);
  }

  private SysRole requireRole(Long id) {
    SysRole role = roleMapper.selectById(id);
    if (role == null || Integer.valueOf(1).equals(role.deleted)) {
      throw new BusinessException("角色不存在");
    }
    return role;
  }

  private void apply(SysRole role, SystemRoleRequest request) {
    role.roleCode = request.roleCode();
    role.roleName = request.roleName();
    role.dataScope = request.dataScope();
  }

  private SystemRoleResponse toResponse(SysRole role) {
    return new SystemRoleResponse(role.id, role.roleCode, role.roleName, role.dataScope);
  }

  private List<Long> normalizeMenuIds(List<Long> menuIds) {
    if (menuIds == null || menuIds.isEmpty()) {
      return List.of();
    }
    Set<Long> uniqueIds = new LinkedHashSet<>();
    for (Long menuId : menuIds) {
      if (menuId == null) {
        throw new BusinessException("菜单授权包含无效菜单");
      }
      uniqueIds.add(menuId);
    }
    return new ArrayList<>(uniqueIds);
  }

  private void validateMenuIds(List<Long> menuIds) {
    if (menuIds.isEmpty()) {
      return;
    }
    String placeholders = String.join(",", java.util.Collections.nCopies(menuIds.size(), "?"));
    Integer existingCount =
        jdbcTemplate.queryForObject(
            "select count(*) from sys_menu where deleted = 0 and status = 'ACTIVE' and id in (" + placeholders + ")",
            Integer.class,
            menuIds.toArray());
    if (existingCount == null || existingCount != menuIds.size()) {
      throw new BusinessException("菜单授权包含无效菜单");
    }
  }

  private List<Long> expandMenuIdsWithDescendants(List<Long> menuIds) {
    if (menuIds.isEmpty()) {
      return List.of();
    }
    String placeholders = String.join(",", java.util.Collections.nCopies(menuIds.size(), "?"));
    List<Long> expandedIds =
        jdbcTemplate.queryForList(
            """
            with recursive selected_tree(id) as (
              select id
              from sys_menu
              where deleted = 0
                and status = 'ACTIVE'
                and id in (%s)
              union all
              select child.id
              from sys_menu child
              join selected_tree parent on child.parent_id = parent.id
              where child.deleted = 0
                and child.status = 'ACTIVE'
            )
            select distinct id
            from selected_tree
            order by id
            """
                .formatted(placeholders),
            Long.class,
            menuIds.toArray());
    return new ArrayList<>(expandedIds);
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
}
