package com.pingan.banzu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.config.SystemSecurityProperties;
import com.pingan.banzu.domain.SysOrg;
import com.pingan.banzu.domain.SysRole;
import com.pingan.banzu.domain.SysUser;
import com.pingan.banzu.domain.SysUserRole;
import com.pingan.banzu.dto.LoginRequest;
import com.pingan.banzu.dto.LoginResponse;
import com.pingan.banzu.dto.UserInfoResponse;
import com.pingan.banzu.mapper.SysOrgMapper;
import com.pingan.banzu.mapper.SysRoleMapper;
import com.pingan.banzu.mapper.SysUserMapper;
import com.pingan.banzu.mapper.SysUserRoleMapper;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import com.pingan.banzu.security.JwtService;
import com.pingan.banzu.security.PasswordService;
import com.pingan.banzu.system.security.VerificationCodeService;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuthService {

  private final JwtService jwtService;
  private final PasswordService passwordService;
  private final SysOrgMapper orgMapper;
  private final SysRoleMapper roleMapper;
  private final SysUserMapper userMapper;
  private final SysUserRoleMapper userRoleMapper;
  private final JdbcTemplate jdbcTemplate;
  private final SystemSecurityProperties securityProperties;
  private final VerificationCodeService verificationCodeService;

  public AuthService(
      JwtService jwtService,
      PasswordService passwordService,
      SysOrgMapper orgMapper,
      SysRoleMapper roleMapper,
      SysUserMapper userMapper,
      SysUserRoleMapper userRoleMapper,
      JdbcTemplate jdbcTemplate,
      SystemSecurityProperties securityProperties,
      VerificationCodeService verificationCodeService) {
    this.jwtService = jwtService;
    this.passwordService = passwordService;
    this.orgMapper = orgMapper;
    this.roleMapper = roleMapper;
    this.userMapper = userMapper;
    this.userRoleMapper = userRoleMapper;
    this.jdbcTemplate = jdbcTemplate;
    this.securityProperties = securityProperties;
    this.verificationCodeService = verificationCodeService;
  }

  public LoginResponse login(LoginRequest request) {
    assertNotLocked(request.username());
    SysUser user =
        userMapper.selectOne(
            new QueryWrapper<SysUser>()
                .eq("username", request.username())
                .eq("deleted", 0));
    if (user == null || !"ACTIVE".equals(user.status) || !passwordService.matches(request.password(), user.passwordHash)) {
      failLogin(request.username(), user == null ? null : user.id, "用户名或密码错误");
    }
    if (securityProperties.isTwoFactorEnabled()
        && !verificationCodeService.verify(request.username(), "LOGIN", request.verificationCode())) {
      failLogin(request.username(), user.id, "验证码错误");
    }
    SysOrg org = orgMapper.selectById(user.orgId);
    CurrentUser currentUser =
        new CurrentUser(user.id, user.username, user.realName, user.orgId, org.orgPath, roleCodes(user.id));
    recordLoginAttempt(user.username, user.id, true, null, null);
    recordLoginAudit(user);
    return new LoginResponse(jwtService.issue(currentUser, user.authVersion == null ? 0 : user.authVersion));
  }

  public void logout() {
    CurrentUser user = CurrentUserContext.require();
    jdbcTemplate.update(
        "update sys_user set auth_version = auth_version + 1, updated_at = ? where id = ?",
        LocalDateTime.now(),
        user.userId());
  }

  public UserInfoResponse userInfo() {
    CurrentUser user = CurrentUserContext.require();
    UserProfileInfo profile = userProfileInfo(user.userId());
    DepartmentInfo department = resolveDepartment(user, profile);
    return new UserInfoResponse(
        String.valueOf(user.userId()),
        user.username(),
        user.realName(),
        user.orgId(),
        user.orgPath(),
        department == null ? null : department.id(),
        department == null ? "" : department.name(),
        profile == null ? "" : text(profile.positionName()),
        "https://unpkg.com/@vbenjs/static-source@0.1.7/source/avatar-v1.webp",
        user.roles(),
        "平安班组用户",
        "/pingan/three-checks/pre-shift-meeting",
        "");
  }

  private UserProfileInfo userProfileInfo(Long userId) {
    List<UserProfileInfo> profiles =
        jdbcTemplate.query(
            """
            select department_org_id, position_name
            from sys_user_profile
            where user_id = ? and deleted = 0
            """,
            (rs, rowNum) ->
                new UserProfileInfo(
                    longValue(rs.getObject("department_org_id")),
                    rs.getString("position_name")),
            userId);
    return profiles.isEmpty() ? null : profiles.get(0);
  }

  private DepartmentInfo resolveDepartment(CurrentUser user, UserProfileInfo profile) {
    if (profile != null && profile.departmentId() != null) {
      DepartmentInfo department = departmentInfo(profile.departmentId());
      if (department != null) {
        return department;
      }
    }
    for (Long orgId : reversedOrgPathIds(user.orgPath(), user.orgId())) {
      DepartmentInfo department = departmentInfo(orgId);
      if (department != null) {
        return department;
      }
    }
    return null;
  }

  private DepartmentInfo departmentInfo(Long orgId) {
    if (orgId == null) {
      return null;
    }
    SysOrg org = orgMapper.selectById(orgId);
    if (org == null || org.deleted != 0 || !"DEPARTMENT".equalsIgnoreCase(org.orgType)) {
      return null;
    }
    return new DepartmentInfo(org.id, text(org.orgName));
  }

  private List<Long> reversedOrgPathIds(String orgPath, Long fallbackOrgId) {
    List<Long> ids =
        orgPath == null || orgPath.isBlank()
            ? new java.util.ArrayList<>()
            : java.util.Arrays.stream(orgPath.split("/"))
                .filter(part -> !part.isBlank())
                .map(this::parseLongOrNull)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(java.util.ArrayList::new));
    if (ids.isEmpty() && fallbackOrgId != null) {
      ids.add(fallbackOrgId);
    }
    java.util.Collections.reverse(ids);
    return ids;
  }

  private Long parseLongOrNull(String value) {
    try {
      return Long.valueOf(value);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private Long longValue(Object value) {
    return value instanceof Number number ? number.longValue() : null;
  }

  private String text(String value) {
    return value == null ? "" : value.trim();
  }

  public List<String> accessCodes() {
    CurrentUser user = CurrentUserContext.require();
    if (user.isAdmin()) {
      return jdbcTemplate.queryForList(
          """
          select distinct permission_code
          from sys_menu
          where status = 'ACTIVE' and deleted = 0 and permission_code is not null and permission_code <> ''
          order by permission_code
          """,
          String.class);
    }
    List<SysUserRole> relations =
        userRoleMapper.selectList(new QueryWrapper<SysUserRole>().eq("user_id", user.userId()));
    if (relations.isEmpty()) {
      return List.of();
    }
    String placeholders = relations.stream().map(relation -> "?").collect(Collectors.joining(","));
    Object[] roleIds = relations.stream().map(relation -> relation.roleId).toArray();
    List<MenuPermission> assignedMenus =
        jdbcTemplate.query(
        """
        select distinct m.id, m.parent_id, m.permission_code
        from sys_role_menu rm
        join sys_menu m on m.id = rm.menu_id
        where rm.role_id in (%s)
          and m.status = 'ACTIVE'
          and m.deleted = 0
          and m.permission_code is not null
          and m.permission_code <> ''
        """
            .formatted(placeholders),
        (rs, rowNum) ->
            new MenuPermission(
                rs.getLong("id"),
                rs.getObject("parent_id") == null ? null : rs.getLong("parent_id"),
                rs.getString("permission_code")),
        roleIds);
    return expandWithAncestorPermissionCodes(assignedMenus);
  }

  private List<String> expandWithAncestorPermissionCodes(List<MenuPermission> assignedMenus) {
    if (assignedMenus.isEmpty()) {
      return List.of();
    }
    List<MenuPermission> activeMenus =
        jdbcTemplate.query(
            """
            select id, parent_id, permission_code
            from sys_menu
            where status = 'ACTIVE'
              and deleted = 0
              and permission_code is not null
              and permission_code <> ''
            """,
            (rs, rowNum) ->
                new MenuPermission(
                    rs.getLong("id"),
                    rs.getObject("parent_id") == null ? null : rs.getLong("parent_id"),
                    rs.getString("permission_code")));
    Map<Long, MenuPermission> menusById = new HashMap<>();
    for (MenuPermission menu : activeMenus) {
      menusById.put(menu.id(), menu);
    }

    Set<String> codes = new LinkedHashSet<>();
    for (MenuPermission assignedMenu : assignedMenus) {
      MenuPermission current = menusById.get(assignedMenu.id());
      while (current != null) {
        codes.add(current.permissionCode());
        current = current.parentId() == null ? null : menusById.get(current.parentId());
      }
    }
    return codes.stream().sorted().toList();
  }

  private List<String> roleCodes(Long userId) {
    List<SysUserRole> relations =
        userRoleMapper.selectList(new QueryWrapper<SysUserRole>().eq("user_id", userId));
    if (relations.isEmpty()) {
      return List.of();
    }
    Map<Long, SysRole> roles =
        roleMapper.selectBatchIds(relations.stream().map(item -> item.roleId).toList()).stream()
            .collect(Collectors.toMap(role -> role.id, Function.identity()));
    return relations.stream()
        .map(item -> roles.get(item.roleId))
        .filter(role -> role != null && (role.deleted == null || role.deleted == 0))
        .map(role -> role.roleCode)
        .toList();
  }

  private record MenuPermission(Long id, Long parentId, String permissionCode) {}

  private record UserProfileInfo(Long departmentId, String positionName) {}

  private record DepartmentInfo(Long id, String name) {}

  private void assertNotLocked(String username) {
    List<Timestamp> locks =
        jdbcTemplate.queryForList(
            """
            select locked_until
            from sys_login_attempt
            where username = ? and locked_until is not null and locked_until > ?
            order by locked_until desc
            limit 1
            """,
            Timestamp.class,
            username,
            Timestamp.valueOf(LocalDateTime.now()));
    if (!locks.isEmpty()) {
      throw new BusinessException("账号已锁定，请稍后再试");
    }
  }

  private void failLogin(String username, Long userId, String reason) {
    int maxAttempts = Math.max(1, securityProperties.getMaxFailedLoginAttempts());
    int currentFailures = consecutiveFailures(username);
    LocalDateTime lockedUntil =
        currentFailures + 1 >= maxAttempts
            ? LocalDateTime.now().plusMinutes(Math.max(1, securityProperties.getLockMinutes()))
            : null;
    recordLoginAttempt(username, userId, false, reason, lockedUntil);
    if (lockedUntil != null) {
      throw new BusinessException("登录失败次数过多，账号已锁定");
    }
    throw new BusinessException(reason);
  }

  private int consecutiveFailures(String username) {
    int limit = Math.max(1, securityProperties.getMaxFailedLoginAttempts());
    List<Integer> attempts =
        jdbcTemplate.queryForList(
            """
            select success
            from sys_login_attempt
            where username = ?
            order by attempted_at desc, id desc
            limit ?
            """,
            Integer.class,
            username,
            limit);
    int count = 0;
    for (Integer success : attempts) {
      if (Integer.valueOf(1).equals(success)) {
        break;
      }
      count++;
    }
    return count;
  }

  private void recordLoginAttempt(
      String username, Long userId, boolean success, String reason, LocalDateTime lockedUntil) {
    jdbcTemplate.update(
        """
        insert into sys_login_attempt
          (username, user_id, success, failure_reason, ip_address, locked_until)
        values (?, ?, ?, ?, ?, ?)
        """,
        username,
        userId,
        success ? 1 : 0,
        reason,
        currentIp(),
        lockedUntil);
  }

  private void recordLoginAudit(SysUser user) {
    jdbcTemplate.update(
        """
        insert into sys_access_log
          (user_id, username, module, target_type, target_id, action, summary, request_path, request_method, ip_address)
        values (?, ?, 'SECURITY', 'SYS_USER', ?, 'LOGIN', ?, ?, ?, ?)
        """,
        user.id,
        user.username,
        user.id,
        "用户登录",
        currentPath(),
        currentMethod(),
        currentIp());
  }

  private String currentPath() {
    ServletRequestAttributes attrs = currentRequestAttributes();
    return attrs == null ? null : attrs.getRequest().getRequestURI();
  }

  private String currentMethod() {
    ServletRequestAttributes attrs = currentRequestAttributes();
    return attrs == null ? null : attrs.getRequest().getMethod();
  }

  private String currentIp() {
    ServletRequestAttributes attrs = currentRequestAttributes();
    return attrs == null ? null : attrs.getRequest().getRemoteAddr();
  }

  private ServletRequestAttributes currentRequestAttributes() {
    return (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
  }
}
