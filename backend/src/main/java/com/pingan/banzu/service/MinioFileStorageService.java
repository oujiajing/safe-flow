package com.pingan.banzu.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.config.StorageProperties;
import com.pingan.banzu.domain.BizAttachment;
import com.pingan.banzu.dto.AttachmentResponse;
import com.pingan.banzu.mapper.BizAttachmentMapper;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "pingan.storage.provider", havingValue = "minio")
public class MinioFileStorageService implements FileStorageService {

  private final BizAttachmentMapper attachmentMapper;
  private final ThreeCheckRecordService threeCheckRecordService;
  private final StorageProperties properties;
  private final MinioClient minioClient;
  private final AttachmentUrlResolver attachmentUrlResolver;
  private final FileUploadValidator uploadValidator;
  private final AttachmentObjectLifecycleService objectLifecycle;
  private volatile boolean bucketReady;

  public MinioFileStorageService(
      BizAttachmentMapper attachmentMapper,
      ThreeCheckRecordService threeCheckRecordService,
      StorageProperties properties,
      MinioClient minioClient,
      AttachmentUrlResolver attachmentUrlResolver,
      FileUploadValidator uploadValidator,
      AttachmentObjectLifecycleService objectLifecycle) {
    this.attachmentMapper = attachmentMapper;
    this.threeCheckRecordService = threeCheckRecordService;
    this.properties = properties;
    this.minioClient = minioClient;
    this.attachmentUrlResolver = attachmentUrlResolver;
    this.uploadValidator = uploadValidator;
    this.objectLifecycle = objectLifecycle;
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
    String objectKey =
        moduleFolder
            + "/"
            + bizId
            + "/"
            + normalizedKind.toLowerCase(Locale.ROOT)
            + "/"
            + UUID.randomUUID()
            + "-"
            + safeName;
    String contentType = uploadValidator.safeContentType(file);
    ObjectWriteResponse writeResponse = putObject(objectKey, file, contentType);

    BizAttachment attachment = new BizAttachment();
    attachment.bizType = bizType;
    attachment.bizId = bizId;
    attachment.fileKind = normalizedKind;
    attachment.originalName = originalName;
    attachment.storagePath = objectKey;
    attachment.storageProvider = "MINIO";
    attachment.bucketName = properties.bucket();
    attachment.objectKey = objectKey;
    attachment.etag = writeResponse.etag();
    attachment.contentType = contentType;
    attachment.fileSize = file.getSize();
    attachment.uploadedBy = user.userId();
    attachment.uploadedAt = LocalDateTime.now();
    attachment.deleted = 0;
    try {
      attachmentMapper.insert(attachment);
      objectLifecycle.compensateOnRollback("MINIO", attachment.bucketName, attachment.objectKey);
    } catch (RuntimeException exception) {
      objectLifecycle.deleteNow("MINIO", attachment.bucketName, attachment.objectKey);
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

  private ObjectWriteResponse putObject(String objectKey, MultipartFile file, String contentType) {
    try {
      ensureBucket();
      try (InputStream inputStream = file.getInputStream()) {
        return minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(properties.bucket())
                .object(objectKey)
                .stream(inputStream, file.getSize(), -1L)
                .contentType(contentType)
                .build());
      }
    } catch (Exception exception) {
      throw new BusinessException("文件保存失败：" + exception.getMessage());
    }
  }

  private void ensureBucket() throws Exception {
    if (bucketReady) {
      return;
    }
    synchronized (this) {
      if (bucketReady) {
        return;
      }
      boolean exists =
          minioClient.bucketExists(BucketExistsArgs.builder().bucket(properties.bucket()).build());
      if (!exists) {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.bucket()).build());
      }
      bucketReady = true;
    }
  }

}
