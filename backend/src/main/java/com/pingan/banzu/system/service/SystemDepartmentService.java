package com.pingan.banzu.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.domain.SysOrg;
import com.pingan.banzu.mapper.SysOrgMapper;
import com.pingan.banzu.system.audit.AuditLogService;
import com.pingan.banzu.system.common.SystemModule;
import com.pingan.banzu.system.domain.SysDepartmentProfile;
import com.pingan.banzu.system.dto.SystemDepartmentQuery;
import com.pingan.banzu.system.dto.SystemDepartmentRequest;
import com.pingan.banzu.system.dto.SystemDepartmentResponse;
import com.pingan.banzu.system.mapper.SysDepartmentProfileMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemDepartmentService {
  private static final Set<String> COMPANY_ORG_TYPES = Set.of("GROUP", "COMPANY");

  private final SysOrgMapper orgMapper;
  private final SysDepartmentProfileMapper profileMapper;
  private final SystemMasterDataSupport support;
  private final AuditLogService auditLogService;

  public SystemDepartmentService(
      SysOrgMapper orgMapper,
      SysDepartmentProfileMapper profileMapper,
      ApplicationContext applicationContext,
      AuditLogService auditLogService) {
    this.orgMapper = orgMapper;
    this.profileMapper = profileMapper;
    this.support = new SystemMasterDataSupport(orgMapper, applicationContext);
    this.auditLogService = auditLogService;
  }

  public PageResult<SystemDepartmentResponse> list(SystemDepartmentQuery query) {
    if (query.organizationId() != null) {
      support.assertCanAccessOrg(query.organizationId());
    }
    Map<Long, SysOrg> orgs = support.orgMap();
    Map<Long, SysDepartmentProfile> profiles = profileMap();
    List<SystemDepartmentResponse> rows =
        orgs.values().stream()
            .filter(org -> "DEPARTMENT".equals(org.orgType))
            .filter(org -> support.canAccessOrg(org.id))
            .filter(org -> support.belongsToOrganization(org, query.organizationId()))
            .filter(org -> support.matchesStatus(org.status, query.status()))
            .filter(org -> {
              SysDepartmentProfile profile = profiles.get(org.id);
              return query.companyOrgId() == null || (profile != null && query.companyOrgId().equals(profile.companyOrgId));
            })
            .filter(org -> {
              SysDepartmentProfile profile = profiles.get(org.id);
              return support.containsAny(query.keyword(), org.orgCode, org.orgName, profile == null ? null : profile.leaderUsername);
            })
            .sorted((left, right) -> Integer.compare(left.sortOrder, right.sortOrder))
            .map(org -> toResponse(org, profiles.get(org.id), orgs))
            .toList();
    return support.page(rows, query.page(), query.pageSize());
  }

  @Transactional
  public SystemDepartmentResponse create(SystemDepartmentRequest request) {
    SysOrg company = requireCompanyOrg(request.companyOrgId());
    support.assertCanAccessOrg(company.id);
    String status = support.statusOrDraft(request.status());
    support.validateStatus(status);
    SysOrg org = support.createOrg(company.id, "DEPARTMENT", request.code(), request.name(), request.childSortOrder(), status);
    SysDepartmentProfile profile = new SysDepartmentProfile();
    profile.orgId = org.id;
    apply(profile, request);
    profile.createdAt = LocalDateTime.now();
    profile.updatedAt = profile.createdAt;
    profile.deleted = 0;
    profileMapper.insert(profile);
    auditLogService.record(SystemModule.DEPARTMENT, "SYS_ORG", org.id, "CREATE", "新增部门 " + org.orgName);
    return toResponse(org, profile, support.orgMap());
  }

  @Transactional
  public SystemDepartmentResponse update(Long id, SystemDepartmentRequest request) {
    support.assertCanAccessOrg(id);
    SysOrg company = requireCompanyOrg(request.companyOrgId());
    support.assertCanAccessOrg(company.id);
    String status = support.statusOrDraft(request.status());
    support.validateStatus(status);
    SysOrg org = support.requireOrg(id, "DEPARTMENT");
    support.assertOrgCodeAvailable(request.code(), id);
    String previousPath = org.orgPath;
    org.parentId = company.id;
    org.orgCode = request.code();
    org.orgName = request.name();
    org.orgPath = company.orgPath + org.id + "/";
    org.sortOrder = support.defaultInt(request.childSortOrder());
    org.status = status;
    org.updatedAt = LocalDateTime.now();
    orgMapper.updateById(org);
    updateDescendantOrgPaths(previousPath, org.orgPath, org.id);
    SysDepartmentProfile profile = profileByOrg(id);
    if (profile == null) {
      profile = new SysDepartmentProfile();
      profile.orgId = id;
      profile.createdAt = LocalDateTime.now();
      profile.deleted = 0;
    }
    apply(profile, request);
    profile.updatedAt = LocalDateTime.now();
    if (profile.id == null) {
      profileMapper.insert(profile);
    } else {
      profileMapper.updateById(profile);
    }
    auditLogService.record(SystemModule.DEPARTMENT, "SYS_ORG", id, "UPDATE", "更新部门 " + org.orgName);
    return toResponse(org, profile, support.orgMap());
  }

  @Transactional
  public SystemDepartmentResponse status(Long id, String status) {
    support.assertCanAccessOrg(id);
    SysOrg org = support.updateOrgStatus(id, "DEPARTMENT", status);
    auditLogService.record(SystemModule.DEPARTMENT, "SYS_ORG", id, "UPDATE", "更新部门状态 " + org.orgName);
    return toResponse(org, profileByOrg(id), support.orgMap());
  }

  @Transactional
  public void delete(Long id) {
    support.assertCanAccessOrg(id);
    SysOrg org = support.requireOrg(id, "DEPARTMENT");
    support.assertNoChildren(id);
    orgMapper.deleteById(id);
    SysDepartmentProfile profile = profileByOrg(id);
    if (profile != null) {
      profileMapper.deleteById(profile.id);
    }
    auditLogService.record(SystemModule.DEPARTMENT, "SYS_ORG", id, "DELETE", "删除部门 " + org.orgName);
  }

  private void apply(SysDepartmentProfile profile, SystemDepartmentRequest request) {
    profile.companyOrgId = request.companyOrgId();
    profile.departmentType = request.departmentType();
    profile.childSortOrder = support.defaultInt(request.childSortOrder());
    profile.leaderUsername = request.leaderUsername();
    profile.description = request.description();
    profile.topLevelName = request.topLevelName();
    profile.groupName = request.groupName();
    profile.level1Unit = request.level1Unit();
    profile.level2Unit = request.level2Unit();
    profile.leaderLevel = request.leaderLevel();
    profile.companySortOrder = support.defaultInt(request.companySortOrder());
  }

  private SysOrg requireCompanyOrg(Long orgId) {
    SysOrg org = support.requireActiveOrg(orgId);
    if (!COMPANY_ORG_TYPES.contains(org.orgType)) {
      throw new BusinessException("所属公司必须是公司或集团");
    }
    return org;
  }

  private void updateDescendantOrgPaths(String previousPath, String nextPath, Long orgId) {
    if (previousPath == null || nextPath == null || previousPath.equals(nextPath)) {
      return;
    }
    List<SysOrg> descendants =
        orgMapper.selectList(
            new QueryWrapper<SysOrg>()
                .ne("id", orgId)
                .likeRight("org_path", previousPath)
                .eq("deleted", 0));
    for (SysOrg descendant : descendants) {
      descendant.orgPath = nextPath + descendant.orgPath.substring(previousPath.length());
      descendant.updatedAt = LocalDateTime.now();
      orgMapper.updateById(descendant);
    }
  }

  private SystemDepartmentResponse toResponse(SysOrg org, SysDepartmentProfile profile, Map<Long, SysOrg> orgs) {
    return new SystemDepartmentResponse(
        org.id,
        org.orgCode,
        org.orgName,
        profile == null ? null : profile.companyOrgId,
        profile == null ? null : support.orgName(orgs, profile.companyOrgId),
        profile == null ? null : profile.departmentType,
        profile == null ? null : profile.childSortOrder,
        profile == null ? null : profile.leaderUsername,
        profile == null ? null : profile.description,
        org.status,
        profile == null ? null : profile.topLevelName,
        profile == null ? null : profile.groupName,
        profile == null ? null : profile.level1Unit,
        profile == null ? null : profile.level2Unit,
        profile == null ? null : profile.leaderLevel,
        profile == null ? null : profile.companySortOrder);
  }

  private SysDepartmentProfile profileByOrg(Long orgId) {
    return profileMapper.selectOne(new QueryWrapper<SysDepartmentProfile>().eq("org_id", orgId).eq("deleted", 0).last("limit 1"));
  }

  private Map<Long, SysDepartmentProfile> profileMap() {
    return profileMapper.selectList(new QueryWrapper<SysDepartmentProfile>().eq("deleted", 0)).stream()
        .collect(Collectors.toMap(profile -> profile.orgId, Function.identity(), (left, right) -> left));
  }
}
