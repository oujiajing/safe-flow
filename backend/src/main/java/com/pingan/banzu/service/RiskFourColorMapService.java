package com.pingan.banzu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.domain.BizAttachment;
import com.pingan.banzu.domain.RiskFourColorMap;
import com.pingan.banzu.domain.SysOrg;
import com.pingan.banzu.dto.AttachmentResponse;
import com.pingan.banzu.dto.RiskFourColorMapRequest;
import com.pingan.banzu.dto.RiskFourColorMapResponse;
import com.pingan.banzu.mapper.BizAttachmentMapper;
import com.pingan.banzu.mapper.RiskFourColorMapMapper;
import com.pingan.banzu.mapper.SysOrgMapper;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import com.pingan.banzu.system.audit.AuditLogService;
import com.pingan.banzu.system.common.SystemModule;
import com.pingan.banzu.system.security.SystemDataScopeService;
import com.pingan.banzu.system.security.SystemPermissionService;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RiskFourColorMapService {

  private static final String BIZ_TYPE = "RISK_FOUR_COLOR_MAP";

  private final AttachmentUrlResolver attachmentUrlResolver;
  private final AuditLogService auditLogService;
  private final BizAttachmentMapper attachmentMapper;
  private final FileStorageService fileStorageService;
  private final RiskFourColorMapMapper mapMapper;
  private final SysOrgMapper orgMapper;
  private final SystemDataScopeService dataScopeService;
  private final SystemPermissionService permissionService;

  public RiskFourColorMapService(
      AttachmentUrlResolver attachmentUrlResolver,
      AuditLogService auditLogService,
      BizAttachmentMapper attachmentMapper,
      FileStorageService fileStorageService,
      RiskFourColorMapMapper mapMapper,
      SysOrgMapper orgMapper,
      SystemDataScopeService dataScopeService,
      SystemPermissionService permissionService) {
    this.attachmentUrlResolver = attachmentUrlResolver;
    this.auditLogService = auditLogService;
    this.attachmentMapper = attachmentMapper;
    this.fileStorageService = fileStorageService;
    this.mapMapper = mapMapper;
    this.orgMapper = orgMapper;
    this.dataScopeService = dataScopeService;
    this.permissionService = permissionService;
  }

  public PageResult<RiskFourColorMapResponse> list(
      Long companyId, String keyword, Integer page, Integer pageSize) {
    assertCanViewRisk();
    QueryWrapper<RiskFourColorMap> wrapper =
        new QueryWrapper<RiskFourColorMap>().eq("deleted", 0);
    dataScopeService.applyOrgScope(wrapper, "company_id");
    if (companyId != null) {
      dataScopeService.assertCanAccessOrg(companyId);
      wrapper.eq("company_id", companyId);
    }
    if (!blank(keyword)) {
      wrapper.and(
          nested ->
              nested
                  .like("name", keyword.trim())
                  .or()
                  .like("remark", keyword.trim()));
    }
    wrapper.orderByDesc("updated_at").orderByDesc("id");

    var all = mapMapper.selectList(wrapper);
    int size = pageSize == null ? 20 : pageSize;
    int from = Math.max(0, ((page == null ? 1 : page) - 1) * size);
    int to = Math.min(all.size(), from + size);
    if (from > to) {
      from = to;
    }
    return new PageResult<>(all.subList(from, to).stream().map(this::response).toList(), all.size());
  }

  @Transactional
  public RiskFourColorMapResponse create(RiskFourColorMapRequest request) {
    assertCanManageRisk();
    Long companyId = requireCompanyId(request);
    dataScopeService.assertCanAccessOrg(companyId);
    CurrentUser user = CurrentUserContext.require();

    RiskFourColorMap map = new RiskFourColorMap();
    if (blank(request.name())) {
      throw new BusinessException("四色图名称不能为空");
    }
    map.name = text(request.name());
    map.companyId = companyId;
    map.remark = text(request.remark());
    map.createdBy = user.userId();
    map.updatedBy = user.userId();
    mapMapper.insert(map);
    auditLogService.record(SystemModule.RISK_LEVEL_CONTROL, "RISK_FOUR_COLOR_MAP", map.id, "CREATE", "新建风险四色图");
    return response(requireMap(map.id));
  }

  @Transactional
  public RiskFourColorMapResponse update(Long id, RiskFourColorMapRequest request) {
    assertCanManageRisk();
    RiskFourColorMap map = requireMap(id);
    Long companyId = requireCompanyId(request);
    dataScopeService.assertCanAccessOrg(companyId);
    if (blank(request.name())) {
      throw new BusinessException("四色图名称不能为空");
    }
    map.name = text(request.name());
    map.companyId = companyId;
    map.remark = text(request.remark());
    map.updatedBy = CurrentUserContext.require().userId();
    map.updatedAt = LocalDateTime.now();
    mapMapper.updateById(map);
    auditLogService.record(SystemModule.RISK_LEVEL_CONTROL, "RISK_FOUR_COLOR_MAP", map.id, "UPDATE", "更新风险四色图");
    return response(requireMap(map.id));
  }

  @Transactional
  public RiskFourColorMapResponse uploadBackground(Long id, MultipartFile file) {
    assertCanManageRisk();
    RiskFourColorMap map = requireMap(id);
    if (file == null || file.isEmpty()) {
      throw new BusinessException("上传文件不能为空");
    }
    String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    if (!contentType.startsWith("image/")) {
      throw new BusinessException("风险四色图底图仅支持图片文件");
    }
    AttachmentResponse attachment =
        fileStorageService.saveBusinessAttachment(BIZ_TYPE, map.id, "risk-four-color-map", "IMAGE", file);
    map.backgroundAttachmentId = Long.valueOf(attachment.id());
    map.updatedBy = CurrentUserContext.require().userId();
    map.updatedAt = LocalDateTime.now();
    mapMapper.updateById(map);
    auditLogService.record(SystemModule.RISK_LEVEL_CONTROL, "RISK_FOUR_COLOR_MAP", map.id, "UPLOAD_BACKGROUND", "上传风险四色图底图");
    return response(requireMap(map.id));
  }

  @Transactional
  public void delete(Long id) {
    assertCanManageRisk();
    RiskFourColorMap map = requireMap(id);
    mapMapper.update(
        null,
        new UpdateWrapper<RiskFourColorMap>()
            .eq("id", map.id)
            .set("deleted", 1)
            .set("updated_by", CurrentUserContext.require().userId())
            .set("updated_at", LocalDateTime.now()));
    auditLogService.record(SystemModule.RISK_LEVEL_CONTROL, "RISK_FOUR_COLOR_MAP", map.id, "DELETE", "删除风险四色图");
  }

  private RiskFourColorMap requireMap(Long id) {
    RiskFourColorMap map = mapMapper.selectById(id);
    if (map == null || Integer.valueOf(1).equals(map.deleted)) {
      throw new BusinessException("风险四色图不存在");
    }
    dataScopeService.assertCanAccessOrg(map.companyId);
    return map;
  }

  private Long requireCompanyId(RiskFourColorMapRequest request) {
    if (request == null || request.companyId() == null) {
      throw new BusinessException("公司不能为空");
    }
    return request.companyId();
  }

  private RiskFourColorMapResponse response(RiskFourColorMap map) {
    return new RiskFourColorMapResponse(
        map.id,
        map.name,
        map.companyId,
        companyName(map.companyId),
        attachmentResponse(map.backgroundAttachmentId),
        map.remark,
        map.createdAt,
        map.updatedAt);
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

  private String text(String value) {
    return value == null ? "" : value.trim();
  }

  private boolean blank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private void assertCanViewRisk() {
    permissionService.assertHasPermission("PINGAN_RISK_ENTRY");
    permissionService.assertHasPermission("PINGAN_RISK_VIEW");
  }

  private void assertCanManageRisk() {
    permissionService.assertHasPermission("PINGAN_RISK_MANAGE");
  }
}
