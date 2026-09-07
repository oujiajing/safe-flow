package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pingan.banzu.config.StorageProperties;
import com.pingan.banzu.domain.BizAttachment;
import com.pingan.banzu.dto.AttachmentResponse;
import com.pingan.banzu.mapper.BizAttachmentMapper;
import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import com.pingan.banzu.service.AttachmentUrlResolver;
import com.pingan.banzu.service.AttachmentObjectLifecycleService;
import com.pingan.banzu.service.FileUploadValidator;
import com.pingan.banzu.service.MinioFileStorageService;
import com.pingan.banzu.service.ThreeCheckRecordService;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class MinioFileStorageServiceTest {

  @AfterEach
  void clearCurrentUser() {
    CurrentUserContext.clear();
  }

  @Test
  void savesUploadedFileToMinioAndPersistsOnlyObjectMetadata() throws Exception {
    CurrentUserContext.set(new CurrentUser(7L, "uploader", "Uploader", 1L, "/1/", List.of("ADMIN")));
    BizAttachmentMapper attachmentMapper = mock(BizAttachmentMapper.class);
    MinioClient minioClient = mock(MinioClient.class);
    ObjectWriteResponse writeResponse = mock(ObjectWriteResponse.class);
    AtomicReference<BizAttachment> inserted = new AtomicReference<>();
    AttachmentUrlResolver urlResolver = attachment -> "http://localhost:9000/presigned";
    StorageProperties properties =
        new StorageProperties(
            "minio",
            "./uploads",
            "http://localhost:9000",
            "http://localhost:9000",
            "pingan-banzu",
            "minioadmin",
            "minioadmin",
            "us-east-1",
            15);

    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
    when(writeResponse.etag()).thenReturn("etag-123");
    when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(writeResponse);
    when(attachmentMapper.insert(any(BizAttachment.class)))
        .thenAnswer(
            invocation -> {
              BizAttachment attachment = invocation.getArgument(0);
              attachment.id = 99L;
              inserted.set(attachment);
              return 1;
            });

    MinioFileStorageService service =
        new MinioFileStorageService(
            attachmentMapper,
            mock(ThreeCheckRecordService.class),
            properties,
            minioClient,
            urlResolver,
            new FileUploadValidator(),
            mock(AttachmentObjectLifecycleService.class));
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "现场照片.png",
            "image/png",
            new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});

    AttachmentResponse response =
        service.saveBusinessAttachment("PRE_SHIFT_MEETING", 12L, "pre-shift-meeting", "IMAGE", file);

    BizAttachment attachment = inserted.get();
    assertThat(attachment.storageProvider).isEqualTo("MINIO");
    assertThat(attachment.bucketName).isEqualTo("pingan-banzu");
    assertThat(attachment.objectKey)
        .startsWith("pre-shift-meeting/12/image/")
        .endsWith("-现场照片.png");
    assertThat(attachment.storagePath).isEqualTo(attachment.objectKey);
    assertThat(attachment.etag).isEqualTo("etag-123");
    assertThat(response.url()).isEqualTo("http://localhost:9000/presigned");
  }

  @Test
  void allowsPdfBusinessAttachments() throws Exception {
    CurrentUserContext.set(new CurrentUser(7L, "uploader", "Uploader", 1L, "/1/", List.of("ADMIN")));
    BizAttachmentMapper attachmentMapper = mock(BizAttachmentMapper.class);
    MinioClient minioClient = mock(MinioClient.class);
    ObjectWriteResponse writeResponse = mock(ObjectWriteResponse.class);
    AtomicReference<BizAttachment> inserted = new AtomicReference<>();
    AttachmentUrlResolver urlResolver = attachment -> "http://localhost:9000/pdf";
    StorageProperties properties =
        new StorageProperties(
            "minio",
            "./uploads",
            "http://localhost:9000",
            "http://localhost:9000",
            "pingan-banzu",
            "minioadmin",
            "minioadmin",
            "us-east-1",
            15);

    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
    when(writeResponse.etag()).thenReturn("etag-pdf");
    when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(writeResponse);
    when(attachmentMapper.insert(any(BizAttachment.class)))
        .thenAnswer(
            invocation -> {
              BizAttachment attachment = invocation.getArgument(0);
              attachment.id = 100L;
              inserted.set(attachment);
              return 1;
            });

    MinioFileStorageService service =
        new MinioFileStorageService(
            attachmentMapper,
            mock(ThreeCheckRecordService.class),
            properties,
            minioClient,
            urlResolver,
            new FileUploadValidator(),
            mock(AttachmentObjectLifecycleService.class));
    MockMultipartFile file =
        new MockMultipartFile("file", "安全学习.pdf", "application/pdf", "%PDF-1.4".getBytes());

    AttachmentResponse response =
        service.saveBusinessAttachment("TRAINING_SAFETY_LEARNING", 22L, "training/safety-learning", "PDF", file);

    BizAttachment attachment = inserted.get();
    assertThat(attachment.fileKind).isEqualTo("PDF");
    assertThat(attachment.objectKey)
        .startsWith("training/safety-learning/22/pdf/")
        .endsWith("-安全学习.pdf");
    assertThat(response.url()).isEqualTo("http://localhost:9000/pdf");
  }

  @Test
  void removesWrittenObjectWhenMetadataInsertFails() throws Exception {
    CurrentUserContext.set(new CurrentUser(7L, "uploader", "Uploader", 1L, "/1/", List.of("ADMIN")));
    BizAttachmentMapper attachmentMapper = mock(BizAttachmentMapper.class);
    MinioClient minioClient = mock(MinioClient.class);
    ObjectWriteResponse writeResponse = mock(ObjectWriteResponse.class);
    AttachmentObjectLifecycleService lifecycle = mock(AttachmentObjectLifecycleService.class);
    StorageProperties properties =
        new StorageProperties(
            "minio", "./uploads", "http://localhost:9000", "", "bucket", "key", "secret", "", 15);
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
    when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(writeResponse);
    doThrow(new IllegalStateException("database unavailable"))
        .when(attachmentMapper)
        .insert(any(BizAttachment.class));
    MinioFileStorageService service =
        new MinioFileStorageService(
            attachmentMapper,
            mock(ThreeCheckRecordService.class),
            properties,
            minioClient,
            attachment -> "url",
            new FileUploadValidator(),
            lifecycle);
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "photo.png",
            "image/png",
            new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.saveBusinessAttachment("TEST", 1L, "test", "IMAGE", file))
        .isInstanceOf(IllegalStateException.class);
    verify(lifecycle).deleteNow(org.mockito.ArgumentMatchers.eq("MINIO"), org.mockito.ArgumentMatchers.eq("bucket"), any());
  }
}
