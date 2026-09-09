package com.pingan.banzu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.common.TrainingSafetyLearningStatus;
import com.pingan.banzu.domain.BizAttachment;
import com.pingan.banzu.domain.SysOrg;
import com.pingan.banzu.domain.ThreeCheckRecord;
import com.pingan.banzu.domain.TrainingSafetyLearningContent;
import com.pingan.banzu.dto.AttachmentResponse;
import com.pingan.banzu.dto.MiniSafetyLearningCheckInRequest;
import com.pingan.banzu.dto.MiniSafetyLearningCheckInResponse;
import com.pingan.banzu.dto.ThreeCheckRecordDetailResponse;
import com.pingan.banzu.dto.TrainingSafetyLearningBatchDeleteRequest;
import com.pingan.banzu.dto.TrainingSafetyLearningContentQuery;
import com.pingan.banzu.dto.TrainingSafetyLearningContentRequest;
import com.pingan.banzu.dto.TrainingSafetyLearningContentResponse;
import com.pingan.banzu.dto.TrainingSafetyLearningImportResult;
import com.pingan.banzu.mapper.BizAttachmentMapper;
import com.pingan.banzu.mapper.SysOrgMapper;
import com.pingan.banzu.mapper.ThreeCheckRecordMapper;
import com.pingan.banzu.mapper.TrainingSafetyLearningContentMapper;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import com.pingan.banzu.system.audit.AuditLogService;
import com.pingan.banzu.system.common.SystemModule;
import com.pingan.banzu.system.security.SystemDataScopeService;
import com.pingan.banzu.system.service.SystemExcelService.ExcelFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
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
public class TrainingSafetyLearningService {

  private static final String BIZ_TYPE = "TRAINING_SAFETY_LEARNING";
  private static final String MINI_SOURCE_CHANNEL = "WECHAT_MINI_PROGRAM";
  private static final int MINI_LEARNING_POINTS = 1;
  private static final List<String> EXCEL_HEADERS =
      List.of("公司", "分类", "标题", "内容", "封面图", "视频", "日期", "计时", "编码", "附件", "HTML提取", "草稿", "状态");
  private static final DateTimeFormatter CODE_MONTH = DateTimeFormatter.ofPattern("yyyyMM");

  private final AttachmentUrlResolver attachmentUrlResolver;
  private final AuditLogService auditLogService;
  private final BizAttachmentMapper attachmentMapper;
  private final FileStorageService fileStorageService;
  private final ObjectMapper objectMapper;
  private final NotificationOutboxService notificationOutboxService;
  private final SysOrgMapper orgMapper;
  private final SystemDataScopeService dataScopeService;
  private final ThreeCheckRecordMapper recordMapper;
  private final ThreeCheckRecordService recordService;
  private final TrainingPermissionPolicy permissionPolicy;
  private final TrainingSafetyLearningContentMapper contentMapper;

  public TrainingSafetyLearningService(
      AttachmentUrlResolver attachmentUrlResolver,
      AuditLogService auditLogService,
      BizAttachmentMapper attachmentMapper,
      FileStorageService fileStorageService,
      ObjectMapper objectMapper,
      NotificationOutboxService notificationOutboxService,
      SysOrgMapper orgMapper,
      SystemDataScopeService dataScopeService,
      ThreeCheckRecordMapper recordMapper,
      ThreeCheckRecordService recordService,
      TrainingPermissionPolicy permissionPolicy,
      TrainingSafetyLearningContentMapper contentMapper) {
    this.attachmentUrlResolver = attachmentUrlResolver;
    this.auditLogService = auditLogService;
    this.attachmentMapper = attachmentMapper;
    this.fileStorageService = fileStorageService;
    this.objectMapper = objectMapper;
    this.notificationOutboxService = notificationOutboxService;
    this.orgMapper = orgMapper;
    this.dataScopeService = dataScopeService;
    this.recordMapper = recordMapper;
    this.recordService = recordService;
    this.permissionPolicy = permissionPolicy;
    this.contentMapper = contentMapper;
  }

  public PageResult<TrainingSafetyLearningContentResponse> list(TrainingSafetyLearningContentQuery query) {
    permissionPolicy.assertCanViewSafetyLearning();
    List<TrainingSafetyLearningContent> all = contentMapper.selectList(queryWrapper(query));
    int size = query == null || query.pageSize() == null ? 20 : query.pageSize();
    int page = query == null || query.page() == null ? 1 : query.page();
    int from = Math.max(0, (page - 1) * size);
    int to = Math.min(all.size(), from + size);
    if (from > to) {
      from = to;
    }
    return new PageResult<>(all.subList(from, to).stream().map(this::response).toList(), all.size());
  }

  public TrainingSafetyLearningContentResponse detail(Long id) {
    permissionPolicy.assertCanViewSafetyLearning();
    return response(requireReadableContent(id));
  }

  @Transactional
  public MiniSafetyLearningCheckInResponse checkInFromMiniProgram(
      Long id, MiniSafetyLearningCheckInRequest request) {
    permissionPolicy.assertCanViewSafetyLearning();
    TrainingSafetyLearningContent content = requireReadableContent(id);
    TrainingSafetyLearningStatus status = TrainingSafetyLearningStatus.parse(content.status);
    if (status != TrainingSafetyLearningStatus.ACTIVE) {
      throw new BusinessException("仅激活的安全学习内容可以打卡");
    }
    CurrentUser user = CurrentUserContext.require();
    String sourceRecordId = "learning-" + content.id + "-user-" + user.userId();
    ThreeCheckRecord existing = findMiniLearningPointsRecord(sourceRecordId, request == null ? null : request.clientRequestId());
    ThreeCheckRecord record = existing == null ? createMiniLearningPointsRecord(content, user, request, sourceRecordId) : existing;
    ThreeCheckRecordDetailResponse pointsRecord = recordService.detailFromMiniProgram("points-flow", record.id);
    notificationOutboxService.enqueue(
        new DomainNotificationEvent(
            "safety-learning:completed:" + content.id + ":" + user.userId(),
            "LEARNING_COMPLETED",
            BIZ_TYPE,
            content.id,
            content.companyId,
            List.of(user.userId()),
            List.of(),
            user.userId(),
            user.userId(),
            null,
            Map.of("summary", content.title)));
    return new MiniSafetyLearningCheckInResponse(content.id, "CHECKED_IN", MINI_LEARNING_POINTS, pointsRecord);
  }

  @Transactional
  public TrainingSafetyLearningContentResponse create(TrainingSafetyLearningContentRequest request) {
    permissionPolicy.assertCanCreateSafetyLearning();
    CurrentUser user = CurrentUserContext.require();
    TrainingSafetyLearningContent content = new TrainingSafetyLearningContent();
    applyRequest(content, request, null);
    content.createdBy = user.userId();
    content.updatedBy = user.userId();
    contentMapper.insert(content);
    if (blank(content.code)) {
      content.code = code(content.learningDate, content.id);
      contentMapper.updateById(content);
    }
    publishAssignmentIfActive(content, user.userId(), "created");
    auditLogService.record(SystemModule.TRAINING, "TRAINING_SAFETY_LEARNING", content.id, "CREATE", "新增安全学习");
    return response(requireContent(content.id));
  }

  @Transactional
  public TrainingSafetyLearningContentResponse update(Long id, TrainingSafetyLearningContentRequest request) {
    assertCanUpdate(request);
    TrainingSafetyLearningContent content = requireContent(id);
    String previousStatus = content.status;
    applyRequest(content, request, id);
    content.updatedBy = CurrentUserContext.require().userId();
    content.updatedAt = LocalDateTime.now();
    contentMapper.updateById(content);
    if (!TrainingSafetyLearningStatus.ACTIVE.name().equals(previousStatus)) {
      publishAssignmentIfActive(content, CurrentUserContext.require().userId(), "activated");
    }
    auditLogService.record(SystemModule.TRAINING, "TRAINING_SAFETY_LEARNING", content.id, "UPDATE", "更新安全学习");
    return response(requireContent(content.id));
  }

  @Transactional
  public void delete(Long id) {
    permissionPolicy.assertCanDeleteSafetyLearning();
    TrainingSafetyLearningContent content = requireContent(id);
    softDelete(List.of(content.id));
    auditLogService.record(SystemModule.TRAINING, "TRAINING_SAFETY_LEARNING", content.id, "DELETE", "删除安全学习");
  }

  @Transactional
  public void batchDelete(TrainingSafetyLearningBatchDeleteRequest request) {
    permissionPolicy.assertCanDeleteSafetyLearning();
    List<Long> ids = safeIds(request);
    ids.forEach(this::requireContent);
    softDelete(ids);
    auditLogService.record(SystemModule.TRAINING, "TRAINING_SAFETY_LEARNING", null, "BATCH_DELETE", "批量删除安全学习");
  }

  @Transactional
  public TrainingSafetyLearningContentResponse uploadAttachment(Long id, MultipartFile file) {
    permissionPolicy.assertCanCreateSafetyLearning();
    TrainingSafetyLearningContent content = requireContent(id);
    if (file == null || file.isEmpty()) {
      throw new BusinessException("上传文件不能为空");
    }
    if (!isPdf(file)) {
      throw new BusinessException("安全学习附件仅支持 PDF 文件");
    }
    AttachmentResponse attachment =
        fileStorageService.saveBusinessAttachment(BIZ_TYPE, content.id, "training/safety-learning", "PDF", file);
    content.attachmentId = Long.valueOf(attachment.id());
    content.attachmentText = attachment.originalName();
    content.updatedBy = CurrentUserContext.require().userId();
    content.updatedAt = LocalDateTime.now();
    contentMapper.updateById(content);
    auditLogService.record(SystemModule.TRAINING, "TRAINING_SAFETY_LEARNING", content.id, "UPLOAD_ATTACHMENT", "上传安全学习附件");
    return response(requireContent(content.id));
  }

  @Transactional
  public TrainingSafetyLearningContentResponse uploadCoverImage(Long id, MultipartFile file) {
    permissionPolicy.assertCanCreateSafetyLearning();
    return uploadMedia(id, file, "IMAGE", "cover_image_id", "cover_image", "上传安全学习封面图");
  }

  @Transactional
  public TrainingSafetyLearningContentResponse uploadVideo(Long id, MultipartFile file) {
    permissionPolicy.assertCanCreateSafetyLearning();
    return uploadMedia(id, file, "VIDEO", "video_id", "video", "上传安全学习视频");
  }

  @Transactional
  public TrainingSafetyLearningContentResponse removeAttachment(Long id) {
    permissionPolicy.assertCanDeleteSafetyLearning();
    TrainingSafetyLearningContent content = requireContent(id);
    if (content.attachmentId != null) {
      attachmentMapper.update(
          null,
          new UpdateWrapper<BizAttachment>()
              .eq("id", content.attachmentId)
              .eq("biz_type", BIZ_TYPE)
              .eq("biz_id", content.id)
              .eq("deleted", 0)
              .set("deleted", 1));
    }
    contentMapper.update(
        null,
        new UpdateWrapper<TrainingSafetyLearningContent>()
            .eq("id", content.id)
            .set("attachment_id", null)
            .set("attachment_text", "")
            .set("updated_by", CurrentUserContext.require().userId())
            .set("updated_at", LocalDateTime.now()));
    auditLogService.record(SystemModule.TRAINING, "TRAINING_SAFETY_LEARNING", content.id, "REMOVE_ATTACHMENT", "删除安全学习附件");
    return response(requireContent(content.id));
  }

  @Transactional
  public TrainingSafetyLearningContentResponse removeCoverImage(Long id) {
    permissionPolicy.assertCanDeleteSafetyLearning();
    return removeMedia(id, "cover_image_id", "cover_image", "REMOVE_COVER_IMAGE", "删除安全学习封面图");
  }

  @Transactional
  public TrainingSafetyLearningContentResponse removeVideo(Long id) {
    permissionPolicy.assertCanDeleteSafetyLearning();
    return removeMedia(id, "video_id", "video", "REMOVE_VIDEO", "删除安全学习视频");
  }

  public ExcelFile template() {
    permissionPolicy.assertCanDownloadSafetyLearning();
    return new ExcelFile("安全学习_导入模板.xlsx", workbook(List.of()));
  }

  public ExcelFile exportData(TrainingSafetyLearningContentQuery query) {
    permissionPolicy.assertCanDownloadSafetyLearning();
    return new ExcelFile("安全学习_数据下载.xlsx", workbook(contentMapper.selectList(queryWrapper(query))));
  }

  @Transactional
  public TrainingSafetyLearningImportResult importData(MultipartFile file) {
    permissionPolicy.assertCanCreateSafetyLearning();
    if (file == null || file.isEmpty()) {
      throw new BusinessException("请选择要上传的安全学习文件");
    }
    CurrentUser user = CurrentUserContext.require();
    List<String> errors = new ArrayList<>();
    int successRows = 0;
    DataFormatter formatter = new DataFormatter();
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file.getBytes()))) {
      var sheet = workbook.getSheetAt(0);
      assertHeaders(sheet.getRow(0), formatter);
      for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null || blank(formatter.formatCellValue(row.getCell(1))) && blank(formatter.formatCellValue(row.getCell(2)))) {
          continue;
        }
        try {
          TrainingSafetyLearningContent content = contentFromExcel(row, formatter, user.userId());
          contentMapper.insert(content);
          if (blank(content.code)) {
            content.code = code(content.learningDate, content.id);
            contentMapper.updateById(content);
          }
          successRows++;
        } catch (BusinessException exception) {
          errors.add("第" + (rowIndex + 1) + "行：" + exception.getMessage());
        }
      }
    } catch (IOException exception) {
      throw new BusinessException("安全学习文件解析失败");
    }
    auditLogService.record(SystemModule.TRAINING, "TRAINING_SAFETY_LEARNING", null, "IMPORT", "导入安全学习");
    return new TrainingSafetyLearningImportResult(successRows, errors);
  }

  private QueryWrapper<TrainingSafetyLearningContent> queryWrapper(TrainingSafetyLearningContentQuery query) {
    TrainingSafetyLearningContentQuery safe =
        query == null ? new TrainingSafetyLearningContentQuery(null, null, null, null, null, null, null, null) : query;
    QueryWrapper<TrainingSafetyLearningContent> wrapper =
        new QueryWrapper<TrainingSafetyLearningContent>().eq("deleted", 0);
    dataScopeService.applyCompanyReadScope(wrapper, "company_id");
    if (safe.companyId() != null) {
      dataScopeService.assertCanReadCompany(safe.companyId());
      wrapper.eq("company_id", safe.companyId());
    }
    if (!blank(safe.category()) && !"all".equalsIgnoreCase(safe.category())) {
      wrapper.eq("category", safe.category().trim());
    }
    if (!blank(safe.keyword())) {
      String keyword = safe.keyword().trim();
      wrapper.and(
          nested ->
              nested
                  .like("code", keyword)
                  .or()
                  .like("title", keyword)
                  .or()
                  .like("category", keyword));
    }
    if (safe.dateStart() != null) {
      wrapper.ge("learning_date", safe.dateStart());
    }
    if (safe.dateEnd() != null) {
      wrapper.le("learning_date", safe.dateEnd());
    }
    if (!blank(safe.status()) && !"all".equalsIgnoreCase(safe.status())) {
      wrapper.eq("status", TrainingSafetyLearningStatus.parse(safe.status()).name());
    }
    wrapper.orderByDesc("learning_date").orderByDesc("id");
    return wrapper;
  }

  private void publishAssignmentIfActive(
      TrainingSafetyLearningContent content, Long operatorUserId, String suffix) {
    if (!TrainingSafetyLearningStatus.ACTIVE.name().equals(content.status)) return;
    notificationOutboxService.enqueue(
        new DomainNotificationEvent(
            "safety-learning:assigned:" + content.id + ":" + suffix,
            "LEARNING_ASSIGNED",
            BIZ_TYPE,
            content.id,
            content.companyId,
            List.of(),
            List.of(operatorUserId),
            content.createdBy,
            operatorUserId,
            null,
            Map.of("summary", content.title)));
  }

  private void assertCanUpdate(TrainingSafetyLearningContentRequest request) {
    if (request != null && "INACTIVE".equalsIgnoreCase(request.status())) {
      permissionPolicy.assertCanVoidSafetyLearning();
      return;
    }
    permissionPolicy.assertCanCreateSafetyLearning();
  }

  private void applyRequest(TrainingSafetyLearningContent content, TrainingSafetyLearningContentRequest request, Long selfId) {
    if (request == null) {
      throw new BusinessException("安全学习内容不能为空");
    }
    if (request.companyId() == null) {
      throw new BusinessException("公司不能为空");
    }
    dataScopeService.assertCanAccessOrg(request.companyId());
    if (blank(request.category())) {
      throw new BusinessException("分类不能为空");
    }
    if (blank(request.title())) {
      throw new BusinessException("标题不能为空");
    }
    if (request.learningDate() == null) {
      throw new BusinessException("日期不能为空");
    }
    String code = text(request.code());
    if (blank(code) && selfId != null) {
      code = code(request.learningDate(), selfId);
    }
    if (!blank(code)) {
      assertUniqueCode(code, selfId);
    }
    content.companyId = request.companyId();
    content.category = text(request.category());
    content.title = text(request.title());
    content.content = text(request.content());
    content.coverImage = text(request.coverImage());
    content.video = text(request.video());
    content.learningDate = request.learningDate();
    content.durationText = text(request.durationText());
    content.code = code;
    content.attachmentText = text(request.attachmentText());
    content.htmlExtract = text(request.htmlExtract());
    content.draft = text(request.draft());
    content.status = TrainingSafetyLearningStatus.parse(request.status()).name();
  }

  private TrainingSafetyLearningContent contentFromExcel(Row row, DataFormatter formatter, Long userId) {
    String company = text(formatter.formatCellValue(row.getCell(0)));
    String category = text(formatter.formatCellValue(row.getCell(1)));
    String title = text(formatter.formatCellValue(row.getCell(2)));
    LocalDate date = parseDate(formatter.formatCellValue(row.getCell(6)));
    String code = text(formatter.formatCellValue(row.getCell(8)));
    if (!blank(code)) {
      assertUniqueCode(code, null);
    }
    TrainingSafetyLearningStatus status = TrainingSafetyLearningStatus.parse(formatter.formatCellValue(row.getCell(12)));
    TrainingSafetyLearningContent content = new TrainingSafetyLearningContent();
    content.companyId = companyId(company);
    if (blank(category)) {
      throw new BusinessException("分类不能为空");
    }
    if (blank(title)) {
      throw new BusinessException("标题不能为空");
    }
    content.category = category;
    content.title = title;
    content.content = text(formatter.formatCellValue(row.getCell(3)));
    content.coverImage = text(formatter.formatCellValue(row.getCell(4)));
    content.video = text(formatter.formatCellValue(row.getCell(5)));
    content.learningDate = date;
    content.durationText = text(formatter.formatCellValue(row.getCell(7)));
    content.code = code;
    content.attachmentText = text(formatter.formatCellValue(row.getCell(9)));
    content.htmlExtract = text(formatter.formatCellValue(row.getCell(10)));
    content.draft = text(formatter.formatCellValue(row.getCell(11)));
    content.status = status.name();
    content.createdBy = userId;
    content.updatedBy = userId;
    return content;
  }

  private TrainingSafetyLearningContentResponse response(TrainingSafetyLearningContent content) {
    TrainingSafetyLearningStatus status = TrainingSafetyLearningStatus.parse(content.status);
    return new TrainingSafetyLearningContentResponse(
        content.id,
        content.companyId,
        orgName(content.companyId),
        content.category,
        content.title,
        content.content,
        content.coverImage,
        attachmentResponse(content.coverImageId),
        content.video,
        attachmentResponse(content.videoId),
        content.learningDate,
        content.durationText,
        content.code,
        attachmentResponse(content.attachmentId),
        content.attachmentText,
        content.htmlExtract,
        content.draft,
        status.name(),
        status.label(),
        content.createdAt,
        content.updatedAt);
  }

  private byte[] workbook(List<TrainingSafetyLearningContent> rows) {
    try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("Data");
      CellStyle headerStyle = workbook.createCellStyle();
      Font font = workbook.createFont();
      font.setBold(true);
      headerStyle.setFont(font);
      Row header = sheet.createRow(0);
      for (int i = 0; i < EXCEL_HEADERS.size(); i++) {
        Cell cell = header.createCell(i);
        cell.setCellValue(EXCEL_HEADERS.get(i));
        cell.setCellStyle(headerStyle);
      }
      for (int i = 0; i < rows.size(); i++) {
        writeExcelRow(sheet.createRow(i + 1), rows.get(i));
      }
      for (int i = 0; i < EXCEL_HEADERS.size(); i++) {
        sheet.autoSizeColumn(i);
      }
      workbook.write(output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new BusinessException("生成安全学习 Excel 失败");
    }
  }

  private void writeExcelRow(Row row, TrainingSafetyLearningContent content) {
    AttachmentResponse attachment = attachmentResponse(content.attachmentId);
    String attachmentText =
        attachment == null ? value(content.attachmentText) : attachment.originalName() + " " + attachment.url();
    AttachmentResponse coverImage = attachmentResponse(content.coverImageId);
    String coverImageText =
        coverImage == null ? value(content.coverImage) : coverImage.originalName() + " " + coverImage.url();
    AttachmentResponse video = attachmentResponse(content.videoId);
    String videoText = video == null ? value(content.video) : video.originalName() + " " + video.url();
    List<String> values =
        List.of(
            orgName(content.companyId),
            value(content.category),
            value(content.title),
            value(content.content),
            coverImageText,
            videoText,
            content.learningDate == null ? "" : content.learningDate.toString(),
            value(content.durationText),
            value(content.code),
            attachmentText,
            value(content.htmlExtract),
            value(content.draft),
            TrainingSafetyLearningStatus.parse(content.status).label());
    for (int i = 0; i < values.size(); i++) {
      row.createCell(i).setCellValue(values.get(i));
    }
  }

  private void assertHeaders(Row row, DataFormatter formatter) {
    if (row == null) {
      throw new BusinessException("导入模板表头不能为空");
    }
    for (int i = 0; i < EXCEL_HEADERS.size(); i++) {
      if (!EXCEL_HEADERS.get(i).equals(formatter.formatCellValue(row.getCell(i)))) {
        throw new BusinessException("导入模板表头不匹配");
      }
    }
  }

  private void assertUniqueCode(String code, Long selfId) {
    QueryWrapper<TrainingSafetyLearningContent> wrapper =
        new QueryWrapper<TrainingSafetyLearningContent>().eq("deleted", 0).eq("code", code);
    if (selfId != null) {
      wrapper.ne("id", selfId);
    }
    if (contentMapper.selectCount(wrapper) > 0) {
      throw new BusinessException("编码已存在");
    }
  }

  private ThreeCheckRecord findMiniLearningPointsRecord(String sourceRecordId, String clientRequestId) {
    if (!blank(clientRequestId)) {
      ThreeCheckRecord byRequest =
          recordMapper.selectOne(
              new QueryWrapper<ThreeCheckRecord>()
                  .eq("module_key", "points-flow")
                  .eq("source_channel", MINI_SOURCE_CHANNEL)
                  .eq("client_request_id", clientRequestId.trim())
                  .eq("deleted", 0)
                  .last("limit 1"));
      if (byRequest != null) {
        return byRequest;
      }
    }
    return recordMapper.selectOne(
        new QueryWrapper<ThreeCheckRecord>()
            .eq("module_key", "points-flow")
            .eq("source_channel", MINI_SOURCE_CHANNEL)
            .eq("source_record_id", sourceRecordId)
            .eq("deleted", 0)
            .last("limit 1"));
  }

  private ThreeCheckRecord createMiniLearningPointsRecord(
      TrainingSafetyLearningContent content,
      CurrentUser user,
      MiniSafetyLearningCheckInRequest request,
      String sourceRecordId) {
    OrgScope scope = resolveLearningOrgScope(content.companyId, user);
    LocalDateTime now = LocalDateTime.now();
    ThreeCheckRecord record = new ThreeCheckRecord();
    record.moduleKey = "points-flow";
    record.recordNo = miniLearningPointsRecordNo(content.learningDate);
    record.companyId = content.companyId;
    record.departmentId = scope.departmentId();
    record.teamId = scope.teamId();
    record.ownerUserId = user.userId();
    record.businessDate = content.learningDate == null ? LocalDate.now() : content.learningDate;
    record.status = "OPENED";
    record.payloadJson = writeJson(miniLearningPointsPayload(content, user, now));
    record.imageCheckStatus = "未上传";
    record.videoCheckStatus = "未上传";
    record.reminderCount = 0;
    record.version = 0;
    record.createdBy = user.userId();
    record.updatedBy = user.userId();
    record.sourceChannel = MINI_SOURCE_CHANNEL;
    record.sourceRecordId = sourceRecordId;
    record.clientRequestId = resolveMiniClientRequestId(request, sourceRecordId);
    record.clientUpdatedAt = now;
    record.lastSyncedAt = now;
    record.createdAt = now;
    record.updatedAt = now;
    record.deleted = 0;
    recordMapper.insert(record);
    auditLogService.record(
        SystemModule.TRAINING,
        "TRAINING_SAFETY_LEARNING",
        content.id,
        "MINI_CHECK_IN",
        "小程序安全学习打卡");
    return record;
  }

  private Map<String, Object> miniLearningPointsPayload(
      TrainingSafetyLearningContent content, CurrentUser user, LocalDateTime now) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("createdAt", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    payload.put("user", user.realName());
    payload.put("pointsReason", "安全学习打卡：" + content.title);
    payload.put("pointsChange", "加分");
    payload.put("pointsQuantity", MINI_LEARNING_POINTS);
    payload.put("learningContentId", content.id);
    payload.put("learningTitle", content.title);
    payload.put("source", "安全学习");
    return payload;
  }

  private OrgScope resolveLearningOrgScope(Long companyId, CurrentUser user) {
    SysOrg company = orgMapper.selectById(companyId);
    if (company == null || Integer.valueOf(1).equals(company.deleted)) {
      throw new BusinessException("公司不存在");
    }

    Long departmentId = null;
    Long teamId = null;
    for (Long orgId : orgPathIds(user.orgPath())) {
      SysOrg org = orgMapper.selectById(orgId);
      if (org == null || Integer.valueOf(1).equals(org.deleted) || !withinCompany(org, company)) {
        continue;
      }
      if ("DEPARTMENT".equals(org.orgType)) {
        departmentId = org.id;
      } else if ("TEAM".equals(org.orgType)) {
        teamId = org.id;
        if (departmentId == null) {
          departmentId = org.parentId;
        }
      }
    }

    if (departmentId == null) {
      SysOrg department = firstOrgUnder(company.orgPath, "DEPARTMENT");
      departmentId = department == null ? null : department.id;
    }
    if (teamId == null) {
      SysOrg team =
          departmentId == null ? firstOrgUnder(company.orgPath, "TEAM") : firstOrgUnder(orgPath(departmentId), "TEAM");
      teamId = team == null ? null : team.id;
    }
    if (departmentId == null || teamId == null) {
      throw new BusinessException("当前用户未绑定班组，无法生成积分");
    }
    return new OrgScope(departmentId, teamId);
  }

  private List<Long> orgPathIds(String orgPath) {
    if (blank(orgPath)) {
      return List.of();
    }
    List<Long> ids = new ArrayList<>();
    for (String part : orgPath.split("/")) {
      if (!blank(part)) {
        ids.add(Long.valueOf(part));
      }
    }
    return ids;
  }

  private boolean withinCompany(SysOrg org, SysOrg company) {
    return org.orgPath != null && company.orgPath != null && org.orgPath.startsWith(company.orgPath);
  }

  private SysOrg firstOrgUnder(String parentPath, String orgType) {
    if (blank(parentPath)) {
      return null;
    }
    return orgMapper.selectOne(
        new QueryWrapper<SysOrg>()
            .eq("deleted", 0)
            .eq("status", "ACTIVE")
            .eq("org_type", orgType)
            .likeRight("org_path", parentPath)
            .orderByAsc("sort_order")
            .orderByAsc("id")
            .last("limit 1"));
  }

  private String orgPath(Long orgId) {
    SysOrg org = orgMapper.selectById(orgId);
    return org == null ? "" : org.orgPath;
  }

  private String resolveMiniClientRequestId(MiniSafetyLearningCheckInRequest request, String sourceRecordId) {
    if (request != null && !blank(request.clientRequestId())) {
      return request.clientRequestId().trim();
    }
    return sourceRecordId + "-" + UUID.randomUUID();
  }

  private String miniLearningPointsRecordNo(LocalDate learningDate) {
    LocalDate date = learningDate == null ? LocalDate.now() : learningDate;
    return "PF-LEARN-" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + shortId();
  }

  private String shortId() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
  }

  private String writeJson(Map<String, Object> payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException exception) {
      throw new BusinessException("安全学习打卡数据序列化失败");
    }
  }

  private TrainingSafetyLearningContent requireContent(Long id) {
    TrainingSafetyLearningContent content = contentMapper.selectById(id);
    if (content == null || Integer.valueOf(1).equals(content.deleted)) {
      throw new BusinessException("安全学习内容不存在");
    }
    dataScopeService.assertCanAccessOrg(content.companyId);
    return content;
  }

  private TrainingSafetyLearningContent requireReadableContent(Long id) {
    TrainingSafetyLearningContent content = contentMapper.selectById(id);
    if (content == null || Integer.valueOf(1).equals(content.deleted)) {
      throw new BusinessException("安全学习内容不存在");
    }
    dataScopeService.assertCanReadCompany(content.companyId);
    return content;
  }

  private void softDelete(List<Long> ids) {
    contentMapper.update(
        null,
        new UpdateWrapper<TrainingSafetyLearningContent>()
            .in("id", ids)
            .set("deleted", 1)
            .set("updated_by", CurrentUserContext.require().userId())
            .set("updated_at", LocalDateTime.now()));
  }

  private TrainingSafetyLearningContentResponse uploadMedia(
      Long id, MultipartFile file, String fileKind, String idColumn, String textColumn, String auditMessage) {
    TrainingSafetyLearningContent content = requireContent(id);
    if (file == null || file.isEmpty()) {
      throw new BusinessException("上传文件不能为空");
    }
    Long oldAttachmentId = "IMAGE".equals(fileKind) ? content.coverImageId : content.videoId;
    if (oldAttachmentId != null) {
      softDeleteAttachment(oldAttachmentId, content.id);
    }
    AttachmentResponse attachment =
        fileStorageService.saveBusinessAttachment(BIZ_TYPE, content.id, "training/safety-learning", fileKind, file);
    contentMapper.update(
        null,
        new UpdateWrapper<TrainingSafetyLearningContent>()
            .eq("id", content.id)
            .set(idColumn, Long.valueOf(attachment.id()))
            .set(textColumn, attachment.originalName())
            .set("updated_by", CurrentUserContext.require().userId())
            .set("updated_at", LocalDateTime.now()));
    auditLogService.record(
        SystemModule.TRAINING,
        "TRAINING_SAFETY_LEARNING",
        content.id,
        "UPLOAD_" + fileKind,
        auditMessage);
    return response(requireContent(content.id));
  }

  private TrainingSafetyLearningContentResponse removeMedia(
      Long id, String idColumn, String textColumn, String auditAction, String auditMessage) {
    TrainingSafetyLearningContent content = requireContent(id);
    Long attachmentId = "cover_image_id".equals(idColumn) ? content.coverImageId : content.videoId;
    if (attachmentId != null) {
      softDeleteAttachment(attachmentId, content.id);
    }
    contentMapper.update(
        null,
        new UpdateWrapper<TrainingSafetyLearningContent>()
            .eq("id", content.id)
            .set(idColumn, null)
            .set(textColumn, "")
            .set("updated_by", CurrentUserContext.require().userId())
            .set("updated_at", LocalDateTime.now()));
    auditLogService.record(SystemModule.TRAINING, "TRAINING_SAFETY_LEARNING", content.id, auditAction, auditMessage);
    return response(requireContent(content.id));
  }

  private void softDeleteAttachment(Long attachmentId, Long contentId) {
    attachmentMapper.update(
        null,
        new UpdateWrapper<BizAttachment>()
            .eq("id", attachmentId)
            .eq("biz_type", BIZ_TYPE)
            .eq("biz_id", contentId)
            .eq("deleted", 0)
            .set("deleted", 1));
  }

  private List<Long> safeIds(TrainingSafetyLearningBatchDeleteRequest request) {
    if (request == null || request.ids() == null || request.ids().isEmpty()) {
      throw new BusinessException("请选择要删除的安全学习内容");
    }
    return request.ids().stream().distinct().toList();
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

  private Long companyId(String company) {
    if (blank(company)) {
      throw new BusinessException("公司不能为空");
    }
    SysOrg org =
        orgMapper.selectOne(
            new QueryWrapper<SysOrg>()
                .eq("deleted", 0)
                .eq("org_type", "COMPANY")
                .and(wrapper -> wrapper.eq("org_name", company).or().eq("org_code", company))
                .last("limit 1"));
    if (org == null) {
      throw new BusinessException("公司不存在：" + company);
    }
    dataScopeService.assertCanAccessOrg(org.id);
    return org.id;
  }

  private String orgName(Long orgId) {
    SysOrg org = orgMapper.selectById(orgId);
    return org == null ? "" : org.orgName;
  }

  private String code(LocalDate date, Long id) {
    return "LEARN-" + CODE_MONTH.format(date) + "-" + String.format("%04d", id);
  }

  private LocalDate parseDate(String value) {
    if (blank(value)) {
      throw new BusinessException("日期不能为空");
    }
    try {
      return LocalDate.parse(value.trim());
    } catch (DateTimeParseException exception) {
      throw new BusinessException("日期格式必须为 yyyy-MM-dd");
    }
  }

  private boolean isPdf(MultipartFile file) {
    String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
    return "application/pdf".equals(contentType) || filename.endsWith(".pdf");
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

  private record OrgScope(Long departmentId, Long teamId) {}
}
