package com.pingan.banzu.service;

import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.config.StorageProperties;
import com.pingan.banzu.domain.BizAttachment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "pingan.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalAttachmentContentReader implements AttachmentContentReader {
  private final Path root;

  public LocalAttachmentContentReader(StorageProperties properties) {
    this.root = Path.of(properties.rootDir()).toAbsolutePath().normalize();
  }

  @Override
  public byte[] read(BizAttachment attachment) {
    if (attachment == null || !"LOCAL".equals(attachment.storageProvider)) {
      throw new BusinessException("附件存储类型不支持");
    }
    Path file = root.resolve(attachment.storagePath == null ? "" : attachment.storagePath).normalize();
    if (!file.startsWith(root) || !Files.isRegularFile(file)) {
      throw new BusinessException("附件不存在");
    }
    try {
      return Files.readAllBytes(file);
    } catch (IOException exception) {
      throw new BusinessException("附件读取失败：" + exception.getMessage());
    }
  }
}
