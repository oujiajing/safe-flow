package com.pingan.banzu.service;

import com.pingan.banzu.common.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileUploadValidator {

  private static final long IMAGE_MAX = 10L * 1024 * 1024;
  private static final long VIDEO_MAX = 200L * 1024 * 1024;
  private static final long DOCUMENT_MAX = 50L * 1024 * 1024;
  private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
  private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4");

  public String validateAttachment(String fileKind, MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException("上传文件不能为空");
    }
    String kind = normalizeKind(fileKind);
    String extension = extension(file.getOriginalFilename());
    byte[] header = header(file);
    switch (kind) {
      case "IMAGE", "RECTIFICATION_AFTER_PHOTO" -> {
        requireSize(file, IMAGE_MAX, "图片");
        if (!IMAGE_EXTENSIONS.contains(extension) || !matchesImage(extension, header)) {
          throw new BusinessException("图片仅支持真实的 JPG、PNG、GIF 或 WEBP 文件");
        }
      }
      case "VIDEO" -> {
        requireSize(file, VIDEO_MAX, "视频");
        if (!VIDEO_EXTENSIONS.contains(extension) || !isMp4(header)) {
          throw new BusinessException("视频仅支持真实的 MP4 文件");
        }
      }
      case "PDF" -> {
        requireSize(file, DOCUMENT_MAX, "PDF");
        if (!"pdf".equals(extension) || !startsWith(header, "%PDF-".getBytes())) {
          throw new BusinessException("PDF 附件仅支持真实的 PDF 文件");
        }
      }
      case "DOCUMENT" -> {
        requireSize(file, DOCUMENT_MAX, "文档");
        boolean valid =
            ("pdf".equals(extension) && startsWith(header, "%PDF-".getBytes()))
                || ("doc".equals(extension) && isOle(header))
                || ("docx".equals(extension) && isZip(header));
        if (!valid) {
          throw new BusinessException("文档附件仅支持真实的 PDF、DOC 或 DOCX 文件");
        }
      }
      default -> throw new BusinessException("不支持的附件类型");
    }
    return kind;
  }

  public String safeContentType(MultipartFile file) {
    return switch (extension(file.getOriginalFilename())) {
      case "jpg", "jpeg" -> "image/jpeg";
      case "png" -> "image/png";
      case "gif" -> "image/gif";
      case "webp" -> "image/webp";
      case "mp4" -> "video/mp4";
      case "pdf" -> "application/pdf";
      case "doc" -> "application/msword";
      case "docx" ->
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
      default -> "application/octet-stream";
    };
  }

  private String normalizeKind(String fileKind) {
    if (fileKind == null) {
      throw new BusinessException("附件类型不能为空");
    }
    String kind = fileKind.trim().toUpperCase(Locale.ROOT);
    if (!Set.of("IMAGE", "RECTIFICATION_AFTER_PHOTO", "VIDEO", "PDF", "DOCUMENT").contains(kind)) {
      throw new BusinessException("附件类型仅支持 IMAGE、RECTIFICATION_AFTER_PHOTO、VIDEO、PDF 或 DOCUMENT");
    }
    return kind;
  }

  private void requireSize(MultipartFile file, long maximum, String label) {
    if (file.getSize() > maximum) {
      throw new BusinessException(label + "文件大小不能超过 " + (maximum / 1024 / 1024) + "MB");
    }
  }

  private byte[] header(MultipartFile file) {
    try (InputStream input = file.getInputStream()) {
      return input.readNBytes(16);
    } catch (IOException exception) {
      throw new BusinessException("无法读取上传文件");
    }
  }

  private String extension(String filename) {
    if (filename == null) {
      return "";
    }
    int dot = filename.lastIndexOf('.');
    return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
  }

  private boolean matchesImage(String extension, byte[] header) {
    return switch (extension) {
      case "jpg", "jpeg" -> startsWith(header, new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff});
      case "png" -> startsWith(header, new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
      case "gif" -> startsWith(header, "GIF87a".getBytes()) || startsWith(header, "GIF89a".getBytes());
      case "webp" ->
          header.length >= 12
              && startsWith(header, "RIFF".getBytes())
              && new String(header, 8, 4).equals("WEBP");
      default -> false;
    };
  }

  private boolean isMp4(byte[] header) {
    return header.length >= 12
        && header[4] == 'f'
        && header[5] == 't'
        && header[6] == 'y'
        && header[7] == 'p';
  }

  private boolean isOle(byte[] header) {
    return startsWith(
        header,
        new byte[] {
          (byte) 0xd0, (byte) 0xcf, 0x11, (byte) 0xe0, (byte) 0xa1, (byte) 0xb1, 0x1a, (byte) 0xe1
        });
  }

  private boolean isZip(byte[] header) {
    return startsWith(header, new byte[] {'P', 'K', 0x03, 0x04});
  }

  private boolean startsWith(byte[] value, byte[] prefix) {
    if (value.length < prefix.length) {
      return false;
    }
    for (int i = 0; i < prefix.length; i++) {
      if (value[i] != prefix[i]) {
        return false;
      }
    }
    return true;
  }
}
