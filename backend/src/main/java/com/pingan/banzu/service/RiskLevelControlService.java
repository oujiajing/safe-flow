package com.pingan.banzu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.domain.RiskControlHazard;
import com.pingan.banzu.domain.RiskControlLibrary;
import com.pingan.banzu.domain.SysOrg;
import com.pingan.banzu.dto.RiskControlHazardRequest;
import com.pingan.banzu.dto.RiskControlHazardResponse;
import com.pingan.banzu.dto.RiskControlImportResult;
import com.pingan.banzu.dto.RiskControlLibraryCreateRequest;
import com.pingan.banzu.dto.RiskControlLibraryResponse;
import com.pingan.banzu.mapper.RiskControlHazardMapper;
import com.pingan.banzu.mapper.RiskControlLibraryMapper;
import com.pingan.banzu.mapper.SysOrgMapper;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import com.pingan.banzu.system.audit.AuditLogService;
import com.pingan.banzu.system.common.SystemModule;
import com.pingan.banzu.system.security.SystemDataScopeService;
import com.pingan.banzu.system.security.SystemPermissionService;
import com.pingan.banzu.system.service.SystemExcelService.ExcelFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RiskLevelControlService {

  private static final List<String> HEADERS =
      List.of(
          "公司",
          "风险点",
          "危险源",
          "风险影响因素",
          "事故类型",
          "事故发生的可能性（L）",
          "人员暴露于危险环境中的频繁程度（E）",
          "发生事故可能造成的后果（C）",
          "风险值（D）",
          "风险等级",
          "关键技术与工程措施",
          "关键人员素养与系统管理措施",
          "关键个体防护与应急管理措施",
          "上级单位责任人",
          "责任部门",
          "责任人/联系方式",
          "可能产生的事故隐患",
          "隐患整治措施");

  private final AuditLogService auditLogService;
  private final RiskControlHazardMapper hazardMapper;
  private final RiskControlLibraryMapper libraryMapper;
  private final NotificationOutboxService notificationOutboxService;
  private final SysOrgMapper orgMapper;
  private final SystemDataScopeService dataScopeService;
  private final SystemPermissionService permissionService;

  public RiskLevelControlService(
      AuditLogService auditLogService,
      RiskControlHazardMapper hazardMapper,
      RiskControlLibraryMapper libraryMapper,
      NotificationOutboxService notificationOutboxService,
      SysOrgMapper orgMapper,
      SystemDataScopeService dataScopeService,
      SystemPermissionService permissionService) {
    this.auditLogService = auditLogService;
    this.hazardMapper = hazardMapper;
    this.libraryMapper = libraryMapper;
    this.notificationOutboxService = notificationOutboxService;
    this.orgMapper = orgMapper;
    this.dataScopeService = dataScopeService;
    this.permissionService = permissionService;
  }

  public PageResult<RiskControlLibraryResponse> listLibraries(
      String keyword, Integer page, Integer pageSize) {
    assertCanViewRisk();
    QueryWrapper<RiskControlLibrary> wrapper =
        new QueryWrapper<RiskControlLibrary>().eq("deleted", 0);
    dataScopeService.applyOrgScope(wrapper, "company_id");
    if (!blank(keyword)) {
      wrapper.like("name", keyword.trim());
    }
    wrapper.orderByDesc("updated_at").orderByDesc("id");

    List<RiskControlLibrary> all = libraryMapper.selectList(wrapper);
    int from = Math.max(0, ((page == null ? 1 : page) - 1) * (pageSize == null ? 20 : pageSize));
    int to = Math.min(all.size(), from + (pageSize == null ? 20 : pageSize));
    if (from > to) {
      from = to;
    }
    return new PageResult<>(
        all.subList(from, to).stream().map(this::libraryResponse).toList(), all.size());
  }

  @Transactional
  public RiskControlLibraryResponse createLibrary(RiskControlLibraryCreateRequest request) {
    assertCanManageRisk();
    if (request == null || request.hazard() == null) {
      throw new BusinessException("请填写首条隐患");
    }
    Long companyId = resolveCompanyId(request.hazard());
    dataScopeService.assertCanAccessOrg(companyId);

    CurrentUser user = CurrentUserContext.require();
    RiskControlLibrary library = new RiskControlLibrary();
    library.name = blank(request.name()) ? defaultLibraryName() : request.name().trim();
    library.companyId = companyId;
    library.createdBy = user.userId();
    library.updatedBy = user.userId();
    libraryMapper.insert(library);

    RiskControlHazard hazard =
        insertHazard(library.id, companyId, request.hazard(), user.userId());
    publishRiskEvent("RISK_CREATED", hazard, user.userId(), "created");
    publishLibraryReview(library, user.userId());
    auditLogService.record(
        SystemModule.RISK_LEVEL_CONTROL, "RISK_CONTROL_LIBRARY", library.id, "CREATE", "新建风险分级管控库");
    return libraryResponse(requireLibrary(library.id));
  }

  public PageResult<RiskControlHazardResponse> listHazards(Long libraryId, Integer page, Integer pageSize) {
    assertCanViewRisk();
    RiskControlLibrary library = requireLibrary(libraryId);
    QueryWrapper<RiskControlHazard> wrapper =
        new QueryWrapper<RiskControlHazard>()
            .eq("library_id", library.id)
            .eq("deleted", 0)
            .orderByAsc("id");
    List<RiskControlHazard> all = hazardMapper.selectList(wrapper);
    int size = pageSize == null ? 50 : pageSize;
    int from = Math.max(0, ((page == null ? 1 : page) - 1) * size);
    int to = Math.min(all.size(), from + size);
    if (from > to) {
      from = to;
    }
    return new PageResult<>(
        all.subList(from, to).stream().map(this::hazardResponse).toList(), all.size());
  }

  @Transactional
  public RiskControlHazardResponse createHazard(Long libraryId, RiskControlHazardRequest request) {
    assertCanManageRisk();
    RiskControlLibrary library = requireLibrary(libraryId);
    RiskControlHazard hazard = insertHazard(library.id, library.companyId, request, CurrentUserContext.require().userId());
    publishRiskEvent(
        "RISK_CREATED", hazard, CurrentUserContext.require().userId(), "created");
    touchLibrary(library.id);
    return hazardResponse(hazardMapper.selectById(hazard.id));
  }

  @Transactional
  public RiskControlHazardResponse updateHazard(
      Long libraryId, Long hazardId, RiskControlHazardRequest request) {
    assertCanManageRisk();
    RiskControlLibrary library = requireLibrary(libraryId);
    RiskControlHazard hazard = hazardMapper.selectById(hazardId);
    if (hazard == null || !library.id.equals(hazard.libraryId) || Integer.valueOf(1).equals(hazard.deleted)) {
      throw new BusinessException("隐患明细不存在");
    }
    String previousRiskLevel = hazard.riskLevel;
    copyHazardFields(hazard, request);
    hazard.companyId = library.companyId;
    hazard.updatedBy = CurrentUserContext.require().userId();
    hazard.updatedAt = LocalDateTime.now();
    hazardMapper.updateById(hazard);
    if (!java.util.Objects.equals(previousRiskLevel, hazard.riskLevel)) {
      publishRiskEvent(
          "RISK_LEVEL_CHANGED",
          hazard,
          CurrentUserContext.require().userId(),
          text(previousRiskLevel) + "-to-" + text(hazard.riskLevel));
    }
    touchLibrary(library.id);
    return hazardResponse(hazardMapper.selectById(hazard.id));
  }

  public ExcelFile template() {
    assertCanViewRisk();
    return new ExcelFile("风险分级管控_导入模板.xlsx", workbook(List.of()));
  }

  public ExcelFile exportLibrary(Long libraryId) {
    assertCanViewRisk();
    RiskControlLibrary library = requireLibrary(libraryId);
    List<RiskControlHazard> hazards =
        hazardMapper.selectList(
            new QueryWrapper<RiskControlHazard>()
                .eq("library_id", library.id)
                .eq("deleted", 0)
                .orderByAsc("id"));
    return new ExcelFile(library.name + "_数据导出.xlsx", workbook(hazards.stream().map(this::excelRow).toList()));
  }

  @Transactional
  public RiskControlImportResult importWorkbook(MultipartFile file) {
    assertCanManageRisk();
    List<RiskControlHazardRequest> rows = readRows(file);
    if (rows.isEmpty()) {
      throw new BusinessException("导入文件没有数据");
    }

    Long companyId = resolveCompanyId(rows.get(0));
    dataScopeService.assertCanAccessOrg(companyId);
    CurrentUser user = CurrentUserContext.require();

    RiskControlLibrary library = new RiskControlLibrary();
    library.name = importLibraryName(file.getOriginalFilename());
    library.companyId = companyId;
    library.createdBy = user.userId();
    library.updatedBy = user.userId();
    libraryMapper.insert(library);

    for (RiskControlHazardRequest row : rows) {
      insertHazard(library.id, companyId, row, user.userId());
    }
    auditLogService.record(
        SystemModule.RISK_LEVEL_CONTROL,
        "RISK_CONTROL_LIBRARY",
        library.id,
        "IMPORT",
        "导入风险分级管控" + rows.size() + "行");
    return new RiskControlImportResult(libraryResponse(requireLibrary(library.id)), rows.size(), List.of());
  }

  private RiskControlLibrary requireLibrary(Long id) {
    RiskControlLibrary library = libraryMapper.selectById(id);
    if (library == null || Integer.valueOf(1).equals(library.deleted)) {
      throw new BusinessException("风险库不存在");
    }
    dataScopeService.assertCanAccessOrg(library.companyId);
    return library;
  }

  private RiskControlHazard insertHazard(
      Long libraryId, Long companyId, RiskControlHazardRequest request, Long userId) {
    if (request == null) {
      throw new BusinessException("请填写隐患明细");
    }
    if (blank(request.riskPoint())) {
      throw new BusinessException("风险点不能为空");
    }
    if (blank(request.dangerSource())) {
      throw new BusinessException("危险源不能为空");
    }

    RiskControlHazard hazard = new RiskControlHazard();
    hazard.libraryId = libraryId;
    hazard.companyId = companyId;
    copyHazardFields(hazard, request);
    hazard.createdBy = userId;
    hazard.updatedBy = userId;
    hazardMapper.insert(hazard);
    return hazard;
  }

  private void publishRiskEvent(
      String eventType, RiskControlHazard hazard, Long operatorUserId, String suffix) {
    notificationOutboxService.enqueue(
        new DomainNotificationEvent(
            "risk-control:" + eventType + ":" + hazard.id + ":" + suffix,
            eventType,
            "RISK_CONTROL_HAZARD",
            hazard.id,
            hazard.companyId,
            List.of(),
            List.of(operatorUserId),
            hazard.createdBy,
            operatorUserId,
            null,
            Map.of(
                "summary",
                text(hazard.riskPoint) + " · " + text(hazard.riskLevel))));
  }

  private void publishLibraryReview(RiskControlLibrary library, Long operatorUserId) {
    notificationOutboxService.enqueue(
        new DomainNotificationEvent(
            "risk-control:library-review:" + library.id,
            "RISK_LIBRARY_REVIEW_REQUESTED",
            "RISK_CONTROL_LIBRARY",
            library.id,
            library.companyId,
            List.of(),
            List.of(operatorUserId),
            library.createdBy,
            operatorUserId,
            null,
            Map.of("summary", library.name)));
  }

  private void copyHazardFields(RiskControlHazard hazard, RiskControlHazardRequest request) {
    hazard.riskPoint = text(request.riskPoint());
    hazard.dangerSource = text(request.dangerSource());
    hazard.riskInfluenceFactors = text(request.riskInfluenceFactors());
    hazard.accidentType = text(request.accidentType());
    hazard.likelihood = text(request.likelihood());
    hazard.exposureFrequency = text(request.exposureFrequency());
    hazard.consequence = text(request.consequence());
    hazard.riskValue = text(request.riskValue());
    hazard.riskLevel = text(request.riskLevel());
    hazard.engineeringMeasures = text(request.engineeringMeasures());
    hazard.managementMeasures = text(request.managementMeasures());
    hazard.emergencyMeasures = text(request.emergencyMeasures());
    hazard.superiorResponsiblePerson = text(request.superiorResponsiblePerson());
    hazard.responsibleDepartment = text(request.responsibleDepartment());
    hazard.responsibleContact = text(request.responsibleContact());
    hazard.possibleHazard = text(request.possibleHazard());
    hazard.rectificationMeasures = text(request.rectificationMeasures());
  }

  private Long resolveCompanyId(RiskControlHazardRequest request) {
    if (request.companyId() != null) {
      return request.companyId();
    }
    if (blank(request.company())) {
      throw new BusinessException("公司不能为空");
    }
    String company = request.company().trim();
    SysOrg exact =
        orgMapper.selectOne(
            new QueryWrapper<SysOrg>()
                .eq("deleted", 0)
                .eq("org_type", "COMPANY")
                .eq("org_name", company)
                .last("limit 1"));
    if (exact != null) {
      return exact.id;
    }
    SysOrg fuzzy =
        orgMapper.selectOne(
            new QueryWrapper<SysOrg>()
                .eq("deleted", 0)
                .eq("org_type", "COMPANY")
                .like("org_name", company)
                .last("limit 1"));
    if (fuzzy == null) {
      throw new BusinessException("公司不存在：" + company);
    }
    return fuzzy.id;
  }

  private RiskControlLibraryResponse libraryResponse(RiskControlLibrary library) {
    return new RiskControlLibraryResponse(
        library.id,
        library.name,
        library.companyId,
        companyName(library.companyId),
        hazardMapper.selectCount(
            new QueryWrapper<RiskControlHazard>().eq("library_id", library.id).eq("deleted", 0)),
        library.createdAt,
        library.updatedAt);
  }

  private RiskControlHazardResponse hazardResponse(RiskControlHazard hazard) {
    return new RiskControlHazardResponse(
        hazard.id,
        hazard.libraryId,
        hazard.companyId,
        companyName(hazard.companyId),
        hazard.riskPoint,
        hazard.dangerSource,
        hazard.riskInfluenceFactors,
        hazard.accidentType,
        hazard.likelihood,
        hazard.exposureFrequency,
        hazard.consequence,
        hazard.riskValue,
        hazard.riskLevel,
        hazard.engineeringMeasures,
        hazard.managementMeasures,
        hazard.emergencyMeasures,
        hazard.superiorResponsiblePerson,
        hazard.responsibleDepartment,
        hazard.responsibleContact,
        hazard.possibleHazard,
        hazard.rectificationMeasures,
        hazard.createdAt,
        hazard.updatedAt);
  }

  private String companyName(Long companyId) {
    SysOrg org = orgMapper.selectById(companyId);
    return org == null ? "" : org.orgName;
  }

  private void touchLibrary(Long libraryId) {
    RiskControlLibrary library = libraryMapper.selectById(libraryId);
    library.updatedAt = LocalDateTime.now();
    library.updatedBy = CurrentUserContext.require().userId();
    libraryMapper.updateById(library);
  }

  private byte[] workbook(List<List<Object>> rows) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      CellStyle headerStyle = workbook.createCellStyle();
      Font font = workbook.createFont();
      font.setBold(true);
      headerStyle.setFont(font);

      var data = workbook.createSheet("Data");
      Row headerRow = data.createRow(0);
      for (int index = 0; index < HEADERS.size(); index++) {
        Cell cell = headerRow.createCell(index);
        cell.setCellValue(HEADERS.get(index));
        cell.setCellStyle(headerStyle);
        data.setColumnWidth(index, Math.min(60, Math.max(12, HEADERS.get(index).length() * 2)) * 256);
      }
      for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
        Row row = data.createRow(rowIndex + 1);
        List<Object> values = rows.get(rowIndex);
        for (int colIndex = 0; colIndex < values.size(); colIndex++) {
          row.createCell(colIndex).setCellValue(String.valueOf(values.get(colIndex) == null ? "" : values.get(colIndex)));
        }
      }

      var companies = workbook.createSheet("企业");
      companies.createRow(0).createCell(0).setCellValue("编码");
      companies.getRow(0).createCell(1).setCellValue("名称");
      List<SysOrg> companyOrgs =
          orgMapper.selectList(
              new QueryWrapper<SysOrg>().eq("deleted", 0).eq("org_type", "COMPANY").orderByAsc("sort_order", "id"));
      for (int index = 0; index < companyOrgs.size(); index++) {
        SysOrg org = companyOrgs.get(index);
        Row row = companies.createRow(index + 1);
        row.createCell(0).setCellValue(org.orgCode);
        row.createCell(1).setCellValue(org.orgName);
      }

      workbook.write(output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new BusinessException("生成风险分级管控 Excel 失败");
    }
  }

  private List<RiskControlHazardRequest> readRows(MultipartFile file) {
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file.getBytes()))) {
      DataFormatter formatter = new DataFormatter();
      var sheet = workbook.getSheetAt(0);
      List<RiskControlHazardRequest> rows = new ArrayList<>();
      for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
          continue;
        }
        Map<String, String> values = new LinkedHashMap<>();
        boolean empty = true;
        for (int colIndex = 0; colIndex < HEADERS.size(); colIndex++) {
          String value = formatter.formatCellValue(row.getCell(colIndex)).trim();
          values.put(HEADERS.get(colIndex), value);
          empty = empty && value.isBlank();
        }
        if (!empty) {
          rows.add(
              new RiskControlHazardRequest(
                  null,
                  values.get("公司"),
                  values.get("风险点"),
                  values.get("危险源"),
                  values.get("风险影响因素"),
                  values.get("事故类型"),
                  values.get("事故发生的可能性（L）"),
                  values.get("人员暴露于危险环境中的频繁程度（E）"),
                  values.get("发生事故可能造成的后果（C）"),
                  values.get("风险值（D）"),
                  values.get("风险等级"),
                  values.get("关键技术与工程措施"),
                  values.get("关键人员素养与系统管理措施"),
                  values.get("关键个体防护与应急管理措施"),
                  values.get("上级单位责任人"),
                  values.get("责任部门"),
                  values.get("责任人/联系方式"),
                  values.get("可能产生的事故隐患"),
                  values.get("隐患整治措施")));
        }
      }
      return rows;
    } catch (IOException exception) {
      throw new BusinessException("读取风险分级管控 Excel 失败");
    }
  }

  private List<Object> excelRow(RiskControlHazard hazard) {
    return List.of(
        companyName(hazard.companyId),
        value(hazard.riskPoint),
        value(hazard.dangerSource),
        value(hazard.riskInfluenceFactors),
        value(hazard.accidentType),
        value(hazard.likelihood),
        value(hazard.exposureFrequency),
        value(hazard.consequence),
        value(hazard.riskValue),
        value(hazard.riskLevel),
        value(hazard.engineeringMeasures),
        value(hazard.managementMeasures),
        value(hazard.emergencyMeasures),
        value(hazard.superiorResponsiblePerson),
        value(hazard.responsibleDepartment),
        value(hazard.responsibleContact),
        value(hazard.possibleHazard),
        value(hazard.rectificationMeasures));
  }

  private String importLibraryName(String filename) {
    String base = blank(filename) ? "风险分级管控导入" : filename.replaceFirst("\\.[^.]+$", "");
    return base + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
  }

  private String defaultLibraryName() {
    return "风险分级管控库_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
  }

  private String value(String value) {
    return value == null ? "" : value;
  }

  private String text(String value) {
    return blank(value) ? "" : value.trim();
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private void assertCanViewRisk() {
    permissionService.assertHasAnyPermission("PINGAN_RISK_ENTRY", "PINGAN_DATABASE_ENTRY");
    permissionService.assertHasAnyPermission("PINGAN_RISK_VIEW", "PINGAN_DATABASE_VIEW");
  }

  private void assertCanManageRisk() {
    permissionService.assertHasAnyPermission("PINGAN_RISK_MANAGE", "PINGAN_DATABASE_MANAGE");
  }
}
