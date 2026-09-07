package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;

import com.pingan.banzu.config.StorageProperties;
import org.junit.jupiter.api.Test;

class StorageConfigurationTest {

  @Test
  void localProviderKeepsUploadUrlCompatibility() {
    StorageProperties properties =
        new StorageProperties("local", "./uploads-local", "", "", "", "", "", "", 15);

    assertThat(properties.isLocal()).isTrue();
    assertThat(properties.isMinio()).isFalse();
    assertThat(properties.localUrl("pre-shift-meeting/1/image/example.jpg"))
        .isEqualTo("/uploads/pre-shift-meeting/1/image/example.jpg");
  }

  @Test
  void minioProviderExposesBucketAndPresignedUrlSettings() {
    StorageProperties properties =
        new StorageProperties(
            "minio",
            "./uploads-local",
            "http://localhost:9000",
            "http://127.0.0.1:9000",
            "safeteam-portfolio",
            "YOUR_MINIO_ACCESS_KEY_HERE",
            "YOUR_MINIO_ACCESS_KEY_HERE",
            "us-east-1",
            20);

    assertThat(properties.isLocal()).isFalse();
    assertThat(properties.isMinio()).isTrue();
    assertThat(properties.effectivePublicEndpoint()).isEqualTo("http://127.0.0.1:9000");
    assertThat(properties.presignedUrlMinutes()).isEqualTo(20);
  }

  @Test
  void minioEndpointNormalizesLocalhostToIpv4LoopbackForDockerOnWindows() {
    StorageProperties properties =
        new StorageProperties(
            "minio",
            "./uploads-local",
            "http://localhost:9000",
            "",
            "safeteam-portfolio",
            "YOUR_MINIO_ACCESS_KEY_HERE",
            "YOUR_MINIO_ACCESS_KEY_HERE",
            "us-east-1",
            15);

    assertThat(properties.endpoint()).isEqualTo("http://127.0.0.1:9000");
    assertThat(properties.effectivePublicEndpoint()).isEqualTo("http://127.0.0.1:9000");
  }
}


