package com.pingan.banzu.service;

import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.config.StorageProperties;
import com.pingan.banzu.domain.BizAttachment;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import java.io.InputStream;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "pingan.storage.provider", havingValue = "minio")
public class MinioAttachmentContentReader implements AttachmentContentReader {
  private final MinioClient client;
  private final StorageProperties properties;

  public MinioAttachmentContentReader(
      @Qualifier("minioStorageClient") MinioClient client, StorageProperties properties) {
    this.client = client;
    this.properties = properties;
  }

  @Override
  public byte[] read(BizAttachment attachment) {
    if (attachment == null || !"MINIO".equals(attachment.storageProvider)) {
      throw new BusinessException("附件存储类型不支持");
    }
    String bucket = attachment.bucketName == null || attachment.bucketName.isBlank()
        ? properties.bucket() : attachment.bucketName;
    String object = attachment.objectKey == null || attachment.objectKey.isBlank()
        ? attachment.storagePath : attachment.objectKey;
    try (InputStream stream = client.getObject(GetObjectArgs.builder()
        .bucket(bucket).object(object).build())) {
      return stream.readAllBytes();
    } catch (IOException | RuntimeException exception) {
      throw new BusinessException("附件读取失败：" + exception.getMessage());
    } catch (Exception exception) {
      throw new BusinessException("附件读取失败：" + exception.getMessage());
    }
  }
}
