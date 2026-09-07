package com.pingan.banzu.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.common.PageResult;
import com.pingan.banzu.domain.BizAttachment;
import com.pingan.banzu.domain.SysOrg;
import com.pingan.banzu.dto.AttachmentResponse;
import com.pingan.banzu.mapper.BizAttachmentMapper;
import com.pingan.banzu.mapper.SysOrgMapper;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import com.pingan.banzu.service.AttachmentUrlResolver;
import com.pingan.banzu.service.FileStorageService;
import com.pingan.banzu.system.audit.AuditLogService;
import com.pingan.banzu.system.common.SystemModule;
import com.pingan.banzu.system.domain.SysOrgContentProfile;
import com.pingan.banzu.system.dto.SystemContentProfileQuery;
import com.pingan.banzu.system.dto.SystemContentProfileRequest;
import com.pingan.banzu.system.dto.SystemContentProfileResponse;
import com.pingan.banzu.system.mapper.SysOrgContentProfileMapper;
import com.pingan.banzu.system.security.SystemDataScopeService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SystemContentProfileService {

  public static final String BIZ_TYPE = "SYS_ORG_CONTENT_PROFILE";
  private static final Set<String> STATUSES = Set.of("DRAFT", "ACTIVE", "INACTIVE");
  private static final Set<String> PROFILE_ORG_TYPES = Set.of("GROUP", "COMPANY");

  private final AttachmentUrlResolver attachmentUrlResolver;
  private final AuditLogService auditLogService;
  private final BizAttachmentMapper attachmentMapper;
  private final FileStorageService fileStorageService;
  private final SysOrgContentProfileMapper profileMapper;
  private final SysOrgMapper orgMapper;
  private final SystemDataScopeService dataScopeService;

  public SystemContentProfileService(
      AttachmentUrlResolver attachmentUrlResolver,
      AuditLogService auditLogService,
      BizAttachmentMapper attachmentMapper,
      FileStorageService fileStorageService,
      SysOrgContentProfileMapper profileMapper,
      SysOrgMapper orgMapper,
      SystemDataScopeService dataScopeService) {
    this.attachmentUrlResolver = attachmentUrlResolver;
    this.auditLogService = auditLogService;
    this.attachmentMapper = attachmentMapper;
    this.fileStorageService = fileStorageService;
    this.profileMapper = profileMapper;
    this.orgMapper = orgMapper;
    this.dataScopeService = dataScopeService;
  }

  public PageResult<SystemContentProfileResponse> list(SystemContentProfileQuery query) {
    List<SysOrgContentProfile> all = profileMapper.selectList(queryWrapper(query));
    Map<Long, SysOrg> orgs = orgMap();
    List<SystemContentProfileResponse> rows =
        all.stream()
            .filter(profile -> dataScopeService.canAccessOrg(profile.orgId))
            .map(profile -> response(profile, orgs.get(profile.orgId)))
            .toList();
    int page = Math.max(1, query == null || query.page() == null ? 1 : query.page());
    int size = Math.min(100, Math.max(1, query == null || query.pageSize() == null ? 20 : query.pageSize()));
    int from = Math.min((page - 1) * size, rows.size());
    int to = Math.min(from + size, rows.size());
    return new PageResult<>(rows.subList(from, to), rows.size());
  }

  @Transactional
  public SystemContentProfileResponse create(SystemContentProfileRequest request) {
    SysOrg org = requireProfileOrg(request.orgId());
    assertUniqueOrg(request.orgId(), null);
    CurrentUser user = CurrentUserContext.require();
    SysOrgContentProfile profile = new SysOrgContentProfile();
    apply(profile, request);
    profile.createdBy = user.userId();
    profile.updatedBy = user.userId();
    profile.createdAt = LocalDateTime.now();
    profile.updatedAt = profile.createdAt;
    profile.deleted = 0L;
    profileMapper.insert(profile);
    auditLogService.record(SystemModule.COMPANY, BIZ_TYPE, profile.id, "CREATE", "新增内容配置 " + org.orgName);
    return response(profile, org);
  }

  @Transactional
  public SystemContentProfileResponse update(Long id, SystemContentProfileRequest request) {
    SysOrgContentProfile profile = requireProfile(id);
    SysOrg org = requireProfileOrg(request.orgId());
    assertUniqueOrg(request.orgId(), id);
    apply(profile, request);
    profile.updatedBy = CurrentUserContext.require().userId();
    profile.updatedAt = LocalDateTime.now();
    profileMapper.updateById(profile);
    auditLogService.record(SystemModule.COMPANY, BIZ_TYPE, profile.id, "UPDATE", "更新内容配置 " + org.orgName);
    return response(requireProfile(id), org);
  }

  @Transactional
  public SystemContentProfileResponse status(Long id, String status) {
    SysOrgContentProfile profile = requireProfile(id);
    validateStatus(status);
    profile.status = status;
    profile.updatedBy = CurrentUserContext.require().userId();
    profile.updatedAt = LocalDateTime.now();
    profileMapper.updateById(profile);
    auditLogService.record(SystemModule.COMPANY, BIZ_TYPE, profile.id, "UPDATE_STATUS", "更新内容配置状态");
    return response(requireProfile(id), orgMapper.selectById(profile.orgId));
  }

  @Transactional
  public void delete(Long id) {
    SysOrgContentProfile profile = requireProfile(id);
    softDeleteAttachment(profile.imageAttachmentId, profile.id);
    softDeleteAttachment(profile.videoAttachmentId, profile.id);
    profileMapper.update(
        null,
        new UpdateWrapper<SysOrgContentProfile>()
            .eq("id", profile.id)
            .set("deleted", profile.id)
            .set("updated_by", CurrentUserContext.require().userId())
            .set("updated_at", LocalDateTime.now()));
    auditLogService.record(SystemModule.COMPANY, BIZ_TYPE, profile.id, "DELETE", "删除内容配置");
  }

  @Transactional
  public SystemContentProfileResponse uploadImage(Long id, MultipartFile file) {
    return uploadMedia(id, file, "IMAGE");
  }

  @Transactional
  public SystemContentProfileResponse uploadVideo(Long id, MultipartFile file) {
    return uploadMedia(id, file, "VIDEO");
  }

  @Transactional
  public SystemContentProfileResponse removeImage(Long id) {
    return removeMedia(id, "IMAGE");
  }

  @Transactional
  public SystemContentProfileResponse removeVideo(Long id) {
    return removeMedia(id, "VIDEO");
  }

  public SystemContentProfileResponse activeByOrg(Long orgId) {
    if (orgId == null) {
      throw new BusinessException("组织不能为空");
    }
    if (!dataScopeService.canAccessOrg(orgId)) {
      return null;
    }
    SysOrgContentProfile profile =
        profileMapper.selectOne(
            new QueryWrapper<SysOrgContentProfile>()
                .eq("org_id", orgId)
                .eq("status", "ACTIVE")
                .eq("deleted", 0)
                .last("limit 1"));
    return profile == null ? null : response(profile, orgMapper.selectById(profile.orgId));
  }

  public List<SystemContentProfileResponse> activeScreenVideos(Long orgId) {
    if (orgId != null) {
      dataScopeService.assertCanAccessOrg(orgId);
    }
    List<SysOrgContentProfile> profiles =
        profileMapper.selectList(
            new QueryWrapper<SysOrgContentProfile>()
                .eq("status", "ACTIVE")
                .eq("deleted", 0)
                .isNotNull("video_attachment_id")
                .orderByAsc("video_sort_order")
                .orderByAsc("id"));
    Map<Long, SysOrg> orgs = orgMap();
    return profiles.stream()
        .filter(profile -> orgId == null || orgId.equals(profile.orgId))
        .filter(profile -> dataScopeService.canAccessOrg(profile.orgId))
        .map(profile -> response(profile, orgs.get(profile.orgId)))
        .filter(response -> response.videoAttachment() != null)
        .toList();
  }

  private QueryWrapper<SysOrgContentProfile> queryWrapper(SystemContentProfileQuery query) {
    SystemContentProfileQuery safe =
        query == null ? new SystemContentProfileQuery(null, null, null, null, null) : query;
    QueryWrapper<SysOrgContentProfile> wrapper =
        new QueryWrapper<SysOrgContentProfile>().eq("deleted", 0);
    if (safe.orgId() != null) {
      dataScopeService.assertCanAccessOrg(safe.orgId());
      wrapper.eq("org_id", safe.orgId());
    }
    if (!blank(safe.status()) && !"all".equalsIgnoreCase(safe.status())) {
      validateStatus(safe.status());
      wrapper.eq("status", safe.status());
    }
    if (!blank(safe.keyword())) {
      String keyword = safe.keyword().trim();
      wrapper.and(nested -> nested.like("title", keyword).or().like("subtitle", keyword).or().like("description", keyword));
    }
    wrapper.orderByAsc("video_sort_order").orderByDesc("id");
    return wrapper;
  }

  private SystemContentProfileResponse uploadMedia(Long id, MultipartFile file, String fileKind) {
    SysOrgContentProfile profile = requireProfile(id);
    Long oldAttachmentId = "IMAGE".equals(fileKind) ? profile.imageAttachmentId : profile.videoAttachmentId;
    softDeleteAttachment(oldAttachmentId, profile.id);
    AttachmentResponse attachment =
        fileStorageService.saveBusinessAttachment(BIZ_TYPE, profile.id, "system/content-profiles", fileKind, file);
    if ("IMAGE".equals(fileKind)) {
      profile.imageAttachmentId = Long.valueOf(attachment.id());
    } else {
      profile.videoAttachmentId = Long.valueOf(attachment.id());
    }
    profile.updatedBy = CurrentUserContext.require().userId();
    profile.updatedAt = LocalDateTime.now();
    profileMapper.updateById(profile);
    auditLogService.record(SystemModule.COMPANY, BIZ_TYPE, profile.id, "UPLOAD_" + fileKind, "上传内容配置媒体");
    return response(requireProfile(id), orgMapper.selectById(profile.orgId));
  }

  private SystemContentProfileResponse removeMedia(Long id, String fileKind) {
    SysOrgContentProfile profile = requireProfile(id);
    Long attachmentId = "IMAGE".equals(fileKind) ? profile.imageAttachmentId : profile.videoAttachmentId;
    softDeleteAttachment(attachmentId, profile.id);
    if ("IMAGE".equals(fileKind)) {
      profile.imageAttachmentId = null;
    } else {
      profile.videoAttachmentId = null;
    }
    profile.updatedBy = CurrentUserContext.require().userId();
    profile.updatedAt = LocalDateTime.now();
    profileMapper.updateById(profile);
    auditLogService.record(SystemModule.COMPANY, BIZ_TYPE, profile.id, "REMOVE_" + fileKind, "删除内容配置媒体");
    return response(requireProfile(id), orgMapper.selectById(profile.orgId));
  }

  private void apply(SysOrgContentProfile profile, SystemContentProfileRequest request) {
    if (request == null) {
      throw new BusinessException("内容配置不能为空");
    }
    validateStatus(statusOrDraft(request.status()));
    profile.orgId = request.orgId();
    profile.title = text(request.title());
    profile.subtitle = text(request.subtitle());
    profile.description = text(request.description());
    profile.videoTitle = text(request.videoTitle());
    profile.videoSortOrder = request.videoSortOrder() == null ? 0 : request.videoSortOrder();
    profile.status = statusOrDraft(request.status());
  }

  private SysOrg requireProfileOrg(Long orgId) {
    if (orgId == null) {
      throw new BusinessException("组织不能为空");
    }
    dataScopeService.assertCanAccessOrg(orgId);
    SysOrg org = orgMapper.selectById(orgId);
    if (org == null || Integer.valueOf(1).equals(org.deleted) || !PROFILE_ORG_TYPES.contains(org.orgType)) {
      throw new BusinessException("内容配置仅支持集团或公司组织");
    }
    return org;
  }

  private SysOrgContentProfile requireProfile(Long id) {
    SysOrgContentProfile profile = profileMapper.selectById(id);
    if (profile == null || !Long.valueOf(0).equals(profile.deleted)) {
      throw new BusinessException("内容配置不存在");
    }
    dataScopeService.assertCanAccessOrg(profile.orgId);
    return profile;
  }

  private void assertUniqueOrg(Long orgId, Long selfId) {
    QueryWrapper<SysOrgContentProfile> wrapper =
        new QueryWrapper<SysOrgContentProfile>().eq("org_id", orgId).eq("deleted", 0);
    if (selfId != null) {
      wrapper.ne("id", selfId);
    }
    if (profileMapper.selectCount(wrapper) > 0) {
      throw new BusinessException("该组织已存在内容配置");
    }
  }

  private void softDeleteAttachment(Long attachmentId, Long profileId) {
    if (attachmentId == null) {
      return;
    }
    attachmentMapper.update(
        null,
        new UpdateWrapper<BizAttachment>()
            .eq("id", attachmentId)
            .eq("biz_type", BIZ_TYPE)
            .eq("biz_id", profileId)
            .eq("deleted", 0)
            .set("deleted", 1));
  }

  private SystemContentProfileResponse response(SysOrgContentProfile profile, SysOrg org) {
    return new SystemContentProfileResponse(
        profile.id,
        profile.orgId,
        org == null ? "" : org.orgName,
        org == null ? "" : org.orgType,
        profile.title,
        profile.subtitle,
        profile.description,
        attachmentResponse(profile.imageAttachmentId),
        attachmentResponse(profile.videoAttachmentId),
        profile.videoTitle,
        profile.videoSortOrder,
        profile.status,
        profile.createdAt,
        profile.updatedAt);
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

  private Map<Long, SysOrg> orgMap() {
    return orgMapper.selectList(new QueryWrapper<SysOrg>().eq("deleted", 0)).stream()
        .collect(Collectors.toMap(org -> org.id, Function.identity(), (left, right) -> left));
  }

  private String statusOrDraft(String status) {
    return blank(status) ? "DRAFT" : status.trim();
  }

  private void validateStatus(String status) {
    if (!STATUSES.contains(status)) {
      throw new BusinessException("状态不合法");
    }
  }

  private String text(String value) {
    return value == null ? "" : value.trim();
  }

  private boolean blank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
