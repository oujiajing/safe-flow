package com.pingan.banzu.config;

import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnProperty(name = "pingan.storage.provider", havingValue = "minio")
public class MinioStorageConfig {

  @Bean(name = "minioStorageClient")
  @Primary
  public MinioClient minioStorageClient(StorageProperties properties) {
    return MinioClient.builder()
        .endpoint(properties.endpoint())
        .credentials(properties.accessKey(), properties.secretKey())
        .build();
  }

  @Bean(name = "minioPresignClient")
  public MinioClient minioPresignClient(StorageProperties properties) {
    return MinioClient.builder()
        .endpoint(properties.effectivePublicEndpoint())
        .credentials(properties.accessKey(), properties.secretKey())
        .build();
  }
}
