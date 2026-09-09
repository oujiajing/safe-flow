package com.pingan.banzu.system.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pingan.banzu.config.SystemSecurityProperties;
import com.pingan.banzu.security.PasswordService;
import com.pingan.banzu.system.domain.SysVerificationCode;
import com.pingan.banzu.system.mapper.SysVerificationCodeMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class VerificationCodeService {

  private final PasswordService passwordService;
  private final SystemSecurityProperties properties;
  private final SysVerificationCodeMapper verificationCodeMapper;

  public VerificationCodeService(
      PasswordService passwordService,
      SystemSecurityProperties properties,
      SysVerificationCodeMapper verificationCodeMapper) {
    this.passwordService = passwordService;
    this.properties = properties;
    this.verificationCodeMapper = verificationCodeMapper;
  }

  public boolean verify(String username, String purpose, String code) {
    if (code == null || code.isBlank()) {
      return false;
    }
    if (properties.getTestVerificationCode() != null
        && !properties.getTestVerificationCode().isBlank()
        && properties.getTestVerificationCode().equals(code)) {
      return true;
    }
    List<SysVerificationCode> candidates =
        verificationCodeMapper.selectList(
            new QueryWrapper<SysVerificationCode>()
                .eq("username", username)
                .eq("purpose", purpose)
                .eq("status", "PENDING")
                .gt("expires_at", LocalDateTime.now())
                .orderByDesc("created_at")
                .last("limit 1"));
    if (candidates.isEmpty()) {
      return false;
    }
    SysVerificationCode verificationCode = candidates.get(0);
    boolean matches =
        code.equals(verificationCode.codeHash) || passwordService.matches(code, verificationCode.codeHash);
    if (!matches) {
      return false;
    }
    verificationCode.status = "VERIFIED";
    verificationCode.verifiedAt = LocalDateTime.now();
    verificationCodeMapper.updateById(verificationCode);
    return true;
  }
}
