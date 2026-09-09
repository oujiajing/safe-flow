package com.pingan.banzu.controller;

import com.pingan.banzu.config.StorageProperties;
import com.pingan.banzu.domain.BizAttachment;
import com.pingan.banzu.mapper.BizAttachmentMapper;
import com.pingan.banzu.service.LocalAttachmentSigner;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@ConditionalOnProperty(name = "pingan.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalAttachmentController {

  private final BizAttachmentMapper attachmentMapper;
  private final LocalAttachmentSigner signer;
  private final Path root;

  public LocalAttachmentController(
      BizAttachmentMapper attachmentMapper,
      LocalAttachmentSigner signer,
      StorageProperties storage) {
    this.attachmentMapper = attachmentMapper;
    this.signer = signer;
    this.root = Path.of(storage.rootDir()).toAbsolutePath().normalize();
  }

  @GetMapping("/api/attachments/{id}/content")
  public ResponseEntity<FileSystemResource> content(
      @PathVariable Long id,
      @RequestParam long expires,
      @RequestParam String signature) {
    if (!signer.isValid(id, expires, signature)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "附件访问地址无效或已过期");
    }
    BizAttachment attachment = attachmentMapper.selectById(id);
    if (attachment == null
        || Integer.valueOf(1).equals(attachment.deleted)
        || !"LOCAL".equals(attachment.storageProvider)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "附件不存在");
    }
    Path file = root.resolve(attachment.storagePath).normalize();
    if (!file.startsWith(root) || !Files.isRegularFile(file)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "附件不存在");
    }
    MediaType mediaType;
    try {
      mediaType = MediaType.parseMediaType(attachment.contentType);
    } catch (Exception exception) {
      mediaType = MediaType.APPLICATION_OCTET_STREAM;
    }
    return ResponseEntity.ok()
        .header("X-Content-Type-Options", "nosniff")
        .header("Content-Security-Policy", "sandbox; default-src 'none'")
        .contentType(mediaType)
        .body(new FileSystemResource(file));
  }
}
