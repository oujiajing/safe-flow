package com.pingan.banzu.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.domain.SysOrg;
import com.pingan.banzu.mapper.SysOrgMapper;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import com.pingan.banzu.system.audit.AuditLogService;
import com.pingan.banzu.system.common.SystemModule;
import com.pingan.banzu.system.domain.SysCompanyProfile;
import com.pingan.banzu.system.dto.SystemCompanyQuery;
import com.pingan.banzu.system.dto.SystemCompanyRequest;
import com.pingan.banzu.system.dto.SystemCompanyResponse;
import com.pingan.banzu.system.mapper.SysCompanyProfileMapper;
import java.lang.reflect.InvocationTargetException;
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
public class SystemCompanyService {
  private static final Set<String> MANAGED_ORG_TYPES = Set.of("GROUP", "COMPANY");
  private static final Set<String> COMPANY_TYPES = Set.of("集团", "分公司", "子公司");

  private final SysOrgMapper orgMapper;
  private final SysCompanyProfileMapper profileMapper;
  private final SystemMasterDataSupport support;
  private final AuditLogService auditLogService;

  public SystemCompanyService(
      SysOrgMapper orgMapper,
      SysCompanyProfileMapper profileMapper,
      ApplicationContext applicationContext,
      AuditLogService auditLogService) {
    this.orgMapper = orgMapper;
    this.profileMapper = profileMapper;
    this.support = new SystemMasterDataSupport(orgMapper, applicationContext);
    this.auditLogService = auditLogService;
  }

  public PageResult<SystemCompanyResponse> list(SystemCompanyQuery query) {
    if (query.organizationId() != null) {
      support.assertCanAccessOrg(query.organizationId());
    }
    Map<Long, SysCompanyProfile> profiles = profileMap();
    List<SystemCompanyResponse> rows =
        orgMapper.selectList(
                new QueryWrapper<SysOrg>()
                    .in("org_type", MANAGED_ORG_TYPES)
                    .eq("deleted", 0)
                    .orderByAsc("org_path")
                    .orderByAsc("sort_order")
                    .orderByAsc("id"))
            .stream()
            .filter(org -> support.canAccessOrg(org.id))
            .filter(org -> support.belongsToOrganization(org, query.organizationId()))
            .filter(org -> support.matchesStatus(org.status, query.status()))
            .filter(org -> isValidCompanyProfile(profiles.get(org.id)))
            .filter(org -> support.containsAny(query.keyword(), org.orgCode, org.orgName, profileValue(profiles.get(org.id), p -> p.shortName)))
            .map(org -> toResponse(org, profiles.get(org.id)))
            .toList();
    return support.page(rows, query.page(), query.pageSize());
  }

  @Transactional
  public SystemCompanyResponse create(SystemCompanyRequest request) {
    support.validateStatus(support.statusOrDraft(request.status()));
    Long parentId = resolveParentOrgId(request, null);
    SysOrg org =
        support.createOrg(
            parentId,
            orgTypeForCompanyType(request.companyType(), "COMPANY"),
            request.code(),
            request.name(),
            request.sortOrder(),
            support.statusOrDraft(request.status()));
    SysCompanyProfile profile = new SysCompanyProfile();
    profile.orgId = org.id;
    apply(profile, request);
    profile.createdAt = LocalDateTime.now();
    profile.updatedAt = profile.createdAt;
    profile.deleted = 0;
    profileMapper.insert(profile);
    auditLogService.record(SystemModule.COMPANY, "SYS_ORG", org.id, "CREATE", "新增公司 " + org.orgName);
    return toResponse(org, profile);
  }

  @Transactional
  public SystemCompanyResponse update(Long id, SystemCompanyRequest request) {
    support.assertCanAccessOrg(id);
    SysOrg org = requireManagedOrg(id);
    support.validateStatus(support.statusOrDraft(request.status()));
    support.assertOrgCodeAvailable(request.code(), id);
    Long parentId = resolveParentOrgId(request, id);
    String previousPath = org.orgPath;
    org.orgType = orgTypeForCompanyType(request.companyType(), org.orgType);
    org.orgCode = request.code();
    org.orgName = request.name();
    org.parentId = parentId;
    org.orgPath = orgPath(parentId, org.id);
    org.sortOrder = support.defaultInt(request.sortOrder());
    org.status = support.statusOrDraft(request.status());
    org.updatedAt = LocalDateTime.now();
    orgMapper.updateById(org);
    updateDescendantOrgPaths(previousPath, org.orgPath, org.id);

    SysCompanyProfile profile = profileByOrg(id);
    if (profile == null) {
      profile = new SysCompanyProfile();
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
    auditLogService.record(SystemModule.COMPANY, "SYS_ORG", id, "UPDATE", "更新公司 " + org.orgName);
    return toResponse(org, profile);
  }

  @Transactional
  public SystemCompanyResponse status(Long id, String status) {
    support.assertCanAccessOrg(id);
    support.validateStatus(status);
    SysOrg org = requireManagedOrg(id);
    org.status = status;
    org.updatedAt = LocalDateTime.now();
    orgMapper.updateById(org);
    auditLogService.record(SystemModule.COMPANY, "SYS_ORG", id, "UPDATE", "更新公司状态 " + org.orgName);
    return toResponse(org, profileByOrg(id));
  }

  @Transactional
  public void delete(Long id) {
    support.assertCanAccessOrg(id);
    SysOrg org = requireManagedOrg(id);
    support.assertNoChildren(id);
    orgMapper.deleteById(id);
    SysCompanyProfile profile = profileByOrg(id);
    if (profile != null) {
      profileMapper.deleteById(profile.id);
    }
    auditLogService.record(SystemModule.COMPANY, "SYS_ORG", id, "DELETE", "删除公司 " + org.orgName);
  }

  private void apply(SysCompanyProfile profile, SystemCompanyRequest request) {
    profile.shortName = request.shortName();
    profile.description = request.description();
    profile.address = request.address();
    profile.companyType = request.companyType();
    profile.level1Name = request.level1Name();
    profile.level2Name = request.level2Name();
    profile.level3Name = request.level3Name();
    profile.level4Name = request.level4Name();
    profile.safetyManagerUsername = request.safetyManagerUsername();
    profile.reporterL1Usernames = request.reporterL1Usernames();
    profile.reporterL2Usernames = request.reporterL2Usernames();
    profile.reporterL3Usernames = request.reporterL3Usernames();
    profile.reportL1Time = request.reportL1Time();
    profile.reportL2Time = request.reportL2Time();
    profile.reportL3Time = request.reportL3Time();
    profile.attachment1Url = request.attachment1Url();
    profile.attachment2Url = request.attachment2Url();
    profile.companyIntro = request.companyIntro();
  }

  private Long resolveParentOrgId(SystemCompanyRequest request, Long currentId) {
    String companyType = requireCompanyType(request.companyType());
    if ("集团".equals(companyType)) {
      if (trimToNull(request.level2Name()) == null) {
        requireSelfLevelName(request, request.level1Name(), "一级");
        return null;
      }
      requireSelfLevelName(request, request.level2Name(), "二级");
      return requireParentOrgId("集团", request.level1Name(), currentId, "未找到一级集团");
    }
    if ("分公司".equals(companyType)) {
      requireSelfLevelName(request, request.level3Name(), "三级");
      return requireParentOrgId("集团", request.level2Name(), currentId, "未找到二级集团");
    }
    requireSelfLevelName(request, request.level4Name(), "四级");
    return requireParentOrgId("分公司", request.level3Name(), currentId, "未找到三级分公司");
  }

  private boolean isSelfName(String value, SystemCompanyRequest request) {
    return value != null && (value.equals(trimToNull(request.name())) || value.equals(trimToNull(request.shortName())));
  }

  private void requireSelfLevelName(SystemCompanyRequest request, String value, String levelLabel) {
    String normalized = trimToNull(value);
    if (normalized == null || !isSelfName(normalized, request)) {
      throw new BusinessException(levelLabel + "名称必须与公司名称或简称一致");
    }
  }

  private Long requireParentOrgId(String companyType, String parentName, Long currentId, String message) {
    String normalizedParentName = trimToNull(parentName);
    if (normalizedParentName == null) {
      throw new BusinessException(message + "：上级组织不能为空");
    }
    Map<Long, SysCompanyProfile> profiles = profileMap();
    List<SysOrg> orgs =
        orgMapper.selectList(
            new QueryWrapper<SysOrg>()
                .in("org_type", MANAGED_ORG_TYPES)
                .eq("status", "ACTIVE")
                .eq("deleted", 0)
                .orderByAsc("org_path")
                .orderByAsc("id"));
    for (SysOrg org : orgs) {
      if (currentId != null && currentId.equals(org.id)) {
        continue;
      }
      SysCompanyProfile profile = profiles.get(org.id);
      if (profile != null
          && companyType.equals(profile.companyType)
          && matchesParentName(org, profile, normalizedParentName)) {
        assertNotSelfOrDescendantParent(currentId, org);
        return org.id;
      }
    }
    throw new BusinessException(message + "：" + normalizedParentName);
  }

  private boolean matchesParentName(SysOrg org, SysCompanyProfile profile, String parentName) {
    return parentName.equals(trimToNull(org.orgName))
        || parentName.equals(profileValue(profile, p -> p.shortName))
        || parentName.equals(profileValue(profile, p -> p.level1Name))
        || parentName.equals(profileValue(profile, p -> p.level2Name))
        || parentName.equals(profileValue(profile, p -> p.level3Name))
        || parentName.equals(profileValue(profile, p -> p.level4Name));
  }

  private void assertNotSelfOrDescendantParent(Long currentId, SysOrg parent) {
    if (currentId == null) {
      return;
    }
    if (currentId.equals(parent.id) || (parent.orgPath != null && parent.orgPath.contains("/" + currentId + "/"))) {
      throw new BusinessException("上级组织不能是自身或下级组织");
    }
  }

  private String orgPath(Long parentId, Long orgId) {
    if (parentId == null) {
      return "/" + orgId + "/";
    }
    SysOrg parent = support.requireActiveOrg(parentId);
    return parent.orgPath + orgId + "/";
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

  private String trimToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private SysOrg requireManagedOrg(Long id) {
    SysOrg org = support.requireOrg(id, null);
    if (!MANAGED_ORG_TYPES.contains(org.orgType)) {
      throw new BusinessException("组织不存在");
    }
    return org;
  }

  private String orgTypeForCompanyType(String companyType, String fallback) {
    String normalizedCompanyType = requireCompanyType(companyType);
    if ("集团".equals(normalizedCompanyType)) {
      return "GROUP";
    }
    if ("分公司".equals(normalizedCompanyType) || "子公司".equals(normalizedCompanyType)) {
      return "COMPANY";
    }
    return fallback;
  }

  private String requireCompanyType(String companyType) {
    String normalizedCompanyType = trimToNull(companyType);
    if (normalizedCompanyType != null && COMPANY_TYPES.contains(normalizedCompanyType)) {
      return normalizedCompanyType;
    }
    throw new BusinessException("公司类型必须是集团、分公司或子公司");
  }

  private boolean isValidCompanyProfile(SysCompanyProfile profile) {
    return profile != null
        && profile.companyType != null
        && COMPANY_TYPES.contains(profile.companyType);
  }

  private SystemCompanyResponse toResponse(SysOrg org, SysCompanyProfile profile) {
    return new SystemCompanyResponse(
        org.id,
        org.orgCode,
        org.sortOrder,
        org.orgName,
        profile == null ? null : profile.shortName,
        profile == null ? null : profile.description,
        org.status,
        profile == null ? null : profile.address,
        profile == null ? null : profile.companyType,
        profile == null ? null : profile.level1Name,
        profile == null ? null : profile.level2Name,
        profile == null ? null : profile.level3Name,
        profile == null ? null : profile.level4Name,
        profile == null ? null : profile.safetyManagerUsername,
        profile == null ? null : profile.reporterL1Usernames,
        profile == null ? null : profile.reporterL2Usernames,
        profile == null ? null : profile.reporterL3Usernames,
        profile == null ? null : profile.reportL1Time,
        profile == null ? null : profile.reportL2Time,
        profile == null ? null : profile.reportL3Time,
        profile == null ? null : profile.attachment1Url,
        profile == null ? null : profile.attachment2Url,
        profile == null ? null : profile.companyIntro);
  }

  private SysCompanyProfile profileByOrg(Long orgId) {
    return profileMapper.selectOne(new QueryWrapper<SysCompanyProfile>().eq("org_id", orgId).eq("deleted", 0).last("limit 1"));
  }

  private Map<Long, SysCompanyProfile> profileMap() {
    return profileMapper.selectList(new QueryWrapper<SysCompanyProfile>().eq("deleted", 0)).stream()
        .collect(Collectors.toMap(profile -> profile.orgId, Function.identity(), (left, right) -> left));
  }

  private String profileValue(SysCompanyProfile profile, Function<SysCompanyProfile, String> getter) {
    return profile == null ? null : getter.apply(profile);
  }
}

class SystemMasterDataSupport {
  private static final Set<String> STATUSES = Set.of("DRAFT", "ACTIVE", "INACTIVE");
  private final SysOrgMapper orgMapper;
  private final ApplicationContext applicationContext;

  SystemMasterDataSupport(SysOrgMapper orgMapper, ApplicationContext applicationContext) {
    this.orgMapper = orgMapper;
    this.applicationContext = applicationContext;
  }

  <T> PageResult<T> page(List<T> rows, Integer page, Integer pageSize) {
    int current = Math.max(1, page == null ? 1 : page);
    int size = Math.min(100, Math.max(1, pageSize == null ? 20 : pageSize));
    int from = Math.min((current - 1) * size, rows.size());
    int to = Math.min(from + size, rows.size());
    return new PageResult<>(rows.subList(from, to), rows.size());
  }

  SysOrg createOrg(Long parentId, String orgType, String code, String name, Integer sortOrder, String status) {
    if (isBlank(code) || isBlank(name)) {
      throw new BusinessException("组织编码和名称不能为空");
    }
    assertOrgCodeAvailable(code, null);
    SysOrg parent = parentId == null ? null : requireActiveOrg(parentId);
    SysOrg org = new SysOrg();
    org.parentId = parentId;
    org.orgType = orgType;
    org.orgCode = code;
    org.orgName = name;
    org.orgPath = "/0/";
    org.sortOrder = defaultInt(sortOrder);
    org.status = status;
    org.createdAt = LocalDateTime.now();
    org.updatedAt = org.createdAt;
    org.deleted = 0;
    orgMapper.insert(org);
    org.orgPath = (parent == null ? "/" : parent.orgPath) + org.id + "/";
    orgMapper.updateById(org);
    return org;
  }

  void assertOrgCodeAvailable(String code, Long excludeId) {
    if (isBlank(code)) {
      return;
    }
    QueryWrapper<SysOrg> wrapper = new QueryWrapper<SysOrg>().eq("org_code", code);
    if (excludeId != null) {
      wrapper.ne("id", excludeId);
    }
    if (orgMapper.selectCount(wrapper) > 0) {
      throw new BusinessException("组织编码已存在");
    }
  }

  SysOrg updateOrgStatus(Long id, String orgType, String status) {
    validateStatus(status);
    SysOrg org = requireOrg(id, orgType);
    org.status = status;
    org.updatedAt = LocalDateTime.now();
    orgMapper.updateById(org);
    return org;
  }

  SysOrg requireOrg(Long id, String orgType) {
    SysOrg org = orgMapper.selectById(id);
    if (org == null || Integer.valueOf(1).equals(org.deleted) || (orgType != null && !orgType.equals(org.orgType))) {
      throw new BusinessException("组织不存在");
    }
    return org;
  }

  SysOrg requireActiveOrg(Long id) {
    SysOrg org = requireOrg(id, null);
    if (!"ACTIVE".equals(org.status)) {
      throw new BusinessException("组织未启用");
    }
    return org;
  }

  void assertNoChildren(Long orgId) {
    if (orgMapper.selectCount(new QueryWrapper<SysOrg>().eq("parent_id", orgId).eq("deleted", 0)) > 0) {
      throw new BusinessException("存在下级组织，不能删除");
    }
  }

  void assertCanAccessOrg(Long orgId) {
    if (CurrentUserContext.require().isAdmin()) {
      return;
    }
    if (dataScopeBean() != null) {
      invokeDataScope("assertCanAccessOrg", orgId);
      return;
    }
    if (!canAccessOrgFallback(orgId)) {
      throw new SecurityException("无权访问该组织");
    }
  }

  boolean canAccessOrg(Long orgId) {
    Object bean = dataScopeBean();
    if (bean != null) {
      Object result = invokeDataScope("canAccessOrg", orgId);
      return Boolean.TRUE.equals(result);
    }
    return canAccessOrgFallback(orgId);
  }

  boolean matchesStatus(String actual, String expected) {
    return isBlank(expected) || "all".equalsIgnoreCase(expected) || expected.equals(actual);
  }

  boolean containsAny(String keyword, String... values) {
    if (isBlank(keyword)) {
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

  boolean belongsToOrganization(SysOrg org, Long organizationId) {
    if (organizationId == null) {
      return true;
    }
    return org != null && belongsToOrganization(org.id, organizationId);
  }

  boolean belongsToOrganization(Long orgId, Long organizationId) {
    if (organizationId == null) {
      return true;
    }
    if (orgId == null) {
      return false;
    }
    SysOrg org = orgMapper.selectById(orgId);
    return org != null
        && !Integer.valueOf(1).equals(org.deleted)
        && (organizationId.equals(org.id) || (org.orgPath != null && org.orgPath.contains("/" + organizationId + "/")));
  }

  int defaultInt(Integer value) {
    return value == null ? 0 : value;
  }

  String statusOrDraft(String status) {
    return isBlank(status) ? "DRAFT" : status;
  }

  void validateStatus(String status) {
    if (!STATUSES.contains(status)) {
      throw new BusinessException("状态不合法");
    }
  }

  String orgName(Map<Long, SysOrg> orgs, Long id) {
    SysOrg org = id == null ? null : orgs.get(id);
    return org == null ? null : org.orgName;
  }

  Map<Long, SysOrg> orgMap() {
    return orgMapper.selectList(new QueryWrapper<SysOrg>().eq("deleted", 0)).stream()
        .collect(Collectors.toMap(org -> org.id, Function.identity()));
  }

  private Object dataScopeBean() {
    try {
      Class<?> type = Class.forName("com.pingan.banzu.system.security.SystemDataScopeService");
      return applicationContext.getBean(type);
    } catch (ClassNotFoundException | RuntimeException exception) {
      return null;
    }
  }

  private Object invokeDataScope(String methodName, Long orgId) {
    Object bean = dataScopeBean();
    try {
      return bean.getClass().getMethod(methodName, Long.class).invoke(bean, orgId);
    } catch (IllegalAccessException | NoSuchMethodException exception) {
      throw new IllegalStateException("SystemDataScopeService接口不匹配", exception);
    } catch (InvocationTargetException exception) {
      Throwable cause = exception.getCause();
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new IllegalStateException(cause);
    }
  }

  private boolean canAccessOrgFallback(Long orgId) {
    CurrentUser user = CurrentUserContext.require();
    if (user.isAdmin()) {
      return true;
    }
    SysOrg org = orgMapper.selectById(orgId);
    return org != null && org.orgPath != null && org.orgPath.startsWith(user.orgPath());
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
