package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.service.FileUploadValidator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FileUploadValidatorTest {

  private final FileUploadValidator validator = new FileUploadValidator();

  @Test
  void acceptsImageOnlyWhenExtensionAndSignatureAgree() {
    MockMultipartFile png =
        file(
            "photo.png",
            "image/png",
            new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});

    assertThat(validator.validateAttachment("IMAGE", png)).isEqualTo("IMAGE");
  }

  @Test
  void rejectsSvgAndFilesDisguisedByMimeOrExtension() {
    assertThatThrownBy(
            () ->
                validator.validateAttachment(
                    "IMAGE", file("active.svg", "image/svg+xml", "<svg><script/>".getBytes())))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(
            () ->
                validator.validateAttachment(
                    "PDF", file("payload.pdf", "application/pdf", "<html>attack</html>".getBytes())))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void rejectsOversizedBusinessFilesBeforeStorage() {
    MockMultipartFile oversizedImage =
        new MockMultipartFile(
            "file", "large.png", "image/png", new byte[10 * 1024 * 1024 + 1]);

    assertThatThrownBy(() -> validator.validateAttachment("IMAGE", oversizedImage))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("10MB");
  }

  private MockMultipartFile file(String name, String contentType, byte[] content) {
    return new MockMultipartFile("file", name, contentType, content);
  }
}
