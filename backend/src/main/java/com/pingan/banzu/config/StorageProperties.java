package com.pingan.banzu.config;

import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pingan.storage")
public record StorageProperties(
    String provider,
    String rootDir,
    String endpoint,
    String publicEndpoint,
    String bucket,
    String accessKey,
    String secretKey,
    String region,
    Integer presignedUrlMinutes) {

  public StorageProperties {
    provider = blankToDefault(provider, "local");
    rootDir = blankToDefault(rootDir, "./uploads");
    endpoint = normalizeLocalhostEndpoint(blankToDefault(endpoint, "http://127.0.0.1:9000"));
    publicEndpoint = normalizeLocalhostEndpoint(blankToDefault(publicEndpoint, ""));
    bucket = blankToDefault(bucket, "pingan-banzu");
    accessKey = blankToDefault(accessKey, "minioadmin");
    secretKey = blankToDefault(secretKey, "minioadmin");
    region = blankToDefault(region, "us-east-1");
    presignedUrlMinutes = presignedUrlMinutes == null || presignedUrlMinutes <= 0 ? 15 : presignedUrlMinutes;
  }

  public boolean isLocal() {
    return "local".equals(normalizedProvider());
  }

  public boolean isMinio() {
    return "minio".equals(normalizedProvider());
  }

  public String effectivePublicEndpoint() {
    return publicEndpoint.isBlank() ? endpoint : publicEndpoint;
  }

  public String localUrl(String storagePath) {
    String normalizedPath = storagePath == null ? "" : storagePath.replace('\\', '/');
    return "/uploads/" + normalizedPath.replaceFirst("^/+", "");
  }

  private String normalizedProvider() {
    return provider.trim().toLowerCase(Locale.ROOT);
  }

  private static String blankToDefault(String value, String defaultValue) {
    return value == null || value.isBlank() ? defaultValue : value.trim();
  }

  private static String normalizeLocalhostEndpoint(String value) {
    if (value == null || value.isBlank()) {
      return value;
    }
    return value.replace("://localhost:", "://127.0.0.1:");
  }
}
