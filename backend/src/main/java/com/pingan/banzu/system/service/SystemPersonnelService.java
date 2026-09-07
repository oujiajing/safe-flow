package com.pingan.banzu.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.domain.SysOrg;
import com.pingan.banzu.domain.SysUser;
import com.pingan.banzu.mapper.SysOrgMapper;
import com.pingan.banzu.mapper.SysUserMapper;
import com.pingan.banzu.system.audit.AuditLogService;
import com.pingan.banzu.system.common.SystemModule;
import com.pingan.banzu.system.domain.SysUserProfile;
import com.pingan.banzu.system.dto.SystemPersonnelQuery;
import com.pingan.banzu.system.dto.SystemPersonnelRequest;
import com.pingan.banzu.system.dto.SystemPersonnelResponse;
import com.pingan.banzu.system.mapper.SysUserProfileMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemPersonnelService {
  private final SysOrgMapper orgMapper;
  private final SysUserMapper userMapper;
  private final SysUserProfileMapper profileMapper;
  private final SystemMasterDataSupport support;
  private final AuditLogService auditLogService;

  public SystemPersonnelService(
      SysOrgMapper orgMapper,
      SysUserMapper userMapper,
      SysUserProfileMapper profileMapper,
      ApplicationContext applicationContext,
      AuditLogService auditLogService) {
    this.orgMapper = orgMapper;
    this.userMapper = userMapper;
    this.profileMapper = profileMapper;
    this.support = new SystemMasterDataSupport(orgMapper, applicationContext);
    this.auditLogService = auditLogService;
  }

  public PageResult<SystemPersonnelResponse> list(SystemPersonnelQuery query) {
    if (query.organizationId() != null) {
      support.assertCanAccessOrg(query.organizationId());
    }
    Map<Long, SysOrg> orgs = support.orgMap();
    Map<Long, SysUserProfile> profiles = profileMap();
    List<SystemPersonnelResponse> rows =
        userMapper.selectList(new QueryWrapper<SysUser>().eq("deleted", 0).orderByAsc("id")).stream()
            .filter(user -> support.canAccessOrg(user.orgId))
            .filter(user -> belongsToOrganization(user, profiles.get(user.id), query.organizationId()))
            .filter(user -> support.matchesStatus(user.status, query.status()))
            .filter(user -> {
              SysUserProfile profile = profiles.get(user.id);
              return query.companyOrgId() == null || (profile != null && query.companyOrgId().equals(profile.companyOrgId));
            })
            .filter(user -> {
              SysUserProfile profile = profiles.get(user.id);
              return query.departmentOrgId() == null || (profile != null && query.departmentOrgId().equals(profile.departmentOrgId));
            })
            .filter(user -> {
              SysUserProfile profile = profiles.get(user.id);
              return query.teamOrgId() == null || (profile != null && query.teamOrgId().equals(profile.teamOrgId));
            })
            .filter(user -> {
              SysUserProfile profile = profiles.get(user.id);
              return support.containsAny(query.keyword(), user.username, user.realName, user.mobile, profile == null ? null : profile.employeeCode);
            })
            .map(user -> toResponse(user, profiles.get(user.id), orgs))
            .toList();
    return support.page(rows, query.page(), query.pageSize());
  }

  @Transactional
  public SystemPersonnelResponse create(SystemPersonnelRequest request) {
    Long companyOrgId = request.companyOrgId();
    Long orgId = resolveUserOrgId(companyOrgId, request.departmentOrgId(), request.teamOrgId());
    support.assertCanAccessOrg(companyOrgId);
    if (request.departmentOrgId() != null) {
      support.assertCanAccessOrg(request.departmentOrgId());
    }
    if (request.teamOrgId() != null) {
      support.assertCanAccessOrg(request.teamOrgId());
    }
    String status = support.statusOrDraft(request.status());
    support.validateStatus(status);
    SysUser user = new SysUser();
    user.username =
        isBlank(request.username())
            ? request.employeeCode()
            : request.username();
    user.passwordHash = "{noop}SAFE_TEST_PASSWORD";
    user.realName = request.name();
    user.mobile = request.mobile();
    user.orgId = orgId;
    user.status = status;
    user.createdAt = LocalDateTime.now();
    user.updatedAt = user.createdAt;
    user.deleted = 0;
    userMapper.insert(user);

    SysUserProfile profile = new SysUserProfile();
    profile.userId = user.id;
    apply(profile, request, companyOrgId);
    profile.createdAt = LocalDateTime.now();
    profile.updatedAt = profile.createdAt;
    profile.deleted = 0;
    profileMapper.insert(profile);
    auditLogService.record(SystemModule.PERSONNEL, "SYS_USER", user.id, "CREATE", "新增人员 " + user.realName);
    return toResponse(user, profile, support.orgMap());
  }

  @Transactional
  public SystemPersonnelResponse update(Long id, SystemPersonnelRequest request) {
    SysUser user = requireUser(id);
    support.assertCanAccessOrg(user.orgId);
    Long companyOrgId = request.companyOrgId();
    support.assertCanAccessOrg(companyOrgId);
    Long orgId = resolveUserOrgId(companyOrgId, request.departmentOrgId(), request.teamOrgId());
    String status = support.statusOrDraft(request.status());
    support.validateStatus(status);
    user.username =
        isBlank(request.username())
            ? request.employeeCode()
            : request.username();
    user.realName = request.name();
    user.mobile = request.mobile();
    user.orgId = orgId;
    user.status = status;
    user.updatedAt = LocalDateTime.now();
    userMapper.updateById(user);

    SysUserProfile profile = profileByUser(id);
    if (profile == null) {
      profile = new SysUserProfile();
      profile.userId = id;
      profile.createdAt = LocalDateTime.now();
      profile.deleted = 0;
    }
    apply(profile, request, companyOrgId);
    profile.updatedAt = LocalDateTime.now();
    if (profile.id == null) {
      profileMapper.insert(profile);
    } else {
      profileMapper.updateById(profile);
    }
    auditLogService.record(SystemModule.PERSONNEL, "SYS_USER", id, "UPDATE", "更新人员 " + user.realName);
    return toResponse(user, profile, support.orgMap());
  }

  @Transactional
  public SystemPersonnelResponse status(Long id, String status) {
    support.validateStatus(status);
    SysUser user = requireUser(id);
    support.assertCanAccessOrg(user.orgId);
    user.status = status;
    user.updatedAt = LocalDateTime.now();
    userMapper.updateById(user);
    auditLogService.record(SystemModule.PERSONNEL, "SYS_USER", id, "UPDATE", "更新人员状态 " + user.realName);
    return toResponse(user, profileByUser(id), support.orgMap());
  }

  @Transactional
  public void delete(Long id) {
    SysUser user = requireUser(id);
    support.assertCanAccessOrg(user.orgId);
    userMapper.deleteById(id);
    SysUserProfile profile = profileByUser(id);
    if (profile != null) {
      profileMapper.deleteById(profile.id);
    }
    auditLogService.record(SystemModule.PERSONNEL, "SYS_USER", id, "DELETE", "删除人员 " + user.realName);
  }

  private void apply(SysUserProfile profile, SystemPersonnelRequest request, Long companyOrgId) {
    profile.employeeCode = request.employeeCode();
    profile.companyOrgId = companyOrgId;
    profile.companyShortName = request.companyShortName();
    profile.departmentOrgId = request.departmentOrgId();
    profile.teamOrgId = request.teamOrgId();
    profile.points = support.defaultInt(request.points());
    profile.receivedPoints = support.defaultInt(request.receivedPoints());
    profile.employeeType = request.employeeType();
    profile.positionName = request.positionName();
    profile.systemRoleCode = request.systemRoleCode();
    profile.submitDate = request.submitDate();
    profile.applicantName = request.applicantName();
    profile.remark = request.remark();
    profile.certificateValidUntil = request.certificateValidUntil();
    profile.joinDate = request.joinDate();
    profile.departmentSortOrder = support.defaultInt(request.departmentSortOrder());
    profile.managementWeight = support.defaultInt(request.managementWeight());
  }

  private SystemPersonnelResponse toResponse(SysUser user, SysUserProfile profile, Map<Long, SysOrg> orgs) {
    return new SystemPersonnelResponse(
        user.id,
        profile == null ? null : profile.employeeCode,
        user.realName,
        user.username,
        profile == null ? null : profile.companyOrgId,
        profile == null ? null : support.orgName(orgs, profile.companyOrgId),
        profile == null ? null : profile.companyShortName,
        profile == null ? null : profile.departmentOrgId,
        profile == null ? null : support.orgName(orgs, profile.departmentOrgId),
        profile == null ? null : profile.teamOrgId,
        profile == null ? null : support.orgName(orgs, profile.teamOrgId),
        profile == null ? null : profile.points,
        profile == null ? null : profile.receivedPoints,
        profile == null ? null : profile.employeeType,
        profile == null ? null : profile.positionName,
        user.mobile,
        user.status,
        profile == null ? null : profile.systemRoleCode,
        profile == null ? null : profile.submitDate,
        profile == null ? null : profile.applicantName,
        profile == null ? null : profile.remark,
        profile == null ? null : profile.certificateValidUntil,
        profile == null ? null : profile.joinDate,
        profile == null ? null : profile.departmentSortOrder,
        profile == null ? null : profile.managementWeight);
  }

  private Long resolveUserOrgId(Long companyOrgId, Long departmentOrgId, Long teamOrgId) {
    if (teamOrgId != null) {
      return teamOrgId;
    }
    if (departmentOrgId != null) {
      return departmentOrgId;
    }
    return companyOrgId;
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private SysUser requireUser(Long id) {
    SysUser user = userMapper.selectById(id);
    if (user == null || Integer.valueOf(1).equals(user.deleted)) {
      throw new com.pingan.banzu.common.BusinessException("人员不存在");
    }
    return user;
  }

  private SysUserProfile profileByUser(Long userId) {
    return profileMapper.selectOne(new QueryWrapper<SysUserProfile>().eq("user_id", userId).eq("deleted", 0).last("limit 1"));
  }

  private Map<Long, SysUserProfile> profileMap() {
    return profileMapper.selectList(new QueryWrapper<SysUserProfile>().eq("deleted", 0)).stream()
        .collect(Collectors.toMap(profile -> profile.userId, Function.identity(), (left, right) -> left));
  }

  private boolean belongsToOrganization(SysUser user, SysUserProfile profile, Long organizationId) {
    return support.belongsToOrganization(user.orgId, organizationId)
        || (profile != null
            && (support.belongsToOrganization(profile.companyOrgId, organizationId)
                || support.belongsToOrganization(profile.departmentOrgId, organizationId)
                || support.belongsToOrganization(profile.teamOrgId, organizationId)));
  }
}

