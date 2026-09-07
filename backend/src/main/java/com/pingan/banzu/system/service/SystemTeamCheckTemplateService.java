package com.pingan.banzu.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.domain.SysOrg;
import com.pingan.banzu.mapper.SysOrgMapper;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import com.pingan.banzu.system.audit.AuditLogService;
import com.pingan.banzu.system.common.SystemModule;
import com.pingan.banzu.system.domain.SysCheckItemLibrary;
import com.pingan.banzu.system.domain.SysTeamCheckTemplate;
import com.pingan.banzu.system.domain.SysTeamCheckTemplateItem;
import com.pingan.banzu.system.dto.SystemCheckItemLibraryRequest;
import com.pingan.banzu.system.dto.SystemCheckItemLibraryResponse;
import com.pingan.banzu.system.dto.SystemTeamCheckTemplateItemRequest;
import com.pingan.banzu.system.dto.SystemTeamCheckTemplateItemResponse;
import com.pingan.banzu.system.dto.SystemTeamCheckTemplateQuery;
import com.pingan.banzu.system.dto.SystemTeamCheckTemplateRequest;
import com.pingan.banzu.system.dto.SystemTeamCheckTemplateResponse;
import com.pingan.banzu.system.mapper.SysCheckItemLibraryMapper;
import com.pingan.banzu.system.mapper.SysTeamCheckTemplateItemMapper;
import com.pingan.banzu.system.mapper.SysTeamCheckTemplateMapper;
import com.pingan.banzu.system.security.SystemDataScopeService;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemTeamCheckTemplateService {

  public static final String BIZ_TYPE = "SYS_TEAM_CHECK_ITEM_TEMPLATE";
  private static final Set<String> STATUSES = Set.of("DRAFT", "ACTIVE", "INACTIVE");
  private static final Set<String> STAGES =
      Set.of(
          "PRE_SHIFT_MEETING_CONFIRMATION",
          "PRE_SHIFT_INSPECTION",
          "MID_SHIFT_INSPECTION",
          "POST_SHIFT_INSPECTION",
          "KEY_SITES");

  private final AuditLogService auditLogService;
  private final SysCheckItemLibraryMapper libraryMapper;
  private final SysOrgMapper orgMapper;
  private final SysTeamCheckTemplateItemMapper templateItemMapper;
  private final SysTeamCheckTemplateMapper templateMapper;
  private final SystemDataScopeService dataScopeService;

  public SystemTeamCheckTemplateService(
      AuditLogService auditLogService,
      SysCheckItemLibraryMapper libraryMapper,
      SysOrgMapper orgMapper,
      SysTeamCheckTemplateItemMapper templateItemMapper,
      SysTeamCheckTemplateMapper templateMapper,
      SystemDataScopeService dataScopeService) {
    this.auditLogService = auditLogService;
    this.libraryMapper = libraryMapper;
    this.orgMapper = orgMapper;
    this.templateItemMapper = templateItemMapper;
    this.templateMapper = templateMapper;
    this.dataScopeService = dataScopeService;
  }

  public PageResult<SystemCheckItemLibraryResponse> listLibrary(
      String stage, String status, String keyword, Integer page, Integer pageSize) {
    QueryWrapper<SysCheckItemLibrary> wrapper = new QueryWrapper<SysCheckItemLibrary>().eq("deleted", 0);
    if (!blank(stage) && !"all".equalsIgnoreCase(stage)) {
      validateStage(stage);
      wrapper.eq("applicable_stage", stage);
    }
    if (!blank(status) && !"all".equalsIgnoreCase(status)) {
      validateStatus(status);
      wrapper.eq("status", status);
    }
    if (!blank(keyword)) {
      String value = keyword.trim();
      wrapper.and(nested -> nested.like("risk_type", value).or().like("check_item", value));
    }
    wrapper.orderByAsc("sort_order").orderByDesc("id");
    List<SystemCheckItemLibraryResponse> rows =
        libraryMapper.selectList(wrapper).stream().map(this::libraryResponse).toList();
    return page(rows, page, pageSize);
  }

  @Transactional
  public SystemCheckItemLibraryResponse createLibrary(SystemCheckItemLibraryRequest request) {
    if (request == null) {
      throw new BusinessException("检查项不能为空");
    }
    validateStage(request.applicableStage());
    validateRiskTypeForStage(request.riskType(), request.applicableStage());
    validateStatus(statusOrActive(request.status()));
    CurrentUser user = CurrentUserContext.require();
    LocalDateTime now = LocalDateTime.now();
    SysCheckItemLibrary item = new SysCheckItemLibrary();
    item.riskType = text(request.riskType());
    item.checkItem = text(request.checkItem());
    item.applicableStage = request.applicableStage().trim();
    item.defaultCheckResult = defaultText(request.defaultCheckResult(), "无隐患");
    item.defaultRectificationDescription = text(request.defaultRectificationDescription());
    item.defaultFollowUpPlan = text(request.defaultFollowUpPlan());
    item.requireImage = bool(request.requireImage());
    item.requireVideo = bool(request.requireVideo());
    item.sortOrder = request.sortOrder() == null ? 0 : request.sortOrder();
    item.status = statusOrActive(request.status());
    item.createdBy = user.userId();
    item.updatedBy = user.userId();
    item.createdAt = now;
    item.updatedAt = now;
    item.deleted = 0L;
    libraryMapper.insert(item);
    auditLogService.record(
        SystemModule.TEAM_CHECK_ITEM_TEMPLATE, BIZ_TYPE, item.id, "CREATE_LIBRARY", "新增检查项");
    return libraryResponse(item);
  }

  @Transactional
  public SystemCheckItemLibraryResponse updateLibrary(Long id, SystemCheckItemLibraryRequest request) {
    SysCheckItemLibrary item = requireLibrary(id);
    if (request == null) {
      throw new BusinessException("检查项不能为空");
    }
    validateStage(request.applicableStage());
    validateRiskTypeForStage(request.riskType(), request.applicableStage());
    validateStatus(statusOrActive(request.status()));
    item.riskType = text(request.riskType());
    item.checkItem = text(request.checkItem());
    item.applicableStage = request.applicableStage().trim();
    item.defaultCheckResult = defaultText(request.defaultCheckResult(), "无隐患");
    item.defaultRectificationDescription = text(request.defaultRectificationDescription());
    item.defaultFollowUpPlan = text(request.defaultFollowUpPlan());
    item.requireImage = bool(request.requireImage());
    item.requireVideo = bool(request.requireVideo());
    item.sortOrder = request.sortOrder() == null ? 0 : request.sortOrder();
    item.status = statusOrActive(request.status());
    item.updatedBy = CurrentUserContext.require().userId();
    item.updatedAt = LocalDateTime.now();
    libraryMapper.updateById(item);
    auditLogService.record(
        SystemModule.TEAM_CHECK_ITEM_TEMPLATE, BIZ_TYPE, item.id, "UPDATE_LIBRARY", "更新检查项");
    return libraryResponse(requireLibrary(id));
  }

  @Transactional
  public SystemCheckItemLibraryResponse libraryStatus(Long id, String status) {
    SysCheckItemLibrary item = requireLibrary(id);
    validateStatus(status);
    item.status = status;
    item.updatedBy = CurrentUserContext.require().userId();
    item.updatedAt = LocalDateTime.now();
    libraryMapper.updateById(item);
    auditLogService.record(
        SystemModule.TEAM_CHECK_ITEM_TEMPLATE, BIZ_TYPE, item.id, "UPDATE_LIBRARY_STATUS", "更新检查项状态");
    return libraryResponse(requireLibrary(id));
  }

  @Transactional
  public void deleteLibrary(Long id) {
    SysCheckItemLibrary item = requireLibrary(id);
    libraryMapper.update(
        null,
        new UpdateWrapper<SysCheckItemLibrary>()
            .eq("id", item.id)
            .eq("deleted", 0)
            .set("deleted", item.id)
            .set("updated_by", CurrentUserContext.require().userId())
            .set("updated_at", LocalDateTime.now()));
    auditLogService.record(
        SystemModule.TEAM_CHECK_ITEM_TEMPLATE, BIZ_TYPE, item.id, "DELETE_LIBRARY", "删除检查项");
  }

  public PageResult<SystemTeamCheckTemplateResponse> list(SystemTeamCheckTemplateQuery query) {
    SystemTeamCheckTemplateQuery safe =
        query == null
            ? new SystemTeamCheckTemplateQuery(null, null, null, null, null, null, null, null, null, null)
            : query;
    QueryWrapper<SysTeamCheckTemplate> wrapper = new QueryWrapper<SysTeamCheckTemplate>().eq("deleted", 0);
    if (Boolean.TRUE.equals(safe.exactScope()) && safe.companyOrgId() != null) {
      wrapper.eq("company_org_id", safe.companyOrgId());
      nullEq(wrapper, "department_org_id", safe.departmentOrgId());
      nullEq(wrapper, "team_org_id", safe.teamOrgId());
    } else {
      if (safe.companyOrgId() != null) {
        wrapper.eq("company_org_id", safe.companyOrgId());
      }
      if (safe.departmentOrgId() != null) {
        wrapper.eq("department_org_id", safe.departmentOrgId());
      }
      if (safe.teamOrgId() != null) {
        wrapper.eq("team_org_id", safe.teamOrgId());
      }
    }
    if (safe.organizationId() != null) {
      wrapper.and(
          nested ->
              nested
                  .eq("company_org_id", safe.organizationId())
                  .or()
                  .eq("department_org_id", safe.organizationId())
                  .or()
                  .eq("team_org_id", safe.organizationId()));
    }
    if (!blank(safe.stage()) && !"all".equalsIgnoreCase(safe.stage())) {
      validateStage(safe.stage());
      wrapper.eq("inspection_stage", safe.stage());
    }
    if (!blank(safe.status()) && !"all".equalsIgnoreCase(safe.status())) {
      validateStatus(safe.status());
      wrapper.eq("status", safe.status());
    }
    if (!blank(safe.keyword())) {
      String keyword = safe.keyword().trim();
      wrapper.and(nested -> nested.like("name", keyword));
    }
    wrapper.orderByDesc("id");
    Map<Long, SysOrg> orgs = orgMap();
    List<SystemTeamCheckTemplateResponse> rows =
        templateMapper.selectList(wrapper).stream()
            .filter(template -> dataScopeService.canAccessOrg(accessOrgId(template)))
            .map(template -> response(template, orgs))
            .toList();
    return page(rows, safe.page(), safe.pageSize());
  }

  @Transactional
  public SystemTeamCheckTemplateResponse create(SystemTeamCheckTemplateRequest request) {
    validateRequest(request);
    SysOrg company = requireOrg(request.companyOrgId(), "COMPANY", "所属公司不存在");
    SysOrg department = request.departmentOrgId() == null ? null : requireOrg(request.departmentOrgId(), "DEPARTMENT", "所属部门不存在");
    SysOrg team = request.teamOrgId() == null ? null : requireOrg(request.teamOrgId(), "TEAM", "所属班组不存在");
    validateHierarchy(company, department, team);
    dataScopeService.assertCanAccessOrg(accessOrgId(request));
    assertUniqueScope(request, null);

    CurrentUser user = CurrentUserContext.require();
    LocalDateTime now = LocalDateTime.now();
    SysTeamCheckTemplate template = new SysTeamCheckTemplate();
    apply(template, request);
    template.version = request.version() == null ? 1 : request.version();
    template.createdBy = user.userId();
    template.updatedBy = user.userId();
    template.createdAt = now;
    template.updatedAt = now;
    template.deleted = 0L;
    templateMapper.insert(template);
    replaceItems(template.id, request.items(), user.userId());
    auditLogService.record(
        SystemModule.TEAM_CHECK_ITEM_TEMPLATE, BIZ_TYPE, template.id, "CREATE", "新增班组检查项模板");
    return response(requireTemplate(template.id), orgMap());
  }

  public SystemTeamCheckTemplateResponse detail(Long id) {
    return response(requireTemplate(id), orgMap());
  }

  @Transactional
  public SystemTeamCheckTemplateResponse update(Long id, SystemTeamCheckTemplateRequest request) {
    SysTeamCheckTemplate template = requireTemplate(id);
    validateRequest(request);
    SysOrg company = requireOrg(request.companyOrgId(), "COMPANY", "所属公司不存在");
    SysOrg department = request.departmentOrgId() == null ? null : requireOrg(request.departmentOrgId(), "DEPARTMENT", "所属部门不存在");
    SysOrg team = request.teamOrgId() == null ? null : requireOrg(request.teamOrgId(), "TEAM", "所属班组不存在");
    validateHierarchy(company, department, team);
    dataScopeService.assertCanAccessOrg(accessOrgId(request));
    assertUniqueScope(request, id);

    apply(template, request);
    template.version = template.version == null ? 1 : template.version + 1;
    template.updatedBy = CurrentUserContext.require().userId();
    template.updatedAt = LocalDateTime.now();
    templateMapper.updateById(template);
    replaceItems(template.id, request.items(), template.updatedBy);
    auditLogService.record(
        SystemModule.TEAM_CHECK_ITEM_TEMPLATE, BIZ_TYPE, template.id, "UPDATE", "更新班组检查项模板");
    return response(requireTemplate(template.id), orgMap());
  }

  @Transactional
  public SystemTeamCheckTemplateResponse status(Long id, String status) {
    SysTeamCheckTemplate template = requireTemplate(id);
    validateStatus(status);
    template.status = status;
    template.updatedBy = CurrentUserContext.require().userId();
    template.updatedAt = LocalDateTime.now();
    templateMapper.updateById(template);
    auditLogService.record(
        SystemModule.TEAM_CHECK_ITEM_TEMPLATE, BIZ_TYPE, template.id, "UPDATE_STATUS", "更新模板状态");
    return response(requireTemplate(id), orgMap());
  }

  @Transactional
  public void delete(Long id) {
    SysTeamCheckTemplate template = requireTemplate(id);
    Long operatorId = CurrentUserContext.require().userId();
    LocalDateTime now = LocalDateTime.now();
    templateMapper.update(
        null,
        new UpdateWrapper<SysTeamCheckTemplate>()
            .eq("id", template.id)
            .eq("deleted", 0)
            .set("deleted", template.id)
            .set("updated_by", operatorId)
            .set("updated_at", now));
    templateItemMapper.update(
        null,
        new UpdateWrapper<SysTeamCheckTemplateItem>()
            .eq("template_id", template.id)
            .eq("deleted", 0)
            .set("deleted", template.id)
            .set("updated_by", operatorId)
            .set("updated_at", now));
    auditLogService.record(
        SystemModule.TEAM_CHECK_ITEM_TEMPLATE, BIZ_TYPE, template.id, "DELETE", "删除班组检查项模板");
  }

  @Transactional
  public SystemTeamCheckTemplateResponse copy(Long sourceId, SystemTeamCheckTemplateRequest target) {
    SysTeamCheckTemplate source = requireTemplate(sourceId);
    List<SysTeamCheckTemplateItem> sourceItems = activeItems(source.id);
    List<SystemTeamCheckTemplateItemRequest> copiedItems =
        sourceItems.stream()
            .map(
                item ->
                    new SystemTeamCheckTemplateItemRequest(
                        item.libraryItemId,
                        item.riskType,
                        item.checkItem,
                        item.defaultCheckResult,
                        item.defaultRectificationDescription,
                        item.defaultFollowUpPlan,
                        intBool(item.requireImage),
                        intBool(item.requireVideo),
                        item.sortOrder))
            .toList();
    SystemTeamCheckTemplateRequest request =
        new SystemTeamCheckTemplateRequest(
            target.name(),
            target.companyOrgId(),
            target.departmentOrgId(),
            target.teamOrgId(),
            target.inspectionStage(),
            statusOrActive(target.status()),
            target.version(),
            target.items() == null || target.items().isEmpty() ? copiedItems : target.items());
    return create(request);
  }

  public SystemTeamCheckTemplateResponse resolve(Long companyOrgId, Long departmentOrgId, Long teamOrgId, String stage) {
    validateStage(stage);
    if (companyOrgId == null) {
      throw new BusinessException("所属公司不能为空");
    }
    dataScopeService.assertCanAccessOrg(teamOrgId != null ? teamOrgId : departmentOrgId != null ? departmentOrgId : companyOrgId);
    SysTeamCheckTemplate template = selectActive(companyOrgId, departmentOrgId, teamOrgId, stage);
    return template == null ? null : response(template, orgMap());
  }

  public List<SystemTeamCheckTemplateItemResponse> resolveItemsForTask(
      Long companyOrgId, Long departmentOrgId, Long teamOrgId, String moduleKey) {
    String stage = stageFromModuleKey(moduleKey);
    if (stage == null || companyOrgId == null) {
      return List.of();
    }
    SysTeamCheckTemplate template = selectActive(companyOrgId, departmentOrgId, teamOrgId, stage);
    if (template == null) {
      return List.of();
    }
    return activeItems(template.id).stream().map(this::itemResponse).toList();
  }

  private SysTeamCheckTemplate selectActive(Long companyOrgId, Long departmentOrgId, Long teamOrgId, String stage) {
    Long effectiveCompanyOrgId = effectiveCompanyOrgId(companyOrgId, departmentOrgId, teamOrgId);
    QueryWrapper<SysTeamCheckTemplate> wrapper =
        new QueryWrapper<SysTeamCheckTemplate>()
            .eq("company_org_id", effectiveCompanyOrgId)
            .eq("inspection_stage", stage)
            .eq("status", "ACTIVE")
            .eq("deleted", 0)
            .last("limit 1");
    nullEq(wrapper, "department_org_id", departmentOrgId);
    nullEq(wrapper, "team_org_id", teamOrgId);
    return templateMapper.selectOne(wrapper);
  }

  private Long effectiveCompanyOrgId(Long companyOrgId, Long departmentOrgId, Long teamOrgId) {
    Long scopedOrgId = teamOrgId != null ? teamOrgId : departmentOrgId;
    Long nearestCompanyOrgId = nearestCompanyOrgId(scopedOrgId);
    if (nearestCompanyOrgId == null || nearestCompanyOrgId.equals(companyOrgId)) {
      return companyOrgId;
    }
    SysOrg nearestCompany = orgMapper.selectById(nearestCompanyOrgId);
    if (nearestCompany != null && belongsTo(nearestCompany, companyOrgId)) {
      return nearestCompanyOrgId;
    }
    return companyOrgId;
  }

  private Long nearestCompanyOrgId(Long orgId) {
    Long currentId = orgId;
    while (currentId != null) {
      SysOrg org = orgMapper.selectById(currentId);
      if (org == null || Integer.valueOf(1).equals(org.deleted)) {
        return null;
      }
      if ("COMPANY".equals(org.orgType)) {
        return org.id;
      }
      currentId = org.parentId;
    }
    return null;
  }

  private void validateRequest(SystemTeamCheckTemplateRequest request) {
    if (request == null) {
      throw new BusinessException("模板不能为空");
    }
    validateStage(request.inspectionStage());
    validateStatus(statusOrActive(request.status()));
    if (request.items() == null || request.items().isEmpty()) {
      throw new BusinessException("模板检查项不能为空");
    }
    for (SystemTeamCheckTemplateItemRequest item : request.items()) {
      if (item == null) {
        throw new BusinessException("模板检查项不能为空");
      }
      validateRiskTypeForStage(item.riskType(), request.inspectionStage());
    }
  }

  private void apply(SysTeamCheckTemplate template, SystemTeamCheckTemplateRequest request) {
    template.name = text(request.name());
    template.companyOrgId = request.companyOrgId();
    template.departmentOrgId = request.departmentOrgId();
    template.teamOrgId = request.teamOrgId();
    template.inspectionStage = request.inspectionStage().trim();
    template.status = statusOrActive(request.status());
  }

  private void replaceItems(Long templateId, List<SystemTeamCheckTemplateItemRequest> requests, Long operatorId) {
    LocalDateTime now = LocalDateTime.now();
    templateItemMapper.update(
        null,
        new UpdateWrapper<SysTeamCheckTemplateItem>()
            .eq("template_id", templateId)
            .eq("deleted", 0)
            .set("deleted", templateId)
            .set("updated_by", operatorId)
            .set("updated_at", now));
    int index = 0;
    for (SystemTeamCheckTemplateItemRequest request : requests) {
      if (request == null) {
        throw new BusinessException("模板检查项不能为空");
      }
      SysTeamCheckTemplateItem item = new SysTeamCheckTemplateItem();
      item.templateId = templateId;
      item.libraryItemId = request.libraryItemId();
      item.riskType = text(request.riskType());
      item.checkItem = text(request.checkItem());
      item.defaultCheckResult = defaultText(request.defaultCheckResult(), "无隐患");
      item.defaultRectificationDescription = text(request.defaultRectificationDescription());
      item.defaultFollowUpPlan = text(request.defaultFollowUpPlan());
      item.requireImage = bool(request.requireImage());
      item.requireVideo = bool(request.requireVideo());
      item.sortOrder = request.sortOrder() == null ? ++index : request.sortOrder();
      item.createdBy = operatorId;
      item.updatedBy = operatorId;
      item.createdAt = now;
      item.updatedAt = now;
      item.deleted = 0L;
      templateItemMapper.insert(item);
    }
  }

  private SysTeamCheckTemplate requireTemplate(Long id) {
    SysTeamCheckTemplate template = templateMapper.selectById(id);
    if (template == null || !Long.valueOf(0).equals(template.deleted)) {
      throw new BusinessException("班组检查项模板不存在");
    }
    dataScopeService.assertCanAccessOrg(accessOrgId(template));
    return template;
  }

  private SysCheckItemLibrary requireLibrary(Long id) {
    SysCheckItemLibrary item = libraryMapper.selectById(id);
    if (item == null || !Long.valueOf(0).equals(item.deleted)) {
      throw new BusinessException("检查项不存在");
    }
    return item;
  }

  private SysOrg requireOrg(Long id, String orgType, String message) {
    if (id == null) {
      throw new BusinessException(message);
    }
    SysOrg org = orgMapper.selectById(id);
    if (org == null || Integer.valueOf(1).equals(org.deleted) || !orgType.equals(org.orgType)) {
      throw new BusinessException(message);
    }
    return org;
  }

  private void validateHierarchy(SysOrg company, SysOrg department, SysOrg team) {
    if (department != null && !belongsTo(department, company.id)) {
      throw new BusinessException("部门不属于所选公司");
    }
    if (team != null && department != null && !belongsTo(team, department.id)) {
      throw new BusinessException("班组不属于所选部门");
    }
    if (team != null && !belongsTo(team, company.id)) {
      throw new BusinessException("班组不属于所选公司");
    }
  }

  private boolean belongsTo(SysOrg org, Long ancestorId) {
    return org.parentId != null && org.parentId.equals(ancestorId)
        || (org.orgPath != null && org.orgPath.contains("/" + ancestorId + "/"));
  }

  private void assertUniqueScope(SystemTeamCheckTemplateRequest request, Long selfId) {
    QueryWrapper<SysTeamCheckTemplate> wrapper =
        new QueryWrapper<SysTeamCheckTemplate>()
            .eq("company_org_id", request.companyOrgId())
            .eq("inspection_stage", request.inspectionStage())
            .eq("deleted", 0);
    nullEq(wrapper, "department_org_id", request.departmentOrgId());
    nullEq(wrapper, "team_org_id", request.teamOrgId());
    if (selfId != null) {
      wrapper.ne("id", selfId);
    }
    if (templateMapper.selectCount(wrapper) > 0) {
      throw new BusinessException("该组织范围和检查阶段已存在模板");
    }
  }

  private List<SysTeamCheckTemplateItem> activeItems(Long templateId) {
    return templateItemMapper.selectList(
        new QueryWrapper<SysTeamCheckTemplateItem>()
            .eq("template_id", templateId)
            .eq("deleted", 0)
            .orderByAsc("sort_order")
            .orderByAsc("id"));
  }

  private SystemTeamCheckTemplateResponse response(SysTeamCheckTemplate template, Map<Long, SysOrg> orgs) {
    SysOrg company = orgs.get(template.companyOrgId);
    SysOrg department = template.departmentOrgId == null ? null : orgs.get(template.departmentOrgId);
    SysOrg team = template.teamOrgId == null ? null : orgs.get(template.teamOrgId);
    String scope = scope(template);
    return new SystemTeamCheckTemplateResponse(
        template.id,
        template.name,
        template.companyOrgId,
        company == null ? "" : company.orgName,
        template.departmentOrgId,
        department == null ? "" : department.orgName,
        template.teamOrgId,
        team == null ? "" : team.orgName,
        scope,
        scopeName(scope),
        template.inspectionStage,
        stageLabel(template.inspectionStage),
        template.status,
        statusLabel(template.status),
        template.version,
        activeItems(template.id).stream().map(this::itemResponse).toList(),
        template.createdAt,
        template.updatedAt);
  }

  private SystemTeamCheckTemplateItemResponse itemResponse(SysTeamCheckTemplateItem item) {
    return new SystemTeamCheckTemplateItemResponse(
        item.id,
        item.libraryItemId,
        item.riskType,
        item.checkItem,
        item.defaultCheckResult,
        item.defaultRectificationDescription,
        item.defaultFollowUpPlan,
        intBool(item.requireImage),
        intBool(item.requireVideo),
        item.sortOrder);
  }

  private SystemCheckItemLibraryResponse libraryResponse(SysCheckItemLibrary item) {
    return new SystemCheckItemLibraryResponse(
        item.id,
        item.riskType,
        item.checkItem,
        item.applicableStage,
        stageLabel(item.applicableStage),
        item.defaultCheckResult,
        item.defaultRectificationDescription,
        item.defaultFollowUpPlan,
        intBool(item.requireImage),
        intBool(item.requireVideo),
        item.sortOrder,
        item.status,
        statusLabel(item.status),
        item.createdAt,
        item.updatedAt);
  }

  private Map<Long, SysOrg> orgMap() {
    return orgMapper.selectList(new QueryWrapper<SysOrg>().eq("deleted", 0)).stream()
        .collect(Collectors.toMap(org -> org.id, Function.identity(), (left, right) -> left));
  }

  private <T> PageResult<T> page(List<T> rows, Integer page, Integer pageSize) {
    int current = Math.max(1, page == null ? 1 : page);
    int size = Math.min(100, Math.max(1, pageSize == null ? 20 : pageSize));
    int from = Math.min((current - 1) * size, rows.size());
    int to = Math.min(from + size, rows.size());
    return new PageResult<>(rows.subList(from, to), rows.size());
  }

  private Long accessOrgId(SysTeamCheckTemplate template) {
    return template.teamOrgId != null
        ? template.teamOrgId
        : template.departmentOrgId != null ? template.departmentOrgId : template.companyOrgId;
  }

  private Long accessOrgId(SystemTeamCheckTemplateRequest request) {
    return request.teamOrgId() != null
        ? request.teamOrgId()
        : request.departmentOrgId() != null ? request.departmentOrgId() : request.companyOrgId();
  }

  private String scope(SysTeamCheckTemplate template) {
    if (template.teamOrgId != null) {
      return "TEAM";
    }
    if (template.departmentOrgId != null) {
      return "DEPARTMENT";
    }
    return "COMPANY";
  }

  private String scopeName(String scope) {
    return switch (scope) {
      case "TEAM" -> "班组";
      case "DEPARTMENT" -> "部门";
      default -> "公司";
    };
  }

  private String stageFromModuleKey(String moduleKey) {
    return switch (moduleKey == null ? "" : moduleKey) {
      case "pre-shift-meeting" -> "PRE_SHIFT_MEETING_CONFIRMATION";
      case "pre-shift-inspection" -> "PRE_SHIFT_INSPECTION";
      case "mid-shift-inspection" -> "MID_SHIFT_INSPECTION";
      case "post-shift-inspection" -> "POST_SHIFT_INSPECTION";
      default -> null;
    };
  }

  private String stageLabel(String stage) {
    return switch (stage) {
      case "PRE_SHIFT_MEETING_CONFIRMATION" -> "班前会安全确认";
      case "PRE_SHIFT_INSPECTION" -> "班前检查";
      case "MID_SHIFT_INSPECTION" -> "班中检查";
      case "POST_SHIFT_INSPECTION" -> "班后检查";
      case "KEY_SITES" -> "重点场所检查";
      default -> stage;
    };
  }

  private String statusLabel(String status) {
    return switch (status) {
      case "ACTIVE" -> "启用";
      case "INACTIVE" -> "停用";
      default -> "草稿";
    };
  }

  private void validateStage(String stage) {
    if (blank(stage) || !STAGES.contains(stage.trim())) {
      throw new BusinessException("检查阶段不合法");
    }
  }

  private void validateStatus(String status) {
    if (blank(status) || !STATUSES.contains(status.trim())) {
      throw new BusinessException("状态不合法");
    }
  }

  private void validateRiskTypeForStage(String riskType, String stage) {
    if (!"POST_SHIFT_INSPECTION".equals(stage) && blank(riskType)) {
      throw new BusinessException("风险类型不能为空");
    }
  }

  private void nullEq(QueryWrapper<SysTeamCheckTemplate> wrapper, String column, Long value) {
    if (value == null) {
      wrapper.isNull(column);
    } else {
      wrapper.eq(column, value);
    }
  }

  private String statusOrActive(String status) {
    return blank(status) ? "ACTIVE" : status.trim();
  }

  private String defaultText(String value, String fallback) {
    String text = text(value);
    return text.isEmpty() ? fallback : text;
  }

  private String text(String value) {
    return value == null ? "" : value.trim();
  }

  private Integer bool(Boolean value) {
    return Boolean.TRUE.equals(value) ? 1 : 0;
  }

  private Boolean intBool(Integer value) {
    return value != null && value > 0;
  }

  private boolean blank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
