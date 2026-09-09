package com.pingan.banzu.service;

import com.pingan.banzu.dto.AttachmentResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

  AttachmentResponse saveThreeCheckRecordAttachment(
      String moduleKey, Long recordId, String fileKind, MultipartFile file);

  void deleteThreeCheckRecordAttachment(String moduleKey, Long recordId, Long attachmentId);

  AttachmentResponse saveMiniThreeCheckRecordAttachment(
      String moduleKey, Long recordId, String fileKind, MultipartFile file);

  AttachmentResponse saveBusinessAttachment(
      String bizType, Long bizId, String moduleFolder, String fileKind, MultipartFile file);
}
