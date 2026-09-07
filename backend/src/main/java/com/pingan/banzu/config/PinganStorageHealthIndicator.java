package com.pingan.banzu.config;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("pinganStorage")
public class PinganStorageHealthIndicator implements HealthIndicator {

  private final StorageProperties storage;
  private final ObjectProvider<MinioClient> minioClient;

  public PinganStorageHealthIndicator(
      StorageProperties storage,
      @Qualifier("minioStorageClient") ObjectProvider<MinioClient> minioClient) {
    this.storage = storage;
    this.minioClient = minioClient;
  }

  @Override
  public Health health() {
    try {
      if (storage.isMinio()) {
        MinioClient client = minioClient.getIfAvailable();
        boolean bucketExists =
            client != null
                && client.bucketExists(
                    BucketExistsArgs.builder().bucket(storage.bucket()).build());
        return bucketExists
            ? Health.up().withDetail("provider", "minio").build()
            : Health.down().withDetail("provider", "minio").build();
      }

      Path root = Path.of(storage.rootDir()).toAbsolutePath().normalize();
      Path probe = Files.exists(root) ? root : root.getParent();
      boolean writable = probe != null && Files.exists(probe) && Files.isWritable(probe);
      return writable
          ? Health.up().withDetail("provider", "local").build()
          : Health.down().withDetail("provider", "local").build();
    } catch (Exception exception) {
      return Health.down().withDetail("provider", storage.provider()).build();
    }
  }
}
