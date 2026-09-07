package com.pingan.banzu.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.config.StorageProperties;
import com.pingan.banzu.domain.BizAttachment;
import com.pingan.banzu.dto.AttachmentResponse;
import com.pingan.banzu.mapper.BizAttachmentMapper;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "pingan.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

  private final BizAttachmentMapper attachmentMapper;
  private final AttachmentUrlResolver attachmentUrlResolver;
  private final ThreeCheckRecordService threeCheckRecordService;
  private final FileUploadValidator uploadValidator;
  private final AttachmentObjectLifecycleService objectLifecycle;
  private final Path rootDir;

  public LocalFileStorageService(
      BizAttachmentMapper attachmentMapper,
      AttachmentUrlResolver attachmentUrlResolver,
      ThreeCheckRecordService threeCheckRecordService,
      FileUploadValidator uploadValidator,
      AttachmentObjectLifecycleService objectLifecycle,
      StorageProperties properties) {
    this.attachmentMapper = attachmentMapper;
    this.attachmentUrlResolver = attachmentUrlResolver;
    this.threeCheckRecordService = threeCheckRecordService;
    this.uploadValidator = uploadValidator;
    this.objectLifecycle = objectLifecycle;
    this.rootDir = Path.of(properties.rootDir()).toAbsolutePath().normalize();
  }

  @Transactional
  @Override
  public AttachmentResponse saveThreeCheckRecordAttachment(
      String moduleKey, Long recordId, String fileKind, MultipartFile file) {
    threeCheckRecordService.assertCanManageAttachmentBeforeUpload(moduleKey, recordId);
    AttachmentResponse response =
        saveBusinessAttachment(
            threeCheckRecordService.bizType(moduleKey),
            recordId,
            "three-check/" + moduleKey,
            fileKind,
            file);
    threeCheckRecordService.markAttachmentUploaded(moduleKey, recordId, response.fileKind());
    threeCheckRecordService.recordAttachmentChange(moduleKey, recordId, response);
    return response;
  }

  @Transactional
  @Override
  public void deleteThreeCheckRecordAttachment(String moduleKey, Long recordId, Long attachmentId) {
    String bizType = threeCheckRecordService.bizType(moduleKey);
    BizAttachment attachment = requireActiveAttachment(bizType, recordId, attachmentId);
    threeCheckRecordService.assertCanManageAttachmentBeforeUpload(moduleKey, recordId);
    softDeleteAttachment(bizType, recordId, attachmentId);
    threeCheckRecordService.markAttachmentDeleted(moduleKey, recordId, attachment.fileKind);
  }

  @Transactional
  @Override
  public AttachmentResponse saveMiniThreeCheckRecordAttachment(
      String moduleKey, Long recordId, String fileKind, MultipartFile file) {
    return saveThreeCheckRecordAttachment(moduleKey, recordId, fileKind, file);
  }

  @Transactional
  @Override
  public AttachmentResponse saveBusinessAttachment(
      String bizType, Long bizId, String moduleFolder, String fileKind, MultipartFile file) {
    CurrentUser user = CurrentUserContext.require();
    String normalizedKind = uploadValidator.validateAttachment(fileKind, file);
    String originalName = file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename();
    String safeName = originalName.replaceAll("[\\\\/:*?\"<>|]", "_");
    Path targetDir = rootDir.resolve(moduleFolder).resolve(String.valueOf(bizId)).resolve(normalizedKind.toLowerCase(Locale.ROOT));
    String storedName = UUID.randomUUID() + "-" + safeName;
    Path target = targetDir.resolve(storedName).normalize();
    try {
      Files.createDirectories(targetDir);
      file.transferTo(target);
    } catch (IOException exception) {
      throw new BusinessException("文件保存失败：" + exception.getMessage());
    }

    BizAttachment attachment = new BizAttachment();
    attachment.bizType = bizType;
    attachment.bizId = bizId;
    attachment.fileKind = normalizedKind;
    attachment.originalName = originalName;
    attachment.storagePath = rootDir.relativize(target).toString().replace('\\', '/');
    attachment.storageProvider = "LOCAL";
    attachment.bucketName = null;
    attachment.objectKey = attachment.storagePath;
    attachment.etag = null;
    attachment.contentType = uploadValidator.safeContentType(file);
    attachment.fileSize = file.getSize();
    attachment.uploadedBy = user.userId();
    attachment.uploadedAt = LocalDateTime.now();
    attachment.deleted = 0;
    try {
      attachmentMapper.insert(attachment);
      objectLifecycle.compensateOnRollback("LOCAL", null, attachment.objectKey);
    } catch (RuntimeException exception) {
      objectLifecycle.deleteNow("LOCAL", null, attachment.objectKey);
      throw exception;
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

  private BizAttachment requireActiveAttachment(String bizType, Long bizId, Long attachmentId) {
    BizAttachment attachment = attachmentMapper.selectById(attachmentId);
    if (attachment == null
        || Integer.valueOf(1).equals(attachment.deleted)
        || !bizType.equals(attachment.bizType)
        || !bizId.equals(attachment.bizId)) {
      throw new BusinessException("附件不存在");
    }
    return attachment;
  }

  private void softDeleteAttachment(String bizType, Long bizId, Long attachmentId) {
    int updated =
        attachmentMapper.update(
            null,
            new UpdateWrapper<BizAttachment>()
                .eq("id", attachmentId)
                .eq("biz_type", bizType)
                .eq("biz_id", bizId)
                .eq("deleted", 0)
                .set("deleted", 1));
    if (updated == 0) {
      throw new BusinessException("附件不存在");
    }
  }

}
