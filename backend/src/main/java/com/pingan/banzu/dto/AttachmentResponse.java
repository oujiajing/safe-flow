package com.pingan.banzu.dto;

public record AttachmentResponse(
    String id,
    String fileKind,
    String originalName,
    String storagePath,
    String url,
    String contentType,
    Long fileSize) {}
