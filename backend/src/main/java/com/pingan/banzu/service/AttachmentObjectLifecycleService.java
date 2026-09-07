package com.pingan.banzu.service;

import com.pingan.banzu.config.StorageProperties;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AttachmentObjectLifecycleService {

  private static final Logger log = LoggerFactory.getLogger(AttachmentObjectLifecycleService.class);

  private final JdbcTemplate jdbcTemplate;
  private final StorageProperties storage;
  private final ObjectProvider<MinioClient> minioClient;
  private final Path localRoot;

  public AttachmentObjectLifecycleService(
      JdbcTemplate jdbcTemplate,
      StorageProperties storage,
      @Qualifier("minioStorageClient") ObjectProvider<MinioClient> minioClient) {
    this.jdbcTemplate = jdbcTemplate;
    this.storage = storage;
    this.minioClient = minioClient;
    this.localRoot = Path.of(storage.rootDir()).toAbsolutePath().normalize();
  }

  public void compensateOnRollback(String provider, String bucket, String objectKey) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status == STATUS_ROLLED_BACK) {
              deleteQuietly(provider, bucket, objectKey);
            }
          }
        });
  }

  public void deleteNow(String provider, String bucket, String objectKey) {
    deleteQuietly(provider, bucket, objectKey);
  }

  @Scheduled(initialDelay = 60_000, fixedDelayString = "${pingan.storage.cleanup-interval-ms:3600000}")
  public void cleanupSoftDeletedObjects() {
    List<StoredObject> objects =
        jdbcTemplate.query(
            """
            select storage_provider, bucket_name, coalesce(object_key, storage_path) object_key
            from biz_attachment
            where deleted = 1
              and coalesce(object_key, storage_path) is not null
            """,
            (rs, rowNum) ->
                new StoredObject(
                    rs.getString("storage_provider"),
                    rs.getString("bucket_name"),
                    rs.getString("object_key")));
    objects.forEach(object -> deleteQuietly(object.provider(), object.bucket(), object.objectKey()));
  }

  private void deleteQuietly(String provider, String bucket, String objectKey) {
    if (objectKey == null || objectKey.isBlank()) {
      return;
    }
    try {
      if ("MINIO".equalsIgnoreCase(provider)) {
        MinioClient client = minioClient.getIfAvailable();
        if (client == null) {
          return;
        }
        client.removeObject(
            RemoveObjectArgs.builder()
                .bucket(bucket == null || bucket.isBlank() ? storage.bucket() : bucket)
                .object(objectKey)
                .build());
        return;
      }
      Path target = localRoot.resolve(objectKey).normalize();
      if (target.startsWith(localRoot)) {
        Files.deleteIfExists(target);
      }
    } catch (Exception exception) {
      log.warn("附件对象清理失败，保留待下次重试: provider={}, objectKey={}", provider, objectKey);
    }
  }

  private record StoredObject(String provider, String bucket, String objectKey) {}
}
