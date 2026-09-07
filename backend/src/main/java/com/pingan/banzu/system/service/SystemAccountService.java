package com.pingan.banzu.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.domain.SysOrg;
import com.pingan.banzu.domain.SysRole;
import com.pingan.banzu.domain.SysUser;
import com.pingan.banzu.domain.SysUserRole;
import com.pingan.banzu.mapper.SysOrgMapper;
import com.pingan.banzu.mapper.SysRoleMapper;
import com.pingan.banzu.mapper.SysUserMapper;
import com.pingan.banzu.mapper.SysUserRoleMapper;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import com.pingan.banzu.security.PasswordService;
import com.pingan.banzu.service.DomainNotificationEvent;
import com.pingan.banzu.service.NotificationOutboxService;
import com.pingan.banzu.system.audit.AuditLogService;
import com.pingan.banzu.system.common.SystemModule;
import com.pingan.banzu.system.dto.SystemAccountQuery;
import com.pingan.banzu.system.dto.SystemAccountRequest;
import com.pingan.banzu.system.dto.SystemAccountResponse;
import com.pingan.banzu.system.dto.SystemRoleResponse;
import com.pingan.banzu.system.security.SystemDataScopeService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemAccountService {

  private final SysUserMapper userMapper;
  private final SysOrgMapper orgMapper;
  private final SysRoleMapper roleMapper;
  private final SysUserRoleMapper userRoleMapper;
  private final PasswordService passwordService;
  private final SystemDataScopeService dataScopeService;
  private final AuditLogService auditLogService;
  private final JdbcTemplate jdbcTemplate;
  private final NotificationOutboxService notificationOutboxService;

  public SystemAccountService(
      SysUserMapper userMapper,
      SysOrgMapper orgMapper,
      SysRoleMapper roleMapper,
      SysUserRoleMapper userRoleMapper,
      PasswordService passwordService,
      SystemDataScopeService dataScopeService,
      AuditLogService auditLogService,
      JdbcTemplate jdbcTemplate,
      NotificationOutboxService notificationOutboxService) {
    this.userMapper = userMapper;
    this.orgMapper = orgMapper;
    this.roleMapper = roleMapper;
    this.userRoleMapper = userRoleMapper;
    this.passwordService = passwordService;
    this.dataScopeService = dataScopeService;
    this.auditLogService = auditLogService;
    this.jdbcTemplate = jdbcTemplate;
    this.notificationOutboxService = notificationOutboxService;
  }

  public PageResult<SystemAccountResponse> list(SystemAccountQuery query) {
    QueryWrapper<SysUser> wrapper = new QueryWrapper<SysUser>().eq("deleted", 0).orderByDesc("id");
    CurrentUser currentUser = CurrentUserContext.require();
    if (query.keyword != null && !query.keyword.isBlank()) {
      wrapper.and(
          w ->
              w.like("username", query.keyword)
                  .or()
                  .like("real_name", query.keyword)
                  .or()
                  .like("mobile", query.keyword));
    }
    if (query.status != null && !query.status.isBlank()) {
      wrapper.eq("status", query.status);
    }
    if (query.organizationId != null) {
      dataScopeService.assertCanAccessOrg(query.organizationId);
      List<Long> orgIds = descendantOrgIds(query.organizationId);
      if (orgIds.isEmpty()) {
        wrapper.eq("org_id", -1L);
      } else {
        wrapper.in("org_id", orgIds);
      }
    } else if (query.orgId != null) {
      dataScopeService.assertCanAccessOrg(query.orgId);
      wrapper.eq("org_id", query.orgId);
    } else {
      dataScopeService.applyOrgScope(wrapper, "org_id");
    }
    if ("SELF".equals(dataScopeService.currentDataScope())) {
      wrapper.eq("id", currentUser.userId());
    }
    List<SysUser> users = userMapper.selectList(wrapper);
    int page = Math.max(query.page, 1);
    int pageSize = Math.max(query.pageSize, 1);
    int from = Math.min((page - 1) * pageSize, users.size());
    int to = Math.min(from + pageSize, users.size());
    return new PageResult<>(users.subList(from, to).stream().map(this::toResponse).toList(), users.size());
  }

  @Transactional
  public SystemAccountResponse create(SystemAccountRequest request) {
    dataScopeService.assertCanAccessOrg(request.orgId());
    SysUser exists =
        userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", request.username()).eq("deleted", 0));
    if (exists != null) {
      throw new BusinessException("用户名已存在");
    }
    SysUser user = new SysUser();
    user.username = request.username();
    user.passwordHash = passwordService.hash(request.password() == null ? "SAFE_TEST_PASSWORD" : request.password());
    user.realName = request.realName();
    user.mobile = request.mobile();
    user.orgId = request.orgId();
    user.status = request.status() == null ? "ACTIVE" : request.status();
    user.deleted = 0;
    userMapper.insert(user);
    replaceRoles(user.id, request.roleIds());
    auditLogService.record(SystemModule.ACCOUNT, "SYS_USER", user.id, "CREATE", "新增账号 " + user.username);
    return toResponse(userMapper.selectById(user.id));
  }

  @Transactional
  public SystemAccountResponse update(Long id, SystemAccountRequest request) {
    SysUser user = requireUser(id);
    String previousStatus = user.status;
    dataScopeService.assertCanAccessOrg(user.orgId);
    dataScopeService.assertCanAccessOrg(request.orgId());
    user.realName = request.realName();
    user.mobile = request.mobile();
    user.orgId = request.orgId();
    user.status = request.status() == null ? user.status : request.status();
    userMapper.updateById(user);
    if (request.roleIds() != null) {
      replaceRoles(id, request.roleIds());
      long version = revokeSessions(id);
      publishSecurityEvent(
          "ROLE_PERMISSION_CHANGED", user, version, "账号角色已由管理员调整");
      publishSecurityEvent("SESSION_REVOKED", user, version, "角色权限变更，请重新登录");
    }
    if (!java.util.Objects.equals(previousStatus, user.status)) {
      long version =
          request.roleIds() == null ? revokeSessions(id) : currentAuthVersion(id);
      publishSecurityEvent(
          "ACCOUNT_STATUS_CHANGED", user, version, "账号状态：" + user.status);
      if (request.roleIds() == null) {
        publishSecurityEvent("SESSION_REVOKED", user, version, "账号状态变更，会话已失效");
      }
    }
    auditLogService.record(SystemModule.ACCOUNT, "SYS_USER", id, "UPDATE", "更新账号 " + user.username);
    return toResponse(userMapper.selectById(id));
  }

  @Transactional
  public void delete(Long id) {
    SysUser user = requireUser(id);
    dataScopeService.assertCanAccessOrg(user.orgId);
    user.deleted = 1;
    jdbcTemplate.update("update sys_user set deleted = 1, updated_at = CURRENT_TIMESTAMP where id = ?", id);
    auditLogService.record(SystemModule.ACCOUNT, "SYS_USER", id, "DELETE", "删除账号 " + user.username);
  }

  @Transactional
  public SystemAccountResponse freeze(Long id) {
    return updateStatus(id, "INACTIVE", "FREEZE", "冻结账号 ");
  }

  @Transactional
  public SystemAccountResponse unfreeze(Long id) {
    return updateStatus(id, "ACTIVE", "UNFREEZE", "解冻账号 ");
  }

  @Transactional
  public SystemAccountResponse resetPassword(Long id, String password) {
    SysUser user = requireUser(id);
    dataScopeService.assertCanAccessOrg(user.orgId);
    user.passwordHash = passwordService.hash(password);
    userMapper.updateById(user);
    long version = revokeSessions(id);
    publishSecurityEvent("SESSION_REVOKED", user, version, "密码已重置，请重新登录");
    auditLogService.record(SystemModule.ACCOUNT, "SYS_USER", id, "RESET_PASSWORD", "重置账号密码 " + user.username);
    return toResponse(user);
  }

  @Transactional
  public SystemAccountResponse updateRoles(Long id, List<Long> roleIds) {
    SysUser user = requireUser(id);
    dataScopeService.assertCanAccessOrg(user.orgId);
    replaceRoles(id, roleIds);
    long version = revokeSessions(id);
    publishSecurityEvent(
        "ROLE_PERMISSION_CHANGED", user, version, "账号角色权限已更新");
    publishSecurityEvent("SESSION_REVOKED", user, version, "角色权限变更，请重新登录");
    auditLogService.record(SystemModule.ACCOUNT, "SYS_USER", id, "ROLE_GRANT", "更新账号角色 " + user.username);
    return toResponse(user);
  }

  private SystemAccountResponse updateStatus(Long id, String status, String action, String summaryPrefix) {
    SysUser user = requireUser(id);
    dataScopeService.assertCanAccessOrg(user.orgId);
    user.status = status;
    userMapper.updateById(user);
    long version = revokeSessions(id);
    publishSecurityEvent(
        "ACCOUNT_STATUS_CHANGED", user, version, summaryPrefix + user.username);
    publishSecurityEvent("SESSION_REVOKED", user, version, "账号状态变更，会话已失效");
    auditLogService.record(SystemModule.ACCOUNT, "SYS_USER", id, action, summaryPrefix + user.username);
    return toResponse(user);
  }

  private SysUser requireUser(Long id) {
    SysUser user = userMapper.selectById(id);
    if (user == null || Integer.valueOf(1).equals(user.deleted)) {
      throw new BusinessException("账号不存在");
    }
    return user;
  }

  private void replaceRoles(Long userId, List<Long> roleIds) {
    userRoleMapper.delete(new QueryWrapper<SysUserRole>().eq("user_id", userId));
    for (Long roleId : roleIds == null ? List.<Long>of() : roleIds) {
      SysUserRole relation = new SysUserRole();
      relation.userId = userId;
      relation.roleId = roleId;
      userRoleMapper.insert(relation);
    }
  }

  private long revokeSessions(Long userId) {
    jdbcTemplate.update(
        "update sys_user set auth_version = auth_version + 1,"
            + " updated_at = CURRENT_TIMESTAMP where id = ?",
        userId);
    return currentAuthVersion(userId);
  }

  private long currentAuthVersion(Long userId) {
    Long version =
        jdbcTemplate.queryForObject(
            "select auth_version from sys_user where id = ?", Long.class, userId);
    return version == null ? 0L : version;
  }

  private void publishSecurityEvent(
      String eventType, SysUser user, long authVersion, String summary) {
    notificationOutboxService.enqueue(
        new DomainNotificationEvent(
            "system-account:"
                + eventType
                + ":"
                + user.id
                + ":"
                + authVersion,
            eventType,
            "SYSTEM_ACCOUNT",
            user.id,
            user.orgId,
            List.of(user.id),
            List.of(),
            user.id,
            CurrentUserContext.require().userId(),
            null,
            Map.of("summary", summary)));
  }

  private List<Long> descendantOrgIds(Long orgId) {
    SysOrg org = orgMapper.selectById(orgId);
    if (org == null || Integer.valueOf(1).equals(org.deleted)) {
      return List.of();
    }
    if (org.orgPath == null || org.orgPath.isBlank()) {
      return List.of(org.id);
    }
    return orgMapper
        .selectList(
            new QueryWrapper<SysOrg>()
                .eq("deleted", 0)
                .likeRight("org_path", org.orgPath)
                .orderByAsc("sort_order", "id"))
        .stream()
        .map(item -> item.id)
        .toList();
  }

  private SystemAccountResponse toResponse(SysUser user) {
    SysOrg org = user.orgId == null ? null : orgMapper.selectById(user.orgId);
    List<SysUserRole> relations = userRoleMapper.selectList(new QueryWrapper<SysUserRole>().eq("user_id", user.id));
    List<Long> roleIds = relations.stream().map(relation -> relation.roleId).toList();
    Map<Long, SysRole> roles =
        roleIds.isEmpty()
            ? Collections.emptyMap()
            : roleMapper.selectBatchIds(roleIds).stream().collect(Collectors.toMap(role -> role.id, Function.identity()));
    List<SystemRoleResponse> roleResponses =
        relations.stream()
            .map(relation -> roles.get(relation.roleId))
            .filter(role -> role != null && !Integer.valueOf(1).equals(role.deleted))
            .map(role -> new SystemRoleResponse(role.id, role.roleCode, role.roleName, role.dataScope))
            .toList();
    return new SystemAccountResponse(
        user.id,
        user.username,
        user.realName,
        user.mobile,
        user.orgId,
        org == null ? null : org.orgName,
        user.status,
        roleIds,
        roleResponses);
  }
}

