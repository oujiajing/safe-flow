package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pingan.banzu.config.StorageProperties;
import com.pingan.banzu.controller.MinioObjectProxyController;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

class MinioObjectProxyControllerTest {

  @Test
  void streamsBundledMiniProgramAssetWhenMinioObjectIsMissing() throws Exception {
    MinioClient minioClient = mock(MinioClient.class);
    when(minioClient.getObject(any(GetObjectArgs.class))).thenThrow(new RuntimeException("missing"));
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
    MinioObjectProxyController controller =
        new MinioObjectProxyController(properties, minioClient, new DefaultResourceLoader());
    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/api/assets/mini-program/icons/fallback.txt");

    ResponseEntity<StreamingResponseBody> response = controller.downloadAsset(request);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    response.getBody().writeTo(outputStream);

    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
    assertThat(outputStream.toString(StandardCharsets.UTF_8))
        .isEqualToNormalizingNewlines("fallback icon asset\n");
  }

  @Test
  void streamsBundledBannerWhenMinioObjectIsMissing() throws Exception {
    MinioClient minioClient = mock(MinioClient.class);
    when(minioClient.getObject(any(GetObjectArgs.class))).thenThrow(new RuntimeException("missing"));
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
    MinioObjectProxyController controller =
        new MinioObjectProxyController(properties, minioClient, new DefaultResourceLoader());
    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/api/assets/mini-program/banners/1.jpg");

    ResponseEntity<StreamingResponseBody> response = controller.downloadAsset(request);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    response.getBody().writeTo(outputStream);

    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
    assertThat(outputStream.size()).isGreaterThan(50_000);
  }
}


