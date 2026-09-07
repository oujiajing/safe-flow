package com.pingan.banzu.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.domain.SysOrg;
import com.pingan.banzu.domain.SysRole;
import com.pingan.banzu.mapper.SysOrgMapper;
import com.pingan.banzu.mapper.SysRoleMapper;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import com.pingan.banzu.service.DomainNotificationEvent;
import com.pingan.banzu.service.NotificationOutboxService;
import com.pingan.banzu.system.audit.AuditLogService;
import com.pingan.banzu.system.common.SystemModule;
import com.pingan.banzu.system.dto.SystemCompanyQuery;
import com.pingan.banzu.system.dto.SystemCompanyRequest;
import com.pingan.banzu.system.dto.SystemCompanyResponse;
import com.pingan.banzu.system.dto.SystemDepartmentQuery;
import com.pingan.banzu.system.dto.SystemDepartmentRequest;
import com.pingan.banzu.system.dto.SystemDepartmentResponse;
import com.pingan.banzu.system.dto.SystemImportResult;
import com.pingan.banzu.system.dto.SystemPersonnelQuery;
import com.pingan.banzu.system.dto.SystemPersonnelRequest;
import com.pingan.banzu.system.dto.SystemPersonnelResponse;
import com.pingan.banzu.system.dto.SystemTeamQuery;
import com.pingan.banzu.system.dto.SystemTeamRequest;
import com.pingan.banzu.system.dto.SystemTeamResponse;
import com.pingan.banzu.system.mapper.SysUserProfileMapper;
import com.pingan.banzu.system.security.SystemDataScopeService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SystemExcelService {

  private static final List<String> COMPANY_HEADERS =
      List.of(
          "编码", "排序", "名称", "简称", "描述", "状态", "地址", "公司类型", "一级", "二级", "三级", "四级",
          "安全管理员", "一级上报人", "二级上报人", "三级上报人", "L1上报时间", "L2上报时间", "L3上报时间",
          "附属文件1", "附属文件2", "公司介绍");
  private static final List<String> DEPARTMENT_HEADERS =
      List.of(
          "编码", "名称", "所属公司", "类型", "子排序", "负责人", "描述", "状态", "顶级", "集团", "一级单位",
          "二级单位", "领导级别", "公司排序");
  private static final List<String> TEAM_HEADERS =
      List.of(
          "名称", "公司", "集团", "一级单位", "二级单位", "车间", "班组作业", "状态", "班组长", "班组成员",
          "安全员", "积分", "编码", "活跃");
  private static final List<String> PERSONNEL_HEADERS =
      List.of(
          "编码", "姓名", "所属公司", "企业简称", "所属部门", "所属班组", "积分", "领用积分", "员工类型", "岗位",
          "手机号", "状态", "系统角色", "部门排序", "管理权重");
  private static final Set<String> STATUSES = Set.of("DRAFT", "ACTIVE", "INACTIVE");

  private final AuditLogService auditLogService;
  private final JdbcTemplate jdbcTemplate;
  private final NotificationOutboxService notificationOutboxService;
  private final SysOrgMapper orgMapper;
  private final SysRoleMapper roleMapper;
  private final SysUserProfileMapper userProfileMapper;
  private final SystemCompanyService companyService;
  private final SystemDataScopeService dataScopeService;
  private final SystemDepartmentService departmentService;
  private final SystemPersonnelService personnelService;
  private final SystemTeamService teamService;

  public SystemExcelService(
      AuditLogService auditLogService,
      JdbcTemplate jdbcTemplate,
      NotificationOutboxService notificationOutboxService,
      SysOrgMapper orgMapper,
      SysRoleMapper roleMapper,
      SysUserProfileMapper userProfileMapper,
      SystemCompanyService companyService,
      SystemDataScopeService dataScopeService,
      SystemDepartmentService departmentService,
      SystemPersonnelService personnelService,
      SystemTeamService teamService) {
    this.auditLogService = auditLogService;
    this.jdbcTemplate = jdbcTemplate;
    this.notificationOutboxService = notificationOutboxService;
    this.orgMapper = orgMapper;
    this.roleMapper = roleMapper;
    this.userProfileMapper = userProfileMapper;
    this.companyService = companyService;
    this.dataScopeService = dataScopeService;
    this.departmentService = departmentService;
    this.personnelService = personnelService;
    this.teamService = teamService;
  }

  public ExcelFile template(String module) {
    ModuleConfig config = moduleConfig(module);
    return new ExcelFile(config.templateFilename(), workbook(config.headers(), List.of()));
  }

  public ExcelFile exportData(String module, Map<String, String> params) {
    ModuleConfig config = moduleConfig(module);
    List<List<Object>> rows =
        switch (config.module()) {
          case COMPANY -> companyRows(params);
          case DEPARTMENT -> departmentRows(params);
          case TEAM -> teamRows(params);
          case PERSONNEL -> personnelRows(params);
          default -> throw new BusinessException("不支持的导出模块");
        };
    auditLogService.record(config.systemModule(), config.module().name(), null, "EXPORT", "导出" + config.title() + "数据");
    return new ExcelFile(config.exportFilename(), workbook(config.headers(), rows));
  }

  public SystemImportResult importData(String module, MultipartFile file) {
    ModuleConfig config = moduleConfig(module);
    List<Map<String, String>> rows = readRows(file, config.headers());
    List<String> errors = validateRows(config, rows);
    if (!errors.isEmpty()) {
      Long jobId = insertImportJob(config, file.getOriginalFilename(), "FAILED", rows.size(), 0, rows.size(), String.join("; ", errors));
      auditLogService.record(config.systemModule(), "SYS_IMPORT_JOB", jobId, "IMPORT_FAILED", "导入" + config.title() + "失败");
      throw new BusinessException(String.join("; ", errors));
    }

    for (Map<String, String> row : rows) {
      importRow(config.module(), row);
    }
    Long jobId = insertImportJob(config, file.getOriginalFilename(), "SUCCESS", rows.size(), rows.size(), 0, null);
    auditLogService.record(config.systemModule(), "SYS_IMPORT_JOB", jobId, "IMPORT", "导入" + config.title() + rows.size() + "行");
    CurrentUser currentUser = CurrentUserContext.require();
    notificationOutboxService.enqueue(
        new DomainNotificationEvent(
            "system-import:finished:" + jobId,
            "IMPORT_FINISHED",
            "SYS_IMPORT_JOB",
            jobId,
            currentUser.orgId(),
            List.of(currentUser.userId()),
            List.of(),
            currentUser.userId(),
            currentUser.userId(),
            null,
            Map.of(
                "summary",
                config.title() + " · 成功导入 " + rows.size() + " 行")));
    return new SystemImportResult(jobId, config.module().name(), "SUCCESS", rows.size(), rows.size(), 0, List.of());
  }

  private List<List<Object>> companyRows(Map<String, String> params) {
    PageResult<SystemCompanyResponse> page =
        companyService.list(new SystemCompanyQuery(longParam(params, "organizationId"), params.get("keyword"), params.get("status"), 1, 10000));
    return page.items().stream()
        .map(
            row ->
                List.<Object>of(
                    value(row.code()), value(row.sortOrder()), value(row.name()), value(row.shortName()), value(row.description()),
                    value(row.status()), value(row.address()), value(row.companyType()), value(row.level1Name()), value(row.level2Name()),
                    value(row.level3Name()), value(row.level4Name()), value(row.safetyManagerUsername()), value(row.reporterL1Usernames()),
                    value(row.reporterL2Usernames()), value(row.reporterL3Usernames()), value(row.reportL1Time()), value(row.reportL2Time()),
                    value(row.reportL3Time()), value(row.attachment1Url()), value(row.attachment2Url()), value(row.companyIntro())))
        .toList();
  }

  private List<List<Object>> departmentRows(Map<String, String> params) {
    PageResult<SystemDepartmentResponse> page =
        departmentService.list(
            new SystemDepartmentQuery(
                longParam(params, "companyOrgId", "companyId"),
                longParam(params, "organizationId"),
                params.get("keyword"),
                params.get("status"),
                1,
                10000));
    return page.items().stream()
        .map(
            row ->
                List.<Object>of(
                    value(row.code()), value(row.name()), value(row.companyName()), value(row.departmentType()), value(row.childSortOrder()),
                    value(row.leaderUsername()), value(row.description()), value(row.status()), value(row.topLevelName()), value(row.groupName()),
                    value(row.level1Unit()), value(row.level2Unit()), value(row.leaderLevel()), value(row.companySortOrder())))
        .toList();
  }

  private List<List<Object>> teamRows(Map<String, String> params) {
    PageResult<SystemTeamResponse> page =
        teamService.list(
            new SystemTeamQuery(
                longParam(params, "companyOrgId", "companyId"),
                longParam(params, "workshopOrgId", "departmentOrgId", "departmentId"),
                longParam(params, "organizationId"),
                params.get("keyword"),
                params.get("status"),
                1,
                10000));
    return page.items().stream()
        .map(
            row ->
                List.<Object>of(
                    value(row.name()), value(row.companyName()), value(row.groupName()), value(row.level1Unit()), value(row.level2Unit()),
                    value(row.workshopName()), value(row.workTypeName()), value(row.status()), value(row.leaderUsername()),
                    value(String.join(",", row.teamMembers() == null ? List.of() : row.teamMembers())), value(row.safetyOfficerUsername()),
                    value(row.points()), value(row.code()), Boolean.TRUE.equals(row.active()) ? "是" : "否"))
        .toList();
  }

  private List<List<Object>> personnelRows(Map<String, String> params) {
    PageResult<SystemPersonnelResponse> page =
        personnelService.list(
            new SystemPersonnelQuery(
                longParam(params, "companyOrgId", "companyId"),
                longParam(params, "departmentOrgId", "departmentId"),
                longParam(params, "teamOrgId", "teamId"),
                longParam(params, "organizationId"),
                params.get("keyword"),
                params.get("status"),
                1,
                10000));
    return page.items().stream()
        .map(
            row ->
                List.<Object>of(
                    value(row.employeeCode()), value(row.name()), value(row.companyName()), value(row.companyShortName()), value(row.departmentName()),
                    value(row.teamName()), value(row.points()), value(row.receivedPoints()), value(row.employeeType()), value(row.positionName()),
                    value(row.mobile()), value(row.status()), value(row.systemRoleCode()), value(row.departmentSortOrder()), value(row.managementWeight())))
        .toList();
  }

  private void importRow(Module module, Map<String, String> row) {
    switch (module) {
      case COMPANY ->
          companyService.create(
              new SystemCompanyRequest(
                  text(row, "编码"),
                  integer(row, "排序"),
                  text(row, "名称"),
                  text(row, "简称"),
                  text(row, "描述"),
                  normalizeStatus(text(row, "状态")),
                  text(row, "地址"),
                  text(row, "公司类型"),
                  text(row, "一级"),
                  text(row, "二级"),
                  text(row, "三级"),
                  text(row, "四级"),
                  text(row, "安全管理员"),
                  text(row, "一级上报人"),
                  text(row, "二级上报人"),
                  text(row, "三级上报人"),
                  integer(row, "L1上报时间"),
                  integer(row, "L2上报时间"),
                  integer(row, "L3上报时间"),
                  text(row, "附属文件1"),
                  text(row, "附属文件2"),
                  text(row, "公司介绍")));
      case DEPARTMENT ->
          departmentService.create(
              new SystemDepartmentRequest(
                  text(row, "编码"),
                  text(row, "名称"),
                  requireOrgReference(text(row, "所属公司"), "COMPANY", "所属公司"),
                  text(row, "类型"),
                  integer(row, "子排序"),
                  text(row, "负责人"),
                  text(row, "描述"),
                  normalizeStatus(text(row, "状态")),
                  text(row, "顶级"),
                  text(row, "集团"),
                  text(row, "一级单位"),
                  text(row, "二级单位"),
                  text(row, "领导级别"),
                  integer(row, "公司排序")));
      case TEAM ->
          teamService.create(
              new SystemTeamRequest(
                  text(row, "编码"),
                  text(row, "名称"),
                  requireOrgReference(text(row, "公司"), "COMPANY", "公司"),
                  optionalOrgReference(text(row, "车间"), "DEPARTMENT", "车间"),
                  text(row, "集团"),
                  text(row, "一级单位"),
                  text(row, "二级单位"),
                  text(row, "车间"),
                  text(row, "班组作业"),
                  text(row, "班组作业"),
                  normalizeStatus(text(row, "状态")),
                  text(row, "班组长"),
                  splitNames(text(row, "班组成员")),
                  text(row, "安全员"),
                  null,
                  null,
                  integer(row, "积分"),
                  bool(row, "活跃")));
      case PERSONNEL ->
          personnelService.create(
              new SystemPersonnelRequest(
                  text(row, "编码"),
                  text(row, "姓名"),
                  text(row, "编码"),
                  requireOrgReference(text(row, "所属公司"), "COMPANY", "所属公司"),
                  text(row, "企业简称"),
                  optionalOrgReference(text(row, "所属部门"), "DEPARTMENT", "所属部门"),
                  optionalOrgReference(text(row, "所属班组"), "TEAM", "所属班组"),
                  integer(row, "积分"),
                  integer(row, "领用积分"),
                  text(row, "员工类型"),
                  text(row, "岗位"),
                  text(row, "手机号"),
                  normalizeStatus(text(row, "状态")),
                  roleCode(text(row, "系统角色")),
                  null,
                  null,
                  null,
                  null,
                  null,
                  integer(row, "部门排序"),
                  integer(row, "管理权重")));
    }
  }

  private List<String> validateRows(ModuleConfig config, List<Map<String, String>> rows) {
    List<String> errors = new ArrayList<>();
    Set<String> codes = new LinkedHashSet<>();
    for (int index = 0; index < rows.size(); index++) {
      Map<String, String> row = rows.get(index);
      int excelRow = index + 2;
      String code = code(config.module(), row);
      String name = name(config.module(), row);
      requireNotBlank(errors, excelRow, codeField(config.module()), code);
      requireNotBlank(errors, excelRow, nameField(config.module()), name);
      if (!isBlank(code) && !codes.add(code)) {
        errors.add("第" + excelRow + "行[" + codeField(config.module()) + "]文件内重复");
      }
      if (!isBlank(code) && codeExists(config.module(), code)) {
        errors.add("第" + excelRow + "行[" + codeField(config.module()) + "]编码已存在");
      }
      validateStatus(errors, excelRow, row.get("状态"));
      validateType(errors, excelRow, row);
      validateReferences(errors, excelRow, config.module(), row);
    }
    return errors;
  }

  private void validateReferences(List<String> errors, int excelRow, Module module, Map<String, String> row) {
    switch (module) {
      case DEPARTMENT -> validateOrgReference(errors, excelRow, "所属公司", row.get("所属公司"), "COMPANY", true);
      case TEAM -> {
        validateOrgReference(errors, excelRow, "公司", row.get("公司"), "COMPANY", true);
        validateOrgReference(errors, excelRow, "车间", row.get("车间"), "DEPARTMENT", false);
      }
      case PERSONNEL -> {
        validateOrgReference(errors, excelRow, "所属公司", row.get("所属公司"), "COMPANY", true);
        validateOrgReference(errors, excelRow, "所属部门", row.get("所属部门"), "DEPARTMENT", false);
        validateOrgReference(errors, excelRow, "所属班组", row.get("所属班组"), "TEAM", false);
        String role = row.get("系统角色");
        if (!isBlank(role) && roleCode(role) == null) {
          errors.add("第" + excelRow + "行[系统角色]角色不存在");
        }
      }
      default -> {
      }
    }
  }

  private void validateOrgReference(List<String> errors, int excelRow, String field, String reference, String orgType, boolean required) {
    if (isBlank(reference)) {
      if (required) {
        errors.add("第" + excelRow + "行[" + field + "]不能为空");
      }
      return;
    }
    SysOrg org = resolveOrg(reference, orgType);
    if (org == null) {
      errors.add("第" + excelRow + "行[" + field + "]组织不存在");
      return;
    }
    if (!dataScopeService.canAccessOrg(org.id)) {
      errors.add("第" + excelRow + "行[" + field + "]超出数据权限");
    }
  }

  private void validateStatus(List<String> errors, int excelRow, String status) {
    String normalized = normalizeStatus(status);
    if (!isBlank(status) && !STATUSES.contains(normalized)) {
      errors.add("第" + excelRow + "行[状态]状态不合法");
    }
  }

  private void validateType(List<String> errors, int excelRow, Map<String, String> row) {
    for (String field : List.of("公司类型", "类型", "班组作业", "员工类型")) {
      String value = row.get(field);
      if (!isBlank(value) && ("INVALID".equalsIgnoreCase(value) || "BROKEN".equalsIgnoreCase(value) || "不合法".equals(value))) {
        errors.add("第" + excelRow + "行[" + field + "]类型不合法");
      }
    }
  }

  private Long insertImportJob(
      ModuleConfig config, String filename, String status, int totalRows, int successRows, int failureRows, String errorSummary) {
    CurrentUser user = CurrentUserContext.require();
    jdbcTemplate.update(
        """
        insert into sys_import_job
          (module, original_filename, status, total_rows, success_rows, failure_rows, error_summary, created_by, completed_at)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        config.module().name(),
        filename == null ? "" : filename,
        status,
        totalRows,
        successRows,
        failureRows,
        errorSummary,
        user.userId(),
        LocalDateTime.now());
    return jdbcTemplate.queryForObject("select max(id) from sys_import_job where created_by = ?", Long.class, user.userId());
  }

  private byte[] workbook(List<String> headers, List<List<Object>> rows) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("Data");
      CellStyle headerStyle = workbook.createCellStyle();
      Font font = workbook.createFont();
      font.setBold(true);
      headerStyle.setFont(font);

      Row header = sheet.createRow(0);
      for (int index = 0; index < headers.size(); index++) {
        Cell cell = header.createCell(index);
        cell.setCellValue(headers.get(index));
        cell.setCellStyle(headerStyle);
        sheet.setColumnWidth(index, Math.min(60, Math.max(12, headers.get(index).length() + 8)) * 256);
      }
      for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
        Row row = sheet.createRow(rowIndex + 1);
        List<Object> values = rows.get(rowIndex);
        for (int colIndex = 0; colIndex < values.size(); colIndex++) {
          row.createCell(colIndex).setCellValue(String.valueOf(values.get(colIndex)));
        }
      }
      sheet.createFreezePane(0, 1);
      workbook.write(output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("生成Excel失败", exception);
    }
  }

  private List<Map<String, String>> readRows(MultipartFile file, List<String> expectedHeaders) {
    try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
      DataFormatter formatter = new DataFormatter();
      var sheet = workbook.getSheetAt(0);
      Row headerRow = sheet.getRow(0);
      if (headerRow == null) {
        throw new BusinessException("导入文件缺少表头");
      }
      for (int index = 0; index < expectedHeaders.size(); index++) {
        String actual = formatter.formatCellValue(headerRow.getCell(index)).trim();
        if (!expectedHeaders.get(index).equals(actual)) {
          throw new BusinessException("导入文件表头不匹配: 第" + (index + 1) + "列应为" + expectedHeaders.get(index));
        }
      }

      List<Map<String, String>> rows = new ArrayList<>();
      for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        Map<String, String> values = new LinkedHashMap<>();
        boolean hasValue = false;
        for (int colIndex = 0; colIndex < expectedHeaders.size(); colIndex++) {
          String value = row == null ? "" : formatter.formatCellValue(row.getCell(colIndex)).trim();
          values.put(expectedHeaders.get(colIndex), value);
          hasValue = hasValue || !value.isBlank();
        }
        if (hasValue) {
          rows.add(values);
        }
      }
      return rows;
    } catch (IOException exception) {
      throw new BusinessException("导入文件读取失败");
    }
  }

  private ModuleConfig moduleConfig(String value) {
    return switch (value) {
      case "company" -> new ModuleConfig(Module.COMPANY, "公司", COMPANY_HEADERS, "公司管理_导入模板.xlsx", "公司管理_数据导出.xlsx", SystemModule.COMPANY);
      case "department" ->
          new ModuleConfig(Module.DEPARTMENT, "部门", DEPARTMENT_HEADERS, "部门管理_导入模板.xlsx", "部门管理_数据导出.xlsx", SystemModule.DEPARTMENT);
      case "team" -> new ModuleConfig(Module.TEAM, "班组", TEAM_HEADERS, "班组管理_导入模板.xlsx", "班组管理_数据导出.xlsx", SystemModule.TEAM);
      case "personnel" ->
          new ModuleConfig(Module.PERSONNEL, "人员", PERSONNEL_HEADERS, "人员管理_导入模板.xlsx", "人员管理_数据导出.xlsx", SystemModule.PERSONNEL);
      default -> throw new BusinessException("不支持的Excel模块");
    };
  }

  private String normalizeStatus(String status) {
    if (isBlank(status)) {
      return "DRAFT";
    }
    return switch (status.trim()) {
      case "启用", "正常", "有效" -> "ACTIVE";
      case "停用", "禁用", "冻结", "无效" -> "INACTIVE";
      case "草稿" -> "DRAFT";
      default -> status.trim().toUpperCase();
    };
  }

  private boolean codeExists(Module module, String code) {
    if (module == Module.PERSONNEL) {
      return userProfileMapper.selectCount(new QueryWrapper<com.pingan.banzu.system.domain.SysUserProfile>().eq("employee_code", code).eq("deleted", 0)) > 0;
    }
    return orgMapper.selectCount(new QueryWrapper<SysOrg>().eq("org_code", code).eq("deleted", 0)) > 0;
  }

  private String code(Module module, Map<String, String> row) {
    return module == Module.PERSONNEL ? row.get("编码") : row.get("编码");
  }

  private String name(Module module, Map<String, String> row) {
    return switch (module) {
      case PERSONNEL -> row.get("姓名");
      default -> row.get("名称");
    };
  }

  private String codeField(Module module) {
    return "编码";
  }

  private String nameField(Module module) {
    return module == Module.PERSONNEL ? "姓名" : "名称";
  }

  private void requireNotBlank(List<String> errors, int row, String field, String value) {
    if (isBlank(value)) {
      errors.add("第" + row + "行[" + field + "]不能为空");
    }
  }

  private SysOrg resolveOrg(String reference, String orgType) {
    if (isBlank(reference)) {
      return null;
    }
    List<SysOrg> matches =
        orgMapper.selectList(
            new QueryWrapper<SysOrg>()
                .eq("deleted", 0)
                .eq("org_type", orgType)
                .and(wrapper -> wrapper.eq("org_code", reference.trim()).or().eq("org_name", reference.trim()))
                .last("limit 1"));
    return matches.isEmpty() ? null : matches.get(0);
  }

  private Long requireOrgReference(String reference, String orgType, String field) {
    SysOrg org = resolveOrg(reference, orgType);
    if (org == null) {
      throw new BusinessException(field + "组织不存在");
    }
    dataScopeService.assertCanAccessOrg(org.id);
    return org.id;
  }

  private Long optionalOrgReference(String reference, String orgType, String field) {
    return isBlank(reference) ? null : requireOrgReference(reference, orgType, field);
  }

  private String roleCode(String roleReference) {
    if (isBlank(roleReference)) {
      return null;
    }
    SysRole role =
        roleMapper.selectOne(
            new QueryWrapper<SysRole>()
                .eq("deleted", 0)
                .and(wrapper -> wrapper.eq("role_code", roleReference.trim()).or().eq("role_name", roleReference.trim()))
                .last("limit 1"));
    return role == null ? null : role.roleCode;
  }

  private List<String> splitNames(String value) {
    if (isBlank(value)) {
      return List.of();
    }
    return List.of(value.split("[,，;；]")).stream().map(String::trim).filter(item -> !item.isBlank()).toList();
  }

  private Long longParam(Map<String, String> params, String... names) {
    for (String name : names) {
      String value = params.get(name);
      if (!isBlank(value)) {
        return Long.valueOf(value);
      }
    }
    return null;
  }

  private Integer integer(Map<String, String> row, String field) {
    String value = text(row, field);
    if (isBlank(value)) {
      return null;
    }
    return Integer.valueOf(value.replace(",", ""));
  }

  private Boolean bool(Map<String, String> row, String field) {
    String value = text(row, field);
    if (isBlank(value)) {
      return true;
    }
    return !Set.of("否", "false", "FALSE", "0", "停用").contains(value);
  }

  private String text(Map<String, String> row, String field) {
    String value = row.get(field);
    return isBlank(value) ? null : value.trim();
  }

  private Object value(Object value) {
    return value == null ? "" : value;
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  public record ExcelFile(String filename, byte[] content) {}

  private record ModuleConfig(
      Module module, String title, List<String> headers, String templateFilename, String exportFilename, SystemModule systemModule) {}

  private enum Module {
    COMPANY,
    DEPARTMENT,
    TEAM,
    PERSONNEL
  }
}
