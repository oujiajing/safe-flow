package com.pingan.banzu.service;

import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.config.StorageProperties;
import com.pingan.banzu.domain.BizAttachment;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http.Method;
import io.minio.MinioClient;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "pingan.storage.provider", havingValue = "minio")
public class MinioAttachmentUrlResolver implements AttachmentUrlResolver {

  private final MinioClient presignClient;
  private final StorageProperties properties;

  public MinioAttachmentUrlResolver(
      @Qualifier("minioStorageClient") MinioClient storageClient,
      @Qualifier("minioPresignClient") MinioClient presignClient,
      StorageProperties properties) {
    this.presignClient = presignClient;
    this.properties = properties;
  }

  @Override
  public String url(BizAttachment attachment) {
    String bucket =
        attachment != null && attachment.bucketName != null && !attachment.bucketName.isBlank()
            ? attachment.bucketName
            : properties.bucket();
    String objectKey =
        attachment != null && attachment.objectKey != null && !attachment.objectKey.isBlank()
            ? attachment.objectKey
            : attachment == null ? "" : attachment.storagePath;
    try {
      return presignClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Method.GET)
              .bucket(bucket)
              .object(objectKey)
              .expiry(properties.presignedUrlMinutes(), TimeUnit.MINUTES)
              .build());
    } catch (Exception exception) {
      throw new BusinessException("附件访问地址生成失败：" + exception.getMessage());
    }
  }
}
