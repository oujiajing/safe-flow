package com.pingan.banzu.service;

import com.pingan.banzu.domain.BizAttachment;

public interface AttachmentUrlResolver {

  String url(BizAttachment attachment);
}
