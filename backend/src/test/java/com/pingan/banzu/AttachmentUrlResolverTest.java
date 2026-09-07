package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pingan.banzu.config.JwtProperties;
import com.pingan.banzu.config.StorageProperties;
import com.pingan.banzu.domain.BizAttachment;
import com.pingan.banzu.service.LocalAttachmentUrlResolver;
import com.pingan.banzu.service.LocalAttachmentSigner;
import com.pingan.banzu.service.MinioAttachmentUrlResolver;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AttachmentUrlResolverTest {

  @Test
  void localResolverUsesExpiringSignedAttachmentUrl() {
    StorageProperties properties =
        new StorageProperties("local", "./uploads-local", "", "", "", "", "", "", 15);
    LocalAttachmentUrlResolver resolver =
        new LocalAttachmentUrlResolver(
            new LocalAttachmentSigner(
                new JwtProperties("test", "test-secret-at-least-32-bytes-long", 60), properties));
    BizAttachment attachment = new BizAttachment();
    attachment.id = 42L;
    attachment.storagePath = "pre-shift-meeting/42/image/a.jpg";

    assertThat(resolver.url(attachment))
        .startsWith("/api/attachments/42/content?expires=")
        .contains("&signature=");
  }

  @Test
  void minioResolverUsesObjectMetadataForPresignedUrl() throws Exception {
    MinioClient storageClient = org.mockito.Mockito.mock(MinioClient.class);
    MinioClient presignClient = org.mockito.Mockito.mock(MinioClient.class);
    when(presignClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
        .thenReturn("http://localhost:9000/safeteam-portfolio/object?X-Amz-Signature=test");
    StorageProperties properties =
        new StorageProperties(
            "minio",
            "./uploads-local",
            "http://minio:9000",
            "http://127.0.0.1:9000",
            "safeteam-portfolio",
            "YOUR_MINIO_ACCESS_KEY_HERE",
            "YOUR_MINIO_ACCESS_KEY_HERE",
            "us-east-1",
            20);
    MinioAttachmentUrlResolver resolver =
        new MinioAttachmentUrlResolver(storageClient, presignClient, properties);
    BizAttachment attachment = new BizAttachment();
    attachment.id = 9L;
    attachment.bucketName = "safeteam-portfolio";
    attachment.objectKey = "pre-shift-meeting/42/image/a.jpg";

    assertThat(resolver.url(attachment))
        .isEqualTo("http://localhost:9000/safeteam-portfolio/object?X-Amz-Signature=test");
    ArgumentCaptor<GetPresignedObjectUrlArgs> argsCaptor =
        ArgumentCaptor.forClass(GetPresignedObjectUrlArgs.class);
    verify(presignClient).getPresignedObjectUrl(argsCaptor.capture());
    verify(storageClient, never()).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    assertThat(argsCaptor.getValue().bucket()).isEqualTo("safeteam-portfolio");
    assertThat(argsCaptor.getValue().object()).isEqualTo("pre-shift-meeting/42/image/a.jpg");
  }

  @Test
  void minioResolverPresignsOldVerifyBucketAttachmentsWithTheirRecordedBucket() throws Exception {
    MinioClient storageClient = org.mockito.Mockito.mock(MinioClient.class);
    MinioClient presignClient = org.mockito.Mockito.mock(MinioClient.class);
    when(presignClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
        .thenReturn("http://localhost:9000/safeteam-portfolio-verify/object?X-Amz-Signature=test");
    StorageProperties properties =
        new StorageProperties(
            "minio",
            "./uploads-local",
            "http://minio:9000",
            "http://localhost:9000",
            "safeteam-portfolio",
            "YOUR_MINIO_ACCESS_KEY_HERE",
            "YOUR_MINIO_ACCESS_KEY_HERE",
            "us-east-1",
            20);
    MinioAttachmentUrlResolver resolver =
        new MinioAttachmentUrlResolver(storageClient, presignClient, properties);
    BizAttachment attachment = new BizAttachment();
    attachment.id = 10L;
    attachment.bucketName = "safeteam-portfolio-verify";
    attachment.storagePath = "training/old/image.jpg";

    assertThat(resolver.url(attachment)).contains("X-Amz-Signature=test");
    ArgumentCaptor<GetPresignedObjectUrlArgs> argsCaptor =
        ArgumentCaptor.forClass(GetPresignedObjectUrlArgs.class);
    verify(presignClient).getPresignedObjectUrl(argsCaptor.capture());
    assertThat(argsCaptor.getValue().bucket()).isEqualTo("safeteam-portfolio-verify");
    assertThat(argsCaptor.getValue().object()).isEqualTo("training/old/image.jpg");
  }
}


