package com.pingan.banzu.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.domain.SysOrg;
import com.pingan.banzu.mapper.SysOrgMapper;
import com.pingan.banzu.system.audit.AuditLogService;
import com.pingan.banzu.system.common.SystemModule;
import com.pingan.banzu.system.domain.SysTeamMember;
import com.pingan.banzu.system.domain.SysTeamProfile;
import com.pingan.banzu.system.dto.SystemTeamQuery;
import com.pingan.banzu.system.dto.SystemTeamRequest;
import com.pingan.banzu.system.dto.SystemTeamResponse;
import com.pingan.banzu.system.mapper.SysTeamMemberMapper;
import com.pingan.banzu.system.mapper.SysTeamProfileMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemTeamService {
  private final SysOrgMapper orgMapper;
  private final SysTeamProfileMapper profileMapper;
  private final SysTeamMemberMapper memberMapper;
  private final SystemMasterDataSupport support;
  private final AuditLogService auditLogService;

  public SystemTeamService(
      SysOrgMapper orgMapper,
      SysTeamProfileMapper profileMapper,
      SysTeamMemberMapper memberMapper,
      ApplicationContext applicationContext,
      AuditLogService auditLogService) {
    this.orgMapper = orgMapper;
    this.profileMapper = profileMapper;
    this.memberMapper = memberMapper;
    this.support = new SystemMasterDataSupport(orgMapper, applicationContext);
    this.auditLogService = auditLogService;
  }

  public PageResult<SystemTeamResponse> list(SystemTeamQuery query) {
    if (query.organizationId() != null) {
      support.assertCanAccessOrg(query.organizationId());
    }
    Map<Long, SysOrg> orgs = support.orgMap();
    Map<Long, SysTeamProfile> profiles = profileMap();
    List<SystemTeamResponse> rows =
        orgs.values().stream()
            .filter(org -> "TEAM".equals(org.orgType))
            .filter(org -> support.canAccessOrg(org.id))
            .filter(org -> support.belongsToOrganization(org, query.organizationId()))
            .filter(org -> support.matchesStatus(org.status, query.status()))
            .filter(org -> {
              SysTeamProfile profile = profiles.get(org.id);
              return query.companyOrgId() == null || (profile != null && query.companyOrgId().equals(profile.companyOrgId));
            })
            .filter(org -> {
              SysTeamProfile profile = profiles.get(org.id);
              return query.workshopOrgId() == null || (profile != null && query.workshopOrgId().equals(profile.workshopOrgId));
            })
            .filter(org -> {
              SysTeamProfile profile = profiles.get(org.id);
              return support.containsAny(query.keyword(), org.orgCode, org.orgName, profile == null ? null : profile.workTypeName);
            })
            .sorted((left, right) -> Integer.compare(left.sortOrder, right.sortOrder))
            .map(org -> toResponse(org, profiles.get(org.id), orgs))
            .toList();
    return support.page(rows, query.page(), query.pageSize());
  }

  @Transactional
  public SystemTeamResponse create(SystemTeamRequest request) {
    support.assertCanAccessOrg(request.companyOrgId());
    if (request.workshopOrgId() != null) {
      support.assertCanAccessOrg(request.workshopOrgId());
    }
    String status = support.statusOrDraft(request.status());
    support.validateStatus(status);
    Long parentId = request.workshopOrgId() == null ? request.companyOrgId() : request.workshopOrgId();
    SysOrg org = support.createOrg(parentId, "TEAM", request.code(), request.name(), 0, status);
    SysTeamProfile profile = new SysTeamProfile();
    profile.orgId = org.id;
    apply(profile, request);
    profile.createdAt = LocalDateTime.now();
    profile.updatedAt = profile.createdAt;
    profile.deleted = 0;
    profileMapper.insert(profile);
    replaceMembers(org.id, request.teamMembers());
    auditLogService.record(SystemModule.TEAM, "SYS_ORG", org.id, "CREATE", "新增班组 " + org.orgName);
    return toResponse(org, profile, support.orgMap());
  }

  @Transactional
  public SystemTeamResponse update(Long id, SystemTeamRequest request) {
    support.assertCanAccessOrg(id);
    support.assertCanAccessOrg(request.companyOrgId());
    if (request.workshopOrgId() != null) {
      support.assertCanAccessOrg(request.workshopOrgId());
    }
    String status = support.statusOrDraft(request.status());
    support.validateStatus(status);
    SysOrg org = support.requireOrg(id, "TEAM");
    support.assertOrgCodeAvailable(request.code(), id);
    String previousPath = org.orgPath;
    Long parentId = request.workshopOrgId() == null ? request.companyOrgId() : request.workshopOrgId();
    SysOrg parent = support.requireActiveOrg(parentId);
    org.parentId = parentId;
    org.orgCode = request.code();
    org.orgName = request.name();
    org.orgPath = parent.orgPath + org.id + "/";
    org.status = status;
    org.updatedAt = LocalDateTime.now();
    orgMapper.updateById(org);
    updateDescendantOrgPaths(previousPath, org.orgPath, org.id);
    SysTeamProfile profile = profileByOrg(id);
    if (profile == null) {
      profile = new SysTeamProfile();
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
    replaceMembers(id, request.teamMembers());
    auditLogService.record(SystemModule.TEAM, "SYS_ORG", id, "UPDATE", "更新班组 " + org.orgName);
    return toResponse(org, profile, support.orgMap());
  }

  @Transactional
  public SystemTeamResponse status(Long id, String status) {
    support.assertCanAccessOrg(id);
    SysOrg org = support.updateOrgStatus(id, "TEAM", status);
    auditLogService.record(SystemModule.TEAM, "SYS_ORG", id, "UPDATE", "更新班组状态 " + org.orgName);
    return toResponse(org, profileByOrg(id), support.orgMap());
  }

  @Transactional
  public void delete(Long id) {
    support.assertCanAccessOrg(id);
    SysOrg org = support.requireOrg(id, "TEAM");
    orgMapper.deleteById(id);
    SysTeamProfile profile = profileByOrg(id);
    if (profile != null) {
      profileMapper.deleteById(profile.id);
    }
    memberMapper.delete(new QueryWrapper<SysTeamMember>().eq("team_org_id", id));
    auditLogService.record(SystemModule.TEAM, "SYS_ORG", id, "DELETE", "删除班组 " + org.orgName);
  }

  private void apply(SysTeamProfile profile, SystemTeamRequest request) {
    profile.companyOrgId = request.companyOrgId();
    profile.workshopOrgId = request.workshopOrgId();
    profile.groupName = request.groupName();
    profile.level1Unit = request.level1Unit();
    profile.level2Unit = request.level2Unit();
    profile.workshopName = request.workshopName();
    profile.workTypeCode = request.workTypeCode();
    profile.workTypeName = request.workTypeName();
    profile.leaderUsername = request.leaderUsername();
    profile.safetyOfficerUsername = request.safetyOfficerUsername();
    profile.submitDate = request.submitDate();
    profile.applicantName = request.applicantName();
    profile.points = support.defaultInt(request.points());
    profile.active = Boolean.FALSE.equals(request.active()) ? 0 : 1;
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

  private void replaceMembers(Long teamOrgId, List<String> names) {
    memberMapper.delete(new QueryWrapper<SysTeamMember>().eq("team_org_id", teamOrgId));
    for (String name : names == null ? List.<String>of() : names) {
      if (name == null || name.isBlank()) {
        continue;
      }
      SysTeamMember member = new SysTeamMember();
      member.teamOrgId = teamOrgId;
      member.username = name.trim();
      member.memberName = name.trim();
      member.memberRole = "MEMBER";
      member.createdAt = LocalDateTime.now();
      member.deleted = 0;
      memberMapper.insert(member);
    }
  }

  private SystemTeamResponse toResponse(SysOrg org, SysTeamProfile profile, Map<Long, SysOrg> orgs) {
    return new SystemTeamResponse(
        org.id,
        org.orgCode,
        org.orgName,
        profile == null ? null : profile.companyOrgId,
        profile == null ? null : support.orgName(orgs, profile.companyOrgId),
        profile == null ? null : profile.workshopOrgId,
        profile == null ? null : support.orgName(orgs, profile.workshopOrgId),
        profile == null ? null : profile.groupName,
        profile == null ? null : profile.level1Unit,
        profile == null ? null : profile.level2Unit,
        profile == null ? null : profile.workshopName,
        profile == null ? null : profile.workTypeCode,
        profile == null ? null : profile.workTypeName,
        org.status,
        profile == null ? null : profile.leaderUsername,
        members(org.id),
        profile == null ? null : profile.safetyOfficerUsername,
        profile == null ? null : profile.submitDate,
        profile == null ? null : profile.applicantName,
        profile == null ? null : profile.points,
        profile == null || !Integer.valueOf(0).equals(profile.active));
  }

  private List<String> members(Long teamOrgId) {
    return new ArrayList<>(
        memberMapper.selectList(new QueryWrapper<SysTeamMember>().eq("team_org_id", teamOrgId).eq("deleted", 0)).stream()
            .map(member -> member.memberName)
            .toList());
  }

  private SysTeamProfile profileByOrg(Long orgId) {
    return profileMapper.selectOne(new QueryWrapper<SysTeamProfile>().eq("org_id", orgId).eq("deleted", 0).last("limit 1"));
  }

  private Map<Long, SysTeamProfile> profileMap() {
    return profileMapper.selectList(new QueryWrapper<SysTeamProfile>().eq("deleted", 0)).stream()
        .collect(Collectors.toMap(profile -> profile.orgId, Function.identity(), (left, right) -> left));
  }
}
