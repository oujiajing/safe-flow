package com.pingan.banzu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.domain.BizAttachment;
import com.pingan.banzu.domain.SafetyLedgerDocument;
import com.pingan.banzu.domain.SysOrg;
import com.pingan.banzu.dto.AttachmentResponse;
import com.pingan.banzu.dto.SafetyLedgerDocumentBatchDeleteRequest;
import com.pingan.banzu.dto.SafetyLedgerDocumentQuery;
import com.pingan.banzu.dto.SafetyLedgerDocumentResponse;
import com.pingan.banzu.mapper.BizAttachmentMapper;
import com.pingan.banzu.mapper.SafetyLedgerDocumentMapper;
import com.pingan.banzu.mapper.SysOrgMapper;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import com.pingan.banzu.system.security.SystemDataScopeService;
import com.pingan.banzu.system.security.SystemPermissionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SafetyLedgerDocumentService {

  private static final String BIZ_TYPE = "SAFETY_LEDGER_DOCUMENT";
  private static final String PERMISSION_LEDGER_ENTRY = "PINGAN_LEDGER_ENTRY";
  private static final String PERMISSION_LEDGER_VIEW = "PINGAN_LEDGER_VIEW";
  private static final String PERMISSION_LEDGER_MANAGE = "PINGAN_LEDGER_MANAGE";
  private static final Set<String> DOCUMENT_LEDGER_KEYS =
      Set.of(
          "safety-rules",
          "operating-procedures",
          "post-duties",
          "full-responsibility",
          "annual-plan",
          "special-activity-plan",
          "risk-control-document",
          "return-to-work-six-ones",
          "hazard-governance-document");

  private final AttachmentUrlResolver attachmentUrlResolver;
  private final BizAttachmentMapper attachmentMapper;
  private final FileStorageService fileStorageService;
  private final NotificationOutboxService notificationOutboxService;
  private final SafetyLedgerDocumentMapper documentMapper;
  private final SysOrgMapper orgMapper;
  private final SystemDataScopeService dataScopeService;
  private final SystemPermissionService permissionService;

  public SafetyLedgerDocumentService(
      AttachmentUrlResolver attachmentUrlResolver,
      BizAttachmentMapper attachmentMapper,
      FileStorageService fileStorageService,
      NotificationOutboxService notificationOutboxService,
      SafetyLedgerDocumentMapper documentMapper,
      SysOrgMapper orgMapper,
      SystemDataScopeService dataScopeService,
      SystemPermissionService permissionService) {
    this.attachmentUrlResolver = attachmentUrlResolver;
    this.attachmentMapper = attachmentMapper;
    this.fileStorageService = fileStorageService;
    this.notificationOutboxService = notificationOutboxService;
    this.documentMapper = documentMapper;
    this.orgMapper = orgMapper;
    this.dataScopeService = dataScopeService;
    this.permissionService = permissionService;
  }

  public PageResult<SafetyLedgerDocumentResponse> list(SafetyLedgerDocumentQuery query) {
    assertViewPermission();
    List<SafetyLedgerDocument> all = documentMapper.selectList(queryWrapper(query));
    int size = query == null || query.pageSize() == null ? 20 : query.pageSize();
    int page = query == null || query.page() == null ? 1 : query.page();
    int from = Math.max(0, (page - 1) * size);
    int to = Math.min(all.size(), from + size);
    if (from > to) {
      from = to;
    }
    return new PageResult<>(all.subList(from, to).stream().map(this::response).toList(), all.size());
  }

  @Transactional
  public SafetyLedgerDocumentResponse create(
      String ledgerKey,
      String name,
      Long companyId,
      String richText,
      String department,
      String team,
      String type,
      LocalDate date,
      MultipartFile file) {
    assertManagePermission();
    CurrentUser user = CurrentUserContext.require();
    validateLedgerKey(ledgerKey);
    if (blank(name)) {
      throw new BusinessException("名称不能为空");
    }
    requireCompany(companyId);
    validateDocumentFile(file);

    SafetyLedgerDocument document = new SafetyLedgerDocument();
    document.ledgerKey = ledgerKey.trim();
    document.name = text(name);
    document.richText = text(richText);
    document.companyId = companyId;
    document.department = text(department);
    document.team = text(team);
    document.documentType = text(type);
    document.documentDate = date;
    document.createdBy = user.userId();
    document.updatedBy = user.userId();
    document.deleted = 0;
    documentMapper.insert(document);

    AttachmentResponse attachment =
        fileStorageService.saveBusinessAttachment(BIZ_TYPE, document.id, "safety-ledger", "DOCUMENT", file);
    document.attachmentId = Long.valueOf(attachment.id());
    document.updatedAt = LocalDateTime.now();
    documentMapper.updateById(document);
    publishDocument(document, user.userId(), "created");
    return response(requireDocument(document.id));
  }

  @Transactional
  public void batchDelete(SafetyLedgerDocumentBatchDeleteRequest request) {
    assertManagePermission();
    List<Long> ids = safeIds(request);
    ids.forEach(this::requireDocument);
    documentMapper.update(
        null,
        new UpdateWrapper<SafetyLedgerDocument>()
            .in("id", ids)
            .set("deleted", 1)
            .set("updated_by", CurrentUserContext.require().userId())
            .set("updated_at", LocalDateTime.now()));
  }

  @Transactional
  public SafetyLedgerDocumentResponse update(
      Long id,
      String ledgerKey,
      String name,
      Long companyId,
      String richText,
      String department,
      String team,
      String type,
      LocalDate date,
      MultipartFile file) {
    assertManagePermission();
    CurrentUser user = CurrentUserContext.require();
    validateLedgerKey(ledgerKey);
    if (blank(name)) {
      throw new BusinessException("名称不能为空");
    }
    requireCompany(companyId);

    SafetyLedgerDocument document = requireDocument(id);
    document.ledgerKey = ledgerKey.trim();
    document.name = text(name);
    document.richText = text(richText);
    document.companyId = companyId;
    document.department = text(department);
    document.team = text(team);
    document.documentType = text(type);
    document.documentDate = date;
    document.updatedBy = user.userId();
    document.updatedAt = LocalDateTime.now();
    if (file != null && !file.isEmpty()) {
      validateDocumentFile(file);
      AttachmentResponse attachment =
          fileStorageService.saveBusinessAttachment(BIZ_TYPE, document.id, "safety-ledger", "DOCUMENT", file);
      document.attachmentId = Long.valueOf(attachment.id());
    }
    documentMapper.updateById(document);
    publishDocument(
        document,
        user.userId(),
        "updated-" + document.updatedAt);
    return response(requireDocument(id));
  }

  @Transactional
  public void delete(Long id) {
    assertManagePermission();
    requireDocument(id);
    documentMapper.update(
        null,
        new UpdateWrapper<SafetyLedgerDocument>()
            .eq("id", id)
            .set("deleted", 1)
            .set("updated_by", CurrentUserContext.require().userId())
            .set("updated_at", LocalDateTime.now()));
  }

  private QueryWrapper<SafetyLedgerDocument> queryWrapper(SafetyLedgerDocumentQuery query) {
    SafetyLedgerDocumentQuery safe =
        query == null ? new SafetyLedgerDocumentQuery(null, null, null, null, null, null, null, null, null) : query;
    QueryWrapper<SafetyLedgerDocument> wrapper = new QueryWrapper<SafetyLedgerDocument>().eq("deleted", 0);
    if (!blank(safe.ledgerKey())) {
      validateLedgerKey(safe.ledgerKey());
      wrapper.eq("ledger_key", safe.ledgerKey().trim());
    }
    dataScopeService.applyOrgScope(wrapper, "company_id");
    if (safe.companyId() != null) {
      requireCompany(safe.companyId());
      wrapper.eq("company_id", safe.companyId());
    }
    if (!blank(safe.keyword())) {
      String keyword = safe.keyword().trim();
      wrapper.and(
          nested ->
              nested
                  .like("name", keyword)
                  .or()
                  .like("rich_text", keyword)
                  .or()
                  .like("document_type", keyword));
    }
    if (safe.dateStart() != null) {
      wrapper.ge("document_date", safe.dateStart());
    }
    if (safe.dateEnd() != null) {
      wrapper.le("document_date", safe.dateEnd());
    }
    if (safe.uploadedStart() != null) {
      wrapper.ge("created_at", safe.uploadedStart().atStartOfDay());
    }
    if (safe.uploadedEnd() != null) {
      wrapper.lt("created_at", safe.uploadedEnd().plusDays(1).atStartOfDay());
    }
    wrapper.orderByDesc("created_at").orderByDesc("id");
    return wrapper;
  }

  private void publishDocument(
      SafetyLedgerDocument document, Long operatorUserId, String suffix) {
    notificationOutboxService.enqueue(
        new DomainNotificationEvent(
            "safety-ledger:published:" + document.id + ":" + suffix,
            "LEDGER_PUBLISHED",
            BIZ_TYPE,
            document.id,
            document.companyId,
            List.of(),
            List.of(operatorUserId),
            document.createdBy,
            operatorUserId,
            null,
            Map.of("summary", document.name)));
  }

  private void assertViewPermission() {
    permissionService.assertHasPermission(PERMISSION_LEDGER_ENTRY);
    permissionService.assertHasPermission(PERMISSION_LEDGER_VIEW);
  }

  private void assertManagePermission() {
    permissionService.assertHasPermission(PERMISSION_LEDGER_MANAGE);
  }

  private SafetyLedgerDocumentResponse response(SafetyLedgerDocument document) {
    return new SafetyLedgerDocumentResponse(
        document.id,
        document.ledgerKey,
        document.name,
        document.richText,
        document.companyId,
        orgName(document.companyId),
        document.department,
        document.team,
        document.documentType,
        document.documentDate,
        attachmentResponse(document.attachmentId),
        document.createdAt,
        document.updatedAt);
  }

  private SafetyLedgerDocument requireDocument(Long id) {
    SafetyLedgerDocument document = documentMapper.selectById(id);
    if (document == null || Integer.valueOf(1).equals(document.deleted)) {
      throw new BusinessException("安全台账文档不存在");
    }
    dataScopeService.assertCanAccessOrg(document.companyId);
    return document;
  }

  private SysOrg requireCompany(Long companyId) {
    if (companyId == null) {
      throw new BusinessException("所属企业不能为空");
    }
    SysOrg org = orgMapper.selectById(companyId);
    if (org == null
        || Integer.valueOf(1).equals(org.deleted)
        || !"COMPANY".equals(org.orgType)
        || !"ACTIVE".equals(org.status)) {
      throw new BusinessException("所属企业必须来自公司管理");
    }
    dataScopeService.assertCanAccessOrg(companyId);
    return org;
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

  private void validateLedgerKey(String ledgerKey) {
    if (blank(ledgerKey) || !DOCUMENT_LEDGER_KEYS.contains(ledgerKey.trim())) {
      throw new BusinessException("安全台账目录不支持自维护");
    }
  }

  private void validateDocumentFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException("上传文件不能为空");
    }
    String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
    if (!filename.endsWith(".pdf") && !filename.endsWith(".doc") && !filename.endsWith(".docx")) {
      throw new BusinessException("文件仅支持 PDF、Word");
    }
  }

  private List<Long> safeIds(SafetyLedgerDocumentBatchDeleteRequest request) {
    if (request == null || request.ids() == null || request.ids().isEmpty()) {
      throw new BusinessException("请选择要删除的安全台账文档");
    }
    return request.ids().stream().distinct().toList();
  }

  private String orgName(Long orgId) {
    SysOrg org = orgMapper.selectById(orgId);
    return org == null ? "" : org.orgName;
  }

  private String text(String value) {
    return value == null ? "" : value.trim();
  }

  private boolean blank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
