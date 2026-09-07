package com.pingan.banzu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pingan.banzu.domain.BizAttachment;
import com.pingan.banzu.domain.ThreeCheckRecord;
import com.pingan.banzu.dto.AttachmentResponse;
import com.pingan.banzu.mapper.BizAttachmentMapper;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
class ThreeCheckRecordAttachmentSupport {

  private final BizAttachmentMapper attachmentMapper;
  private final ThreeCheckRecordConcurrentUpdater concurrentUpdater;
  private final AttachmentUrlResolver attachmentUrlResolver;

  ThreeCheckRecordAttachmentSupport(
      BizAttachmentMapper attachmentMapper,
      ThreeCheckRecordConcurrentUpdater concurrentUpdater,
      AttachmentUrlResolver attachmentUrlResolver) {
    this.attachmentMapper = attachmentMapper;
    this.concurrentUpdater = concurrentUpdater;
    this.attachmentUrlResolver = attachmentUrlResolver;
  }

  void markUploaded(ThreeCheckRecord record, String fileKind) {
    if ("IMAGE".equals(fileKind)) {
      record.imageCheckStatus = "现场照片";
    }
    if ("VIDEO".equals(fileKind)) {
      record.videoCheckStatus = "视频已传";
    }
    touch(record);
  }

  void markDeleted(ThreeCheckRecord record, String bizType, String fileKind) {
    if ("IMAGE".equals(fileKind) && !hasActive(bizType, record.id, "IMAGE")) {
      record.imageCheckStatus = "未上传";
    }
    if ("VIDEO".equals(fileKind) && !hasActive(bizType, record.id, "VIDEO")) {
      record.videoCheckStatus = "未上传";
    }
    touch(record);
  }

  List<AttachmentResponse> list(String bizType, Long recordId) {
    return attachmentMapper
        .selectList(
            new QueryWrapper<BizAttachment>()
                .eq("biz_type", bizType)
                .eq("biz_id", recordId)
                .eq("deleted", 0))
        .stream()
        .sorted(Comparator.comparing(attachment -> attachment.uploadedAt))
        .map(this::toResponse)
        .toList();
  }

  String firstUrl(String bizType, Long recordId, String fileKind) {
    return attachmentMapper
        .selectList(
            new QueryWrapper<BizAttachment>()
                .eq("biz_type", bizType)
                .eq("biz_id", recordId)
                .eq("file_kind", fileKind)
                .eq("deleted", 0)
                .orderByDesc("uploaded_at")
                .last("limit 1"))
        .stream()
        .findFirst()
        .map(attachmentUrlResolver::url)
        .orElse("");
  }

  private boolean hasActive(String bizType, Long recordId, String fileKind) {
    return attachmentMapper.selectCount(
            new QueryWrapper<BizAttachment>()
                .eq("biz_type", bizType)
                .eq("biz_id", recordId)
                .eq("file_kind", fileKind)
                .eq("deleted", 0))
        > 0;
  }

  private AttachmentResponse toResponse(BizAttachment attachment) {
    return new AttachmentResponse(
        String.valueOf(attachment.id),
        attachment.fileKind,
        attachment.originalName,
        attachment.storagePath,
        attachmentUrlResolver.url(attachment),
        attachment.contentType,
        attachment.fileSize);
  }

  private void touch(ThreeCheckRecord record) {
    record.updatedAt = LocalDateTime.now();
    concurrentUpdater.update(record);
  }
}
