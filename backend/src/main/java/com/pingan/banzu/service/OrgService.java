package com.pingan.banzu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pingan.banzu.domain.SysOrg;
import com.pingan.banzu.dto.OrgNodeResponse;
import com.pingan.banzu.mapper.SysOrgMapper;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import com.pingan.banzu.system.domain.SysCompanyProfile;
import com.pingan.banzu.system.mapper.SysCompanyProfileMapper;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OrgService {
  private static final Set<String> COMPANY_NAVIGATION_TYPES = Set.of("集团", "分公司", "子公司");

  private final SysOrgMapper orgMapper;
  private final SysCompanyProfileMapper companyProfileMapper;

  public OrgService(SysOrgMapper orgMapper, SysCompanyProfileMapper companyProfileMapper) {
    this.orgMapper = orgMapper;
    this.companyProfileMapper = companyProfileMapper;
  }

  public List<OrgNodeResponse> tree() {
    CurrentUser user = CurrentUserContext.require();
    List<SysOrg> orgs =
        orgMapper.selectList(
            new QueryWrapper<SysOrg>().eq("status", "ACTIVE").eq("deleted", 0).orderByAsc("sort_order", "id"));
    if (!user.isAdmin()) {
      orgs = orgs.stream().filter(org -> isVisibleToUser(org, user)).toList();
    }
    Map<Long, List<SysOrg>> byParent = orgs.stream().collect(Collectors.groupingBy(org -> org.parentId == null ? 0L : org.parentId));
    Long rootParentId = 0L;
    return byParent.getOrDefault(rootParentId, List.of()).stream()
        .sorted(Comparator.comparing(org -> org.sortOrder))
        .map(org -> toNode(org, byParent))
        .toList();
  }

  public List<OrgNodeResponse> companyTree() {
    CurrentUser user = CurrentUserContext.require();
    Map<Long, SysCompanyProfile> profiles =
        companyProfileMapper.selectList(new QueryWrapper<SysCompanyProfile>().eq("deleted", 0)).stream()
            .collect(Collectors.toMap(profile -> profile.orgId, Function.identity(), (left, right) -> left));
    List<SysOrg> orgs =
        orgMapper.selectList(
                new QueryWrapper<SysOrg>()
                    .in("org_type", List.of("GROUP", "COMPANY"))
                    .eq("status", "ACTIVE")
                    .eq("deleted", 0)
                    .orderByAsc("org_path")
                    .orderByAsc("sort_order")
                    .orderByAsc("id"))
            .stream()
            .filter(org -> isVisibleToUser(org, user))
            .filter(org -> {
              SysCompanyProfile profile = profiles.get(org.id);
              return isCompanyNavigationProfile(profile);
            })
            .toList();
    Map<Long, SysOrg> orgById = orgs.stream().collect(Collectors.toMap(org -> org.id, Function.identity()));
    Map<Long, SysCompanyProfile> visibleProfiles =
        orgs.stream()
            .map(org -> profiles.get(org.id))
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(profile -> profile.orgId, Function.identity()));
    Map<Long, List<SysOrg>> byParent =
        orgs.stream()
            .collect(
                Collectors.groupingBy(
                    org -> companyNavigationParentId(org, profiles.get(org.id), orgById, visibleProfiles),
                    LinkedHashMap::new,
                    Collectors.toList()));
    return byParent.getOrDefault(0L, List.of()).stream()
        .sorted(companyNavigationComparator())
        .map(org -> toCompanyNode(org, profiles.get(org.id), byParent))
        .toList();
  }

  private OrgNodeResponse toNode(SysOrg org, Map<Long, List<SysOrg>> byParent) {
    List<OrgNodeResponse> children =
        byParent.getOrDefault(org.id, List.of()).stream()
            .sorted(Comparator.comparing(child -> child.sortOrder))
            .map(child -> toNode(child, byParent))
            .toList();
    return new OrgNodeResponse(String.valueOf(org.id), org.id, org.orgName, org.orgType, null, children);
  }

  private OrgNodeResponse toCompanyNode(
      SysOrg org, SysCompanyProfile profile, Map<Long, List<SysOrg>> byParent) {
    List<OrgNodeResponse> children =
        byParent.getOrDefault(org.id, List.of()).stream()
            .sorted(companyNavigationComparator())
            .map(child -> toCompanyNode(child, profileFor(child.id), byParent))
            .toList();
    return new OrgNodeResponse(
        String.valueOf(org.id), org.id, org.orgName, org.orgType, profile.companyType, children);
  }

  private SysCompanyProfile profileFor(Long orgId) {
    return companyProfileMapper.selectOne(
        new QueryWrapper<SysCompanyProfile>().eq("org_id", orgId).eq("deleted", 0).last("limit 1"));
  }

  private Long companyNavigationParentId(
      SysOrg org,
      SysCompanyProfile profile,
      Map<Long, SysOrg> orgById,
      Map<Long, SysCompanyProfile> profiles) {
    if (profile == null) {
      return 0L;
    }
    Long parentId =
        switch (profile.companyType) {
          case "集团" -> isBlank(profile.level2Name) ? null : findParentId(profiles, orgById, "集团", profile.level1Name);
          case "分公司" -> findParentId(profiles, orgById, "集团", profile.level2Name);
          case "子公司" -> findParentId(profiles, orgById, "分公司", profile.level3Name);
          default -> null;
        };
    return parentId == null || org.id.equals(parentId) ? 0L : parentId;
  }

  private Long findParentId(
      Map<Long, SysCompanyProfile> profiles, Map<Long, SysOrg> orgById, String companyType, String name) {
    if (isBlank(name)) {
      return null;
    }
    return profiles.values().stream()
        .filter(profile -> companyType.equals(profile.companyType))
        .filter(profile -> matchesProfileName(orgById.get(profile.orgId), profile, name))
        .map(profile -> profile.orgId)
        .findFirst()
        .orElse(null);
  }

  private boolean matchesProfileName(SysOrg org, SysCompanyProfile profile, String name) {
    String normalizedName = trimToNull(name);
    return normalizedName != null
        && (normalizedName.equals(trimToNull(org == null ? null : org.orgName))
            || normalizedName.equals(trimToNull(profile.shortName))
            || normalizedName.equals(trimToNull(profile.level1Name))
            || normalizedName.equals(trimToNull(profile.level2Name))
            || normalizedName.equals(trimToNull(profile.level3Name))
            || normalizedName.equals(trimToNull(profile.level4Name)));
  }

  private Comparator<SysOrg> companyNavigationComparator() {
    return Comparator.comparing((SysOrg org) -> org.sortOrder == null ? 0 : org.sortOrder)
        .thenComparing(org -> org.id);
  }

  private boolean isCompanyNavigationProfile(SysCompanyProfile profile) {
    return profile != null
        && profile.companyType != null
        && COMPANY_NAVIGATION_TYPES.contains(profile.companyType);
  }

  private boolean isVisibleToUser(SysOrg org, CurrentUser user) {
    if (user.isAdmin()) {
      return true;
    }
    return org.orgPath != null
        && (org.orgPath.startsWith(user.orgPath()) || user.orgPath().startsWith(org.orgPath));
  }

  private String trimToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private boolean isBlank(String value) {
    return trimToNull(value) == null;
  }
}
