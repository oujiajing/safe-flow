package com.pingan.banzu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pingan.security")
public class SystemSecurityProperties {
  private int maxFailedLoginAttempts = 5;
  private long lockMinutes = 30;
  private boolean twoFactorEnabled = false;
  private String testVerificationCode = "";

  public int getMaxFailedLoginAttempts() {
    return maxFailedLoginAttempts;
  }

  public void setMaxFailedLoginAttempts(int maxFailedLoginAttempts) {
    this.maxFailedLoginAttempts = maxFailedLoginAttempts;
  }

  public long getLockMinutes() {
    return lockMinutes;
  }

  public void setLockMinutes(long lockMinutes) {
    this.lockMinutes = lockMinutes;
  }

  public boolean isTwoFactorEnabled() {
    return twoFactorEnabled;
  }

  public void setTwoFactorEnabled(boolean twoFactorEnabled) {
    this.twoFactorEnabled = twoFactorEnabled;
  }

  public String getTestVerificationCode() {
    return testVerificationCode;
  }

  public void setTestVerificationCode(String testVerificationCode) {
    this.testVerificationCode = testVerificationCode;
  }
}
