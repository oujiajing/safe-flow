package com.pingan.banzu.service;

import com.pingan.banzu.domain.BizAttachment;

public interface AttachmentContentReader {
  byte[] read(BizAttachment attachment);
}
