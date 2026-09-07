package com.pingan.banzu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.common.SpecialWorkAction;
import com.pingan.banzu.common.SpecialWorkStatus;
import com.pingan.banzu.domain.BizAttachment;
import com.pingan.banzu.domain.SpecialWorkRecord;
import com.pingan.banzu.domain.SysOrg;
import com.pingan.banzu.dto.AttachmentResponse;
import com.pingan.banzu.dto.SpecialWorkBatchDeleteRequest;
import com.pingan.banzu.dto.SpecialWorkActionRequest;
import com.pingan.banzu.dto.SpecialWorkRecordQuery;
import com.pingan.banzu.dto.SpecialWorkRecordRequest;
import com.pingan.banzu.dto.SpecialWorkRecordResponse;
import com.pingan.banzu.mapper.BizAttachmentMapper;
import com.pingan.banzu.mapper.SpecialWorkRecordMapper;
import com.pingan.banzu.mapper.SysOrgMapper;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import com.pingan.banzu.system.audit.AuditLogService;
import com.pingan.banzu.system.common.SystemModule;
import com.pingan.banzu.system.security.SystemDataScopeService;
import com.pingan.banzu.system.security.SystemPermissionService;
import com.pingan.banzu.system.service.SystemExcelService.ExcelFile;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SpecialWorkRecordService {

  private static final String BIZ_TYPE = "SPECIAL_WORK";
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final List<String> EXCEL_HEADERS =
      List.of(
          "公司",
          "项目",
          "作业类型",
          "作业申请时间",
          "图片",
          "作业内容",
          "作业地点",
          "风险辨识结果",
          "作业实施时间(开始)",
          "作业实施时间(结束)",
          "安全交底人",
          "监护人",
          "接受交底人",
          "完工验收人",
          "完工验收时间",
          "状态");

  private final AttachmentUrlResolver attachmentUrlResolver;
  private final AuditLogService auditLogService;
  private final BizAttachmentMapper attachmentMapper;
  private final FileStorageService fileStorageService;
  private final SpecialWorkRecordMapper recordMapper;
  private final SysOrgMapper orgMapper;
  private final SystemDataScopeService dataScopeService;
  private final SystemPermissionService permissionService;
  private final NotificationEventPublisher notificationEventPublisher;
  private final NotificationOutboxService notificationOutboxService;
  private final NotificationService notificationService;

  public SpecialWorkRecordService(
      AttachmentUrlResolver attachmentUrlResolver,
      AuditLogService auditLogService,
      BizAttachmentMapper attachmentMapper,
      FileStorageService fileStorageService,
      SpecialWorkRecordMapper recordMapper,
      SysOrgMapper orgMapper,
      SystemDataScopeService dataScopeService,
      SystemPermissionService permissionService,
      NotificationEventPublisher notificationEventPublisher,
      NotificationOutboxService notificationOutboxService,
      NotificationService notificationService) {
    this.attachmentUrlResolver = attachmentUrlResolver;
    this.auditLogService = auditLogService;
    this.attachmentMapper = attachmentMapper;
    this.fileStorageService = fileStorageService;
    this.recordMapper = recordMapper;
    this.orgMapper = orgMapper;
    this.dataScopeService = dataScopeService;
    this.permissionService = permissionService;
    this.notificationEventPublisher = notificationEventPublisher;
    this.notificationOutboxService = notificationOutboxService;
    this.notificationService = notificationService;
  }

  public PageResult<SpecialWorkRecordResponse> list(SpecialWorkRecordQuery query) {
    assertCanViewSpecialWork();
    List<SpecialWorkRecord> all = recordMapper.selectList(queryWrapper(query));
    int size = query.pageSize() == null ? 20 : query.pageSize();
    int from = Math.max(0, ((query.page() == null ? 1 : query.page()) - 1) * size);
    int to = Math.min(all.size(), from + size);
    if (from > to) {
      from = to;
    }
    return new PageResult<>(all.subList(from, to).stream().map(this::response).toList(), all.size());
  }

  public SpecialWorkRecordResponse get(Long id) {
    assertCanViewSpecialWork();
    return response(requireRecord(id));
  }

  @Transactional
  public SpecialWorkRecordResponse create(SpecialWorkRecordRequest request) {
    permissionService.assertHasPermission("PINGAN_SPECIAL_WORK_APPLY");
    CurrentUser user = CurrentUserContext.require();
    SpecialWorkRecord record = new SpecialWorkRecord();
    assertInitialStatus(request);
    applyEditableFields(record, request);
    clearWorkflowEvidence(record);
    record.status = SpecialWorkStatus.PENDING_APPROVAL.name();
    record.createdBy = user.userId();
    record.updatedBy = user.userId();
    recordMapper.insert(record);
    enqueueWorkflowNotification(record, "SPECIAL_WORK_APPROVAL_REQUESTED", user.userId());
    auditLogService.record(SystemModule.SPECIAL_WORK, "SPECIAL_WORK", record.id, "CREATE", "新增特殊作业");
    return response(requireRecord(record.id));
  }

  @Transactional
  public SpecialWorkRecordResponse update(Long id, SpecialWorkRecordRequest request) {
    assertCanReviewSpecialWork();
    SpecialWorkRecord record = requireRecord(id);
    assertNotCompleted(record);
    assertStatusUnchanged(record, request);
    applyEditableFields(record, request);
    applyCurrentStageEvidence(record, request);
    Long operatorId = CurrentUserContext.require().userId();
    record.updatedBy = operatorId;
    record.updatedAt = LocalDateTime.now();
    recordMapper.updateById(record);
    auditLogService.record(SystemModule.SPECIAL_WORK, "SPECIAL_WORK", record.id, "UPDATE", "更新特殊作业");
    return response(requireRecord(record.id));
  }

  private void enqueueWorkflowNotification(
      SpecialWorkRecord record, String eventType, Long operatorId) {
    notificationOutboxService.enqueue(
        new DomainNotificationEvent(
            "special-work:" + eventType + ":" + record.id + ":" + record.status,
            eventType,
            BIZ_TYPE,
            record.id,
            record.companyId,
            List.of(),
            List.of(operatorId),
            record.createdBy,
            operatorId,
            record.implementationEndTime,
            Map.of(
                "summary",
                value(record.project) + " · " + value(record.workType) + " · " + value(record.workLocation))));
  }

  @Transactional
  public SpecialWorkRecordResponse executeAction(
      Long id, String actionValue, SpecialWorkActionRequest request) {
    SpecialWorkRecord record = requireRecord(id);
    SpecialWorkAction action = SpecialWorkAction.parse(actionValue);
    assertCanExecuteAction(record, action);
    SpecialWorkStatus currentStatus = SpecialWorkStatus.parse(record.status);
    if (currentStatus != action.fromStatus()) {
      throw new BusinessException(
          "当前状态为“" + currentStatus.label() + "”，不能执行“" + action.label() + "”");
    }

    applyActionEvidence(record, action, request);
    validateTimeline(record);
    String previousStatus = record.status;
    Long operatorId = CurrentUserContext.require().userId();
    record.status = action.toStatus().name();
    record.updatedBy = operatorId;
    record.updatedAt = LocalDateTime.now();
    recordMapper.updateById(record);
    notificationService.cancelBusinessActions(BIZ_TYPE, record.id, "特殊作业流程已进入下一阶段");
    if (action == SpecialWorkAction.SUBMIT_ACCEPTANCE) {
      enqueueWorkflowNotification(record, "SPECIAL_WORK_ACCEPTANCE_REQUESTED", operatorId);
    } else if (action == SpecialWorkAction.COMPLETE_ACCEPTANCE) {
      enqueueWorkflowNotification(record, "SPECIAL_WORK_COMPLETED", operatorId);
    }
    auditLogService.record(
        SystemModule.SPECIAL_WORK,
        "SPECIAL_WORK",
        record.id,
        action.name(),
        action.label());
    notificationEventPublisher.specialWorkChanged(record, previousStatus, operatorId);
    return response(requireRecord(record.id));
  }

  @Transactional
  public void delete(Long id) {
    assertCanReviewSpecialWork();
    SpecialWorkRecord record = requireRecord(id);
    softDelete(List.of(record.id));
    auditLogService.record(SystemModule.SPECIAL_WORK, "SPECIAL_WORK", record.id, "DELETE", "删除特殊作业");
  }

  @Transactional
  public void batchDelete(SpecialWorkBatchDeleteRequest request) {
    assertCanReviewSpecialWork();
    List<Long> ids = request.ids().stream().distinct().toList();
    ids.forEach(this::requireRecord);
    softDelete(ids);
    auditLogService.record(SystemModule.SPECIAL_WORK, "SPECIAL_WORK", null, "BATCH_DELETE", "批量删除特殊作业");
  }

  @Transactional
  public SpecialWorkRecordResponse uploadImage(Long id, MultipartFile file) {
    SpecialWorkRecord record = requireRecord(id);
    assertCanUploadSpecialWorkImage(record);
    if (file == null || file.isEmpty()) {
      throw new BusinessException("上传文件不能为空");
    }
    String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    if (!contentType.startsWith("image/")) {
      throw new BusinessException("特殊作业图片仅支持图片文件");
    }
    AttachmentResponse attachment =
        fileStorageService.saveBusinessAttachment(BIZ_TYPE, record.id, "special-work", "IMAGE", file);
    record.imageAttachmentId = Long.valueOf(attachment.id());
    record.updatedBy = CurrentUserContext.require().userId();
    record.updatedAt = LocalDateTime.now();
    recordMapper.updateById(record);
    auditLogService.record(SystemModule.SPECIAL_WORK, "SPECIAL_WORK", record.id, "UPLOAD_IMAGE", "上传特殊作业图片");
    return response(requireRecord(record.id));
  }

  public ExcelFile exportData(SpecialWorkRecordQuery query) {
    assertCanViewSpecialWork();
    List<SpecialWorkRecord> records = recordMapper.selectList(queryWrapper(query));
    return new ExcelFile("特殊作业_数据下载.xlsx", workbook(records));
  }

  private QueryWrapper<SpecialWorkRecord> queryWrapper(SpecialWorkRecordQuery query) {
    SpecialWorkRecordQuery safeQuery = query == null ? new SpecialWorkRecordQuery(null, null, null, null, null, null) : query;
    QueryWrapper<SpecialWorkRecord> wrapper =
        new QueryWrapper<SpecialWorkRecord>().eq("deleted", 0);
    dataScopeService.applyOrgScope(wrapper, "company_id");
    if (safeQuery.companyId() != null) {
      dataScopeService.assertCanAccessOrg(safeQuery.companyId());
      wrapper.eq("company_id", safeQuery.companyId());
    }
    if (!blank(safeQuery.status()) && !"all".equalsIgnoreCase(safeQuery.status())) {
      wrapper.eq("status", SpecialWorkStatus.parse(safeQuery.status()).name());
    }
    if (safeQuery.dateStart() != null) {
      wrapper.ge("application_time", safeQuery.dateStart().atStartOfDay());
    }
    if (safeQuery.dateEnd() != null) {
      wrapper.lt("application_time", safeQuery.dateEnd().plusDays(1).atStartOfDay());
    }
    wrapper.orderByDesc("application_time").orderByDesc("id");
    return wrapper;
  }

  private void applyEditableFields(SpecialWorkRecord record, SpecialWorkRecordRequest request) {
    if (request == null) {
      throw new BusinessException("特殊作业不能为空");
    }
    dataScopeService.assertCanAccessOrg(request.companyId());
    if (blank(request.project())) {
      throw new BusinessException("项目不能为空");
    }
    if (blank(request.workType())) {
      throw new BusinessException("作业类型不能为空");
    }
    record.companyId = request.companyId();
    record.project = text(request.project());
    record.workType = text(request.workType());
    record.applicationTime = request.applicationTime();
    record.workContent = text(request.workContent());
    record.workLocation = text(request.workLocation());
    record.riskIdentificationResult = text(request.riskIdentificationResult());
  }

  private void assertInitialStatus(SpecialWorkRecordRequest request) {
    if (request != null
        && !blank(request.status())
        && SpecialWorkStatus.parse(request.status()) != SpecialWorkStatus.PENDING_APPROVAL) {
      throw new BusinessException("新建特殊作业只能进入待审批状态");
    }
  }

  private void clearWorkflowEvidence(SpecialWorkRecord record) {
    record.implementationStartTime = null;
    record.implementationEndTime = null;
    record.safetyDisclosurePerson = "";
    record.guardian = "";
    record.disclosureReceiver = "";
    record.completionAcceptor = "";
    record.completionAcceptanceTime = null;
  }

  private void applyCurrentStageEvidence(
      SpecialWorkRecord record, SpecialWorkRecordRequest request) {
    switch (SpecialWorkStatus.parse(record.status)) {
      case PENDING_APPROVAL -> {
        record.implementationStartTime = request.implementationStartTime();
        record.safetyDisclosurePerson = text(request.safetyDisclosurePerson());
        record.guardian = text(request.guardian());
        record.disclosureReceiver = text(request.disclosureReceiver());
      }
      case IN_PROGRESS -> record.implementationEndTime = request.implementationEndTime();
      case PENDING_ACCEPTANCE -> {
        record.completionAcceptor = text(request.completionAcceptor());
        record.completionAcceptanceTime = request.completionAcceptanceTime();
      }
      case COMPLETED -> throw new BusinessException("已完成的特殊作业不能编辑");
    }
  }

  private void assertStatusUnchanged(
      SpecialWorkRecord record, SpecialWorkRecordRequest request) {
    if (request != null
        && !blank(request.status())
        && SpecialWorkStatus.parse(request.status()) != SpecialWorkStatus.parse(record.status)) {
      throw new BusinessException("特殊作业状态只能通过流程动作变更");
    }
  }

  private void assertNotCompleted(SpecialWorkRecord record) {
    if (SpecialWorkStatus.parse(record.status) == SpecialWorkStatus.COMPLETED) {
      throw new BusinessException("已完成的特殊作业不能编辑");
    }
  }

  private void applyActionEvidence(
      SpecialWorkRecord record, SpecialWorkAction action, SpecialWorkActionRequest request) {
    SpecialWorkActionRequest safeRequest =
        request == null
            ? new SpecialWorkActionRequest(null, null, null, null, null, null, null)
            : request;
    switch (action) {
      case APPROVE_AND_START -> {
        record.implementationStartTime = safeRequest.implementationStartTime();
        record.safetyDisclosurePerson = text(safeRequest.safetyDisclosurePerson());
        record.guardian = text(safeRequest.guardian());
        record.disclosureReceiver = text(safeRequest.disclosureReceiver());
        requireText(record.riskIdentificationResult, "风险辨识结果不能为空");
        requireTime(record.implementationStartTime, "作业实施开始时间不能为空");
        requireText(record.safetyDisclosurePerson, "安全交底人不能为空");
        requireText(record.guardian, "监护人不能为空");
        requireText(record.disclosureReceiver, "接受交底人不能为空");
      }
      case SUBMIT_ACCEPTANCE -> {
        record.implementationEndTime = safeRequest.implementationEndTime();
        requireTime(record.implementationEndTime, "作业实施结束时间不能为空");
      }
      case COMPLETE_ACCEPTANCE -> {
        record.completionAcceptor = text(safeRequest.completionAcceptor());
        record.completionAcceptanceTime = safeRequest.completionAcceptanceTime();
        requireText(record.completionAcceptor, "完工验收人不能为空");
        requireTime(record.completionAcceptanceTime, "完工验收时间不能为空");
      }
    }
  }

  private void validateTimeline(SpecialWorkRecord record) {
    if (record.implementationStartTime != null
        && record.applicationTime != null
        && record.implementationStartTime.isBefore(record.applicationTime)) {
      throw new BusinessException("作业实施开始时间不能早于申请时间");
    }
    if (record.implementationEndTime != null
        && record.implementationStartTime != null
        && record.implementationEndTime.isBefore(record.implementationStartTime)) {
      throw new BusinessException("作业实施结束时间不能早于开始时间");
    }
    if (record.completionAcceptanceTime != null
        && record.implementationEndTime != null
        && record.completionAcceptanceTime.isBefore(record.implementationEndTime)) {
      throw new BusinessException("完工验收时间不能早于作业结束时间");
    }
  }

  private void requireText(String value, String message) {
    if (blank(value)) {
      throw new BusinessException(message);
    }
  }

  private void requireTime(LocalDateTime value, String message) {
    if (value == null) {
      throw new BusinessException(message);
    }
  }

  private void softDelete(List<Long> ids) {
    recordMapper.update(
        null,
        new UpdateWrapper<SpecialWorkRecord>()
            .in("id", ids)
            .set("deleted", 1)
            .set("updated_by", CurrentUserContext.require().userId())
            .set("updated_at", LocalDateTime.now()));
  }

  private SpecialWorkRecord requireRecord(Long id) {
    SpecialWorkRecord record = recordMapper.selectById(id);
    if (record == null || Integer.valueOf(1).equals(record.deleted)) {
      throw new BusinessException("特殊作业不存在");
    }
    dataScopeService.assertCanAccessOrg(record.companyId);
    return record;
  }

  private SpecialWorkRecordResponse response(SpecialWorkRecord record) {
    SpecialWorkStatus status = SpecialWorkStatus.parse(record.status);
    return new SpecialWorkRecordResponse(
        record.id,
        record.companyId,
        companyName(record.companyId),
        record.project,
        record.workType,
        record.applicationTime,
        attachmentResponse(record.imageAttachmentId),
        record.workContent,
        record.workLocation,
        record.riskIdentificationResult,
        record.implementationStartTime,
        record.implementationEndTime,
        record.safetyDisclosurePerson,
        record.guardian,
        record.disclosureReceiver,
        record.completionAcceptor,
        record.completionAcceptanceTime,
        status.name(),
        status.label(),
        record.createdAt,
        record.updatedAt);
  }

  private byte[] workbook(List<SpecialWorkRecord> records) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("Data");
      CellStyle headerStyle = workbook.createCellStyle();
      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerStyle.setFont(headerFont);
      Row header = sheet.createRow(0);
      for (int i = 0; i < EXCEL_HEADERS.size(); i++) {
        header.createCell(i).setCellValue(EXCEL_HEADERS.get(i));
        header.getCell(i).setCellStyle(headerStyle);
      }
      for (int i = 0; i < records.size(); i++) {
        writeExcelRow(sheet.createRow(i + 1), records.get(i));
      }
      for (int i = 0; i < EXCEL_HEADERS.size(); i++) {
        sheet.autoSizeColumn(i);
      }
      workbook.write(output);
      return output.toByteArray();
    } catch (Exception exception) {
      throw new BusinessException("生成特殊作业 Excel 失败");
    }
  }

  private void writeExcelRow(Row row, SpecialWorkRecord record) {
    AttachmentResponse image = attachmentResponse(record.imageAttachmentId);
    String imageText = image == null ? "" : image.originalName() + " " + image.url();
    List<String> values =
        List.of(
            companyName(record.companyId),
            value(record.project),
            value(record.workType),
            format(record.applicationTime),
            imageText,
            value(record.workContent),
            value(record.workLocation),
            value(record.riskIdentificationResult),
            format(record.implementationStartTime),
            format(record.implementationEndTime),
            value(record.safetyDisclosurePerson),
            value(record.guardian),
            value(record.disclosureReceiver),
            value(record.completionAcceptor),
            format(record.completionAcceptanceTime),
            SpecialWorkStatus.parse(record.status).label());
    for (int i = 0; i < values.size(); i++) {
      row.createCell(i).setCellValue(values.get(i));
    }
  }

  private AttachmentResponse attachmentResponse(Long attachmentId) {
    if (attachmentId == null) {
      return null;
    }
    BizAttachment attachment = attachmentMapper.selectById(attachmentId);
    if (attachment == null || Integer.valueOf(1).equals(attachment.deleted)) {
      return null;
    }
    return new AttachmentResponse(
        String.valueOf(attachment.id),
        attachment.fileKind,
        attachment.originalName,
        attachment.storagePath,
        attachmentUrlResolver.url(attachment),
        attachment.contentType,
        attachment.fileSize);
  }

  private String companyName(Long companyId) {
    SysOrg org = orgMapper.selectById(companyId);
    return org == null ? "" : org.orgName;
  }

  private String format(LocalDateTime value) {
    return value == null ? "" : DATE_TIME_FORMATTER.format(value);
  }

  private String value(String value) {
    return value == null ? "" : value;
  }

  private String text(String value) {
    return value == null ? "" : value.trim();
  }

  private boolean blank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private void assertCanViewSpecialWork() {
    permissionService.assertHasPermission("PINGAN_SPECIAL_WORK_ENTRY");
    permissionService.assertHasPermission("PINGAN_SPECIAL_WORK_VIEW");
  }

  private void assertCanReviewSpecialWork() {
    permissionService.assertHasPermission("PINGAN_SPECIAL_WORK_REVIEW");
  }

  private void assertCanExecuteAction(
      SpecialWorkRecord record, SpecialWorkAction action) {
    switch (action) {
      case APPROVE_AND_START ->
          permissionService.assertHasPermission("PINGAN_SPECIAL_WORK_APPROVE");
      case SUBMIT_ACCEPTANCE -> {
        CurrentUser user = CurrentUserContext.require();
        if (!user.userId().equals(record.createdBy)
            || !permissionService.hasPermission("PINGAN_SPECIAL_WORK_APPLY")) {
          throw new SecurityException("仅申请人可提交特殊作业验收");
        }
      }
      case COMPLETE_ACCEPTANCE ->
          permissionService.assertHasPermission("PINGAN_SPECIAL_WORK_REVIEW");
    }
  }

  private void assertCanUploadSpecialWorkImage(SpecialWorkRecord record) {
    CurrentUser user = CurrentUserContext.require();
    boolean isOwnApplication =
        user.userId().equals(record.createdBy)
            && permissionService.hasPermission("PINGAN_SPECIAL_WORK_APPLY");
    if (!isOwnApplication && !permissionService.hasPermission("PINGAN_SPECIAL_WORK_REVIEW")) {
      throw new SecurityException("无权执行该操作");
    }
  }
}
