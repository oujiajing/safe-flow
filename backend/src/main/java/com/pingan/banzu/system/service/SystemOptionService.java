package com.pingan.banzu.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pingan.banzu.domain.SysOrg;
import com.pingan.banzu.mapper.SysOrgMapper;
import java.util.List;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class SystemOptionService {
  private final SysOrgMapper orgMapper;
  private final SystemMasterDataSupport support;

  public SystemOptionService(SysOrgMapper orgMapper, ApplicationContext applicationContext) {
    this.orgMapper = orgMapper;
    this.support = new SystemMasterDataSupport(orgMapper, applicationContext);
  }

  public List<OptionItem> companies() {
    return orgOptions("COMPANY", null);
  }

  public List<OptionItem> departments(Long companyOrgId) {
    return orgOptions("DEPARTMENT", companyOrgId);
  }

  public List<OptionItem> teams(Long parentOrgId) {
    return orgOptions("TEAM", parentOrgId);
  }

  private List<OptionItem> orgOptions(String orgType, Long parentId) {
    return orgMapper.selectList(new QueryWrapper<SysOrg>().eq("org_type", orgType).eq("status", "ACTIVE").eq("deleted", 0).orderByAsc("sort_order"))
        .stream()
        .filter(org -> parentId == null || parentId.equals(org.parentId) || org.orgPath.contains("/" + parentId + "/"))
        .filter(org -> support.canAccessOrg(org.id))
        .map(org -> new OptionItem(org.id, org.orgName, org.orgCode))
        .toList();
  }

  public record OptionItem(Long value, String label, String code) {}
}
