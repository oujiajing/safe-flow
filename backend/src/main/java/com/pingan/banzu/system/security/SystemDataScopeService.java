package com.pingan.banzu.system.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pingan.banzu.domain.SysOrg;
import com.pingan.banzu.domain.SysRole;
import com.pingan.banzu.mapper.SysOrgMapper;
import com.pingan.banzu.mapper.SysRoleMapper;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class SystemDataScopeService {

  private final SysOrgMapper orgMapper;
  private final SysRoleMapper roleMapper;

  public SystemDataScopeService(SysOrgMapper orgMapper, SysRoleMapper roleMapper) {
    this.orgMapper = orgMapper;
    this.roleMapper = roleMapper;
  }

  public boolean canAccessOrg(Long orgId) {
    if (orgId == null) {
      return false;
    }
    return accessibleOrgIds().contains(orgId);
  }

  public void assertCanAccessOrg(Long orgId) {
    if (!canAccessOrg(orgId)) {
      throw new SecurityException("无权访问该组织数据");
    }
  }

  public boolean canReadCompany(Long companyId) {
    return companyId != null && readableCompanyIds().contains(companyId);
  }

  public void assertCanReadCompany(Long companyId) {
    if (!canReadCompany(companyId)) {
      throw new SecurityException("无权访问该组织数据");
    }
  }

  public boolean canReadDepartment(Long departmentId) {
    return departmentId != null && readableDepartmentIds().contains(departmentId);
  }

  public void assertCanReadDepartment(Long departmentId) {
    if (!canReadDepartment(departmentId)) {
      throw new SecurityException("无权访问该组织数据");
    }
  }

  public String currentDataScope() {
    return effectiveScope(CurrentUserContext.require());
  }

  public List<Long> accessibleOrgIds() {
    CurrentUser user = CurrentUserContext.require();
    String scope = effectiveScope(user);
    if ("ALL".equals(scope)) {
      return activeOrgIds();
    }
    if ("SELF".equals(scope)) {
      return List.of(user.orgId());
    }
    SysOrg ownOrg = orgMapper.selectById(user.orgId());
    if (ownOrg == null || ownOrg.orgPath == null) {
      return List.of(user.orgId());
    }
    return orgMapper
        .selectList(
            new QueryWrapper<SysOrg>()
                .eq("deleted", 0)
                .likeRight("org_path", ownOrg.orgPath)
                .orderByAsc("sort_order", "id"))
        .stream()
        .map(org -> org.id)
        .toList();
  }

  public QueryWrapper<?> applyOrgScope(QueryWrapper<?> wrapper, String orgIdColumn) {
    CurrentUser user = CurrentUserContext.require();
    if ("ALL".equals(effectiveScope(user))) {
      return wrapper;
    }
    List<Long> orgIds = accessibleOrgIds();
    if (orgIds.isEmpty()) {
      wrapper.eq(orgIdColumn, -1L);
    } else {
      wrapper.in(orgIdColumn, orgIds);
    }
    return wrapper;
  }

  public QueryWrapper<?> applyCompanyReadScope(QueryWrapper<?> wrapper, String companyIdColumn) {
    if ("ALL".equals(effectiveScope(CurrentUserContext.require()))) {
      return wrapper;
    }
    List<Long> companyIds = readableCompanyIds();
    if (companyIds.isEmpty()) {
      wrapper.eq(companyIdColumn, -1L);
    } else {
      wrapper.in(companyIdColumn, companyIds);
    }
    return wrapper;
  }

  public QueryWrapper<?> applyOptionalCompanyReadScope(
      QueryWrapper<?> wrapper, String companyIdColumn) {
    if ("ALL".equals(effectiveScope(CurrentUserContext.require()))) {
      return wrapper;
    }
    List<Long> companyIds = readableCompanyIds();
    wrapper.and(
        nested -> {
          nested.isNull(companyIdColumn);
          if (!companyIds.isEmpty()) {
            nested.or().in(companyIdColumn, companyIds);
          }
        });
    return wrapper;
  }

  public QueryWrapper<?> applyDepartmentReadScope(QueryWrapper<?> wrapper, String departmentIdColumn) {
    if ("ALL".equals(effectiveScope(CurrentUserContext.require()))) {
      return wrapper;
    }
    List<Long> departmentIds = readableDepartmentIds();
    if (!departmentIds.isEmpty()) {
      wrapper.in(departmentIdColumn, departmentIds);
    }
    return wrapper;
  }

  public QueryWrapper<?> applyOptionalDepartmentReadScope(
      QueryWrapper<?> wrapper, String departmentIdColumn) {
    if ("ALL".equals(effectiveScope(CurrentUserContext.require()))) {
      return wrapper;
    }
    List<Long> departmentIds = readableDepartmentIds();
    wrapper.and(
        nested -> {
          nested.isNull(departmentIdColumn);
          if (!departmentIds.isEmpty()) {
            nested.or().in(departmentIdColumn, departmentIds);
          }
        });
    return wrapper;
  }

  private List<Long> activeOrgIds() {
    return orgMapper.selectList(new QueryWrapper<SysOrg>().eq("deleted", 0).orderByAsc("sort_order", "id")).stream()
        .map(org -> org.id)
        .toList();
  }

  private List<Long> readableCompanyIds() {
    return readableAncestorIds("COMPANY");
  }

  private List<Long> readableDepartmentIds() {
    return readableAncestorIds("DEPARTMENT");
  }

  private List<Long> readableAncestorIds(String orgType) {
    CurrentUser user = CurrentUserContext.require();
    if ("ALL".equals(effectiveScope(user))) {
      return activeOrgs().stream().filter(org -> orgType.equals(org.orgType)).map(org -> org.id).toList();
    }
    Map<Long, SysOrg> orgsById =
        activeOrgs().stream().collect(Collectors.toMap(org -> org.id, org -> org));
    Set<Long> ids = new LinkedHashSet<>();
    for (Long accessibleOrgId : accessibleOrgIds()) {
      SysOrg org = orgsById.get(accessibleOrgId);
      if (org == null || org.orgPath == null) {
        continue;
      }
      for (String part : org.orgPath.split("/")) {
        if (part == null || part.isBlank()) {
          continue;
        }
        Long id = Long.valueOf(part);
        SysOrg ancestor = orgsById.get(id);
        if (ancestor != null && orgType.equals(ancestor.orgType)) {
          ids.add(ancestor.id);
        }
      }
    }
    return ids.stream().filter(Objects::nonNull).toList();
  }

  private List<SysOrg> activeOrgs() {
    return orgMapper.selectList(new QueryWrapper<SysOrg>().eq("deleted", 0).orderByAsc("sort_order", "id"));
  }

  private String effectiveScope(CurrentUser user) {
    if (user.isAdmin()) {
      return "ALL";
    }
    if (user.roles() == null || user.roles().isEmpty()) {
      return "SELF";
    }
    List<SysRole> roles =
        roleMapper.selectList(new QueryWrapper<SysRole>().in("role_code", user.roles()).eq("deleted", 0));
    return roles.stream()
        .map(role -> role.dataScope)
        .min(Comparator.comparingInt(this::scopeRank))
        .orElse("SELF");
  }

  private int scopeRank(String scope) {
    return switch (scope) {
      case "ALL" -> 0;
      case "ORG_AND_CHILDREN" -> 1;
      default -> 2;
    };
  }
}
