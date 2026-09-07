package com.pingan.banzu.controller;

import com.pingan.banzu.config.StorageProperties;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@ConditionalOnProperty(name = "pingan.storage.provider", havingValue = "minio")
public class MinioObjectProxyController {

  private static final String ASSET_PREFIX = "/api/assets/";
  private static final String MINI_PROGRAM_PREFIX = "mini-program/";

  private final StorageProperties storageProperties;
  private final MinioClient minioClient;
  private final ResourceLoader resourceLoader;

  public MinioObjectProxyController(
      StorageProperties storageProperties,
      @Qualifier("minioStorageClient") MinioClient minioClient,
      ResourceLoader resourceLoader) {
    this.storageProperties = storageProperties;
    this.minioClient = minioClient;
    this.resourceLoader = resourceLoader;
  }

  @GetMapping("/api/assets/**")
  public ResponseEntity<StreamingResponseBody> downloadAsset(HttpServletRequest request) {
    String objectKey = request.getRequestURI().substring(ASSET_PREFIX.length());
    String safeObjectKey = requireObjectKey(objectKey);
    return ResponseEntity.ok()
        .contentType(MediaTypeFactory.getMediaType(safeObjectKey).orElse(MediaType.APPLICATION_OCTET_STREAM))
        .body(streamObject(storageProperties.bucket(), safeObjectKey));
  }

  private StreamingResponseBody streamObject(String bucket, String objectKey) {
    return outputStream -> {
      try (GetObjectResponse inputStream =
          minioClient.getObject(
              GetObjectArgs.builder().bucket(bucket).object(objectKey).build())) {
        StreamUtils.copy(inputStream, outputStream);
      } catch (Exception exception) {
        if (streamFallbackAsset(objectKey, outputStream)) {
          return;
        }
        throw new IOException("MinIO 资源读取失败", exception);
      }
    };
  }

  private boolean streamFallbackAsset(String objectKey, java.io.OutputStream outputStream)
      throws IOException {
    for (Resource resource : fallbackAssetResources(objectKey)) {
      if (resource.exists() && resource.isReadable()) {
        try (InputStream inputStream = resource.getInputStream()) {
          StreamUtils.copy(inputStream, outputStream);
          return true;
        }
      }
    }
    return false;
  }

  private List<Resource> fallbackAssetResources(String objectKey) {
    List<Resource> resources = new ArrayList<>();
    resources.add(resourceLoader.getResource("classpath:/assets/" + objectKey));
    if (objectKey.startsWith(MINI_PROGRAM_PREFIX)) {
      String miniProgramAssetPath = objectKey.substring(MINI_PROGRAM_PREFIX.length());
      resources.add(resourceLoader.getResource("classpath:/assets/" + miniProgramAssetPath));
      resources.add(resourceLoader.getResource("file:mini-program/assets/" + miniProgramAssetPath));
      resources.add(resourceLoader.getResource("file:../mini-program/assets/" + miniProgramAssetPath));
    }
    return resources;
  }

  private String requireObjectKey(String objectKey) {
    if (objectKey == null
        || objectKey.isBlank()
        || objectKey.startsWith("/")
        || objectKey.contains("..")) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "资源不存在");
    }
    return objectKey;
  }
}
