package com.pingan.banzu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pingan.banzu.domain.SysUser;
import com.pingan.banzu.domain.ThreeCheckRecord;
import com.pingan.banzu.mapper.SysUserMapper;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.system.domain.SysTeamMember;
import com.pingan.banzu.system.domain.SysTeamProfile;
import com.pingan.banzu.system.mapper.SysTeamMemberMapper;
import com.pingan.banzu.system.mapper.SysTeamProfileMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ThreeCheckAccessPolicyService {

  public static final String ROLE_TEAM_LEADER = "TEAM_LEADER";
  public static final String ROLE_TEAM_MEMBER = "TEAM_MEMBER";
  public static final String ROLE_CURTAIN_WALL_LEADER = "CURTAIN_WALL_LEADER";
  private static final Set<String> BROADER_THREE_CHECK_ROLES =
      Set.of(
          "GROUP_LEADER",
          "ENTERPRISE_LEADER",
          "COMPANY_LEADER",
          "DEPARTMENT_MANAGER",
          "WORKSHOP_DIRECTOR",
          "SAFETY_OFFICER",
          "ADMIN");

  private final SysTeamProfileMapper teamProfileMapper;
  private final SysTeamMemberMapper teamMemberMapper;
  private final SysUserMapper userMapper;

  public ThreeCheckAccessPolicyService(
      SysTeamProfileMapper teamProfileMapper,
      SysTeamMemberMapper teamMemberMapper,
      SysUserMapper userMapper) {
    this.teamProfileMapper = teamProfileMapper;
    this.teamMemberMapper = teamMemberMapper;
    this.userMapper = userMapper;
  }

  public boolean isTeamLeader(CurrentUser user) {
    return hasRole(user, ROLE_TEAM_LEADER);
  }

  public boolean isCurtainWallLeader(CurrentUser user) {
    return hasRole(user, ROLE_CURTAIN_WALL_LEADER);
  }

  public boolean isTeamMember(CurrentUser user) {
    return hasRole(user, ROLE_TEAM_MEMBER);
  }

  public boolean shouldApplyTeamLeaderNarrowing(CurrentUser user, String currentDataScope) {
    return !"SELF".equals(currentDataScope)
        && isTeamLeader(user)
        && !hasBroaderThreeCheckRole(user);
  }

  public boolean shouldApplyCurtainWallNarrowing(CurrentUser user, String currentDataScope) {
    return !"SELF".equals(currentDataScope)
        && isCurtainWallLeader(user)
        && !hasBroaderThreeCheckRole(user);
  }

  public List<Long> visibleTeamIds(CurrentUser user) {
    if (!isTeamLeader(user)) {
      return null;
    }
    Set<Long> teamIds = new LinkedHashSet<>();
    if (user.orgId() != null) {
      teamIds.add(user.orgId());
    }
    teamIds.addAll(teamIdsFromProfiles(user.username()));
    teamIds.addAll(teamIdsFromMembers(user.userId(), user.username()));
    return new ArrayList<>(teamIds);
  }

  public List<Long> visibleOwnerUserIds(CurrentUser user) {
    if (!isTeamLeader(user)) {
      return null;
    }
    return List.of(user.userId());
  }

  public List<Long> visibleTeamMemberTeamIds(CurrentUser user) {
    if (!isTeamMember(user)) {
      return null;
    }
    Set<Long> teamIds = new LinkedHashSet<>();
    if (user.orgId() != null) {
      teamIds.add(user.orgId());
    }
    teamIds.addAll(teamIdsFromMemberRecords(user.userId(), user.username()));
    return new ArrayList<>(teamIds);
  }

  public boolean canAccessAsTeamMember(ThreeCheckRecord record, CurrentUser user) {
    if (!isTeamMember(user)) {
      return false;
    }
    List<Long> visibleTeamIds = visibleTeamMemberTeamIds(user);
    boolean teamVisible = record.teamId != null && visibleTeamIds.contains(record.teamId);
    boolean ownerVisible = record.ownerUserId != null && record.ownerUserId.equals(user.userId());
    return teamVisible || ownerVisible;
  }

  public boolean canAccessAsTeamLeader(ThreeCheckRecord record, CurrentUser user) {
    if (!isTeamLeader(user)) {
      return false;
    }
    List<Long> visibleTeamIds = visibleTeamIds(user);
    boolean teamVisible = record.teamId != null && visibleTeamIds.contains(record.teamId);
    boolean ownerVisible = record.ownerUserId != null && record.ownerUserId.equals(user.userId());
    return teamVisible || ownerVisible;
  }

  public boolean isCurtainWallModule(String moduleKey) {
    return moduleKey != null && moduleKey.contains("curtain-wall");
  }

  public boolean isCurtainWallDispatchRoot(ThreeCheckRecord record) {
    return record != null && "curtain-wall-team-dispatch".equals(record.moduleKey);
  }

  public Long resolveTeamLeaderUserId(Long teamId) {
    if (teamId == null) {
      return null;
    }
    SysTeamProfile profile =
        teamProfileMapper.selectOne(
            new QueryWrapper<SysTeamProfile>().eq("org_id", teamId).eq("deleted", 0));
    if (profile == null || isBlank(profile.leaderUsername)) {
      return null;
    }
    String username = profile.leaderUsername.split(";")[0].trim();
    if (username.isBlank()) {
      return null;
    }
    SysUser user =
        userMapper.selectOne(
            new QueryWrapper<SysUser>().eq("username", username).eq("deleted", 0));
    return user == null ? null : user.id;
  }

  private List<Long> teamIdsFromProfiles(String username) {
    if (isBlank(username)) {
      return List.of();
    }
    return teamProfileMapper
        .selectList(new QueryWrapper<SysTeamProfile>().eq("deleted", 0).like("leader_username", username))
        .stream()
        .filter(profile -> containsUsername(profile.leaderUsername, username))
        .map(profile -> profile.orgId)
        .filter(id -> id != null)
        .toList();
  }

  private List<Long> teamIdsFromMembers(Long userId, String username) {
    QueryWrapper<SysTeamMember> wrapper =
        new QueryWrapper<SysTeamMember>().eq("deleted", 0).eq("member_role", "LEADER");
    wrapper.and(
        query ->
            query.eq(userId != null, "user_id", userId)
                .or()
                .eq(!isBlank(username), "username", username));
    return teamMemberMapper.selectList(wrapper).stream()
        .map(member -> member.teamOrgId)
        .filter(id -> id != null)
        .toList();
  }

  private List<Long> teamIdsFromMemberRecords(Long userId, String username) {
    QueryWrapper<SysTeamMember> wrapper =
        new QueryWrapper<SysTeamMember>().eq("deleted", 0);
    wrapper.and(
        query ->
            query.eq(userId != null, "user_id", userId)
                .or()
                .eq(!isBlank(username), "username", username));
    return teamMemberMapper.selectList(wrapper).stream()
        .map(member -> member.teamOrgId)
        .filter(id -> id != null)
        .toList();
  }

  private boolean hasRole(CurrentUser user, String roleCode) {
    return user.roles() != null && user.roles().contains(roleCode);
  }

  private boolean hasBroaderThreeCheckRole(CurrentUser user) {
    if (user.isAdmin() || user.roles() == null) {
      return user.isAdmin();
    }
    return user.roles().stream().anyMatch(BROADER_THREE_CHECK_ROLES::contains);
  }

  private boolean containsUsername(String usernames, String username) {
    if (isBlank(usernames) || isBlank(username)) {
      return false;
    }
    for (String current : usernames.split(";")) {
      if (username.equals(current.trim())) {
        return true;
      }
    }
    return false;
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
