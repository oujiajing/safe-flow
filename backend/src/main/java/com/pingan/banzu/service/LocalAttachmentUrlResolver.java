package com.pingan.banzu.service;

import com.pingan.banzu.domain.BizAttachment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "pingan.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalAttachmentUrlResolver implements AttachmentUrlResolver {

  private final LocalAttachmentSigner signer;

  public LocalAttachmentUrlResolver(LocalAttachmentSigner signer) {
    this.signer = signer;
  }

  @Override
  public String url(BizAttachment attachment) {
    return signer.url(attachment);
  }
}
