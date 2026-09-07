package com.pingan.banzu.service;

import com.pingan.banzu.config.JwtProperties;
import com.pingan.banzu.config.StorageProperties;
import com.pingan.banzu.domain.BizAttachment;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "pingan.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalAttachmentSigner {

  private final byte[] secret;
  private final StorageProperties storage;
  private final Clock clock;

  @Autowired
  public LocalAttachmentSigner(JwtProperties jwt, StorageProperties storage) {
    this(jwt, storage, Clock.systemUTC());
  }

  LocalAttachmentSigner(JwtProperties jwt, StorageProperties storage, Clock clock) {
    this.secret = jwt.secret().getBytes(StandardCharsets.UTF_8);
    this.storage = storage;
    this.clock = clock;
  }

  public String url(BizAttachment attachment) {
    long expires = Instant.now(clock).plusSeconds(storage.presignedUrlMinutes() * 60L).getEpochSecond();
    return "/api/attachments/"
        + attachment.id
        + "/content?expires="
        + expires
        + "&signature="
        + signature(attachment.id, expires);
  }

  public boolean isValid(long attachmentId, long expires, String candidate) {
    if (candidate == null || expires < Instant.now(clock).getEpochSecond()) {
      return false;
    }
    return MessageDigest.isEqual(
        signature(attachmentId, expires).getBytes(StandardCharsets.US_ASCII),
        candidate.getBytes(StandardCharsets.US_ASCII));
  }

  private String signature(long attachmentId, long expires) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      return java.util.HexFormat.of()
          .formatHex(mac.doFinal((attachmentId + ":" + expires).getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("无法生成附件访问签名", exception);
    }
  }
}
