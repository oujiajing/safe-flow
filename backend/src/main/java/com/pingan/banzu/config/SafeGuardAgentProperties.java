package com.pingan.banzu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pingan.safeguard-agent")
public record SafeGuardAgentProperties(
    boolean enabled,
    String baseUrl,
    String serviceToken,
    long requestTimeoutMs,
    long maxImageBytes,
    boolean assessmentEnabled,
    boolean reviewDraftEnabled,
    String companyAllowlist,
    boolean writebackEnabled) {

  public SafeGuardAgentProperties {
    baseUrl = baseUrl == null || baseUrl.isBlank()
        ? "http://127.0.0.1:9090/api/safeguard-agent"
        : baseUrl.replaceAll("/$", "");
    requestTimeoutMs = requestTimeoutMs <= 0 ? 120_000 : requestTimeoutMs;
    maxImageBytes = maxImageBytes <= 0 ? 8 * 1024 * 1024 : maxImageBytes;
  }

  public boolean isCompanyAllowed(Long companyId) {
    if (companyAllowlist == null || companyAllowlist.isBlank()) return true;
    if (companyId == null) return false;
    return java.util.Arrays.stream(companyAllowlist.split(","))
        .map(String::trim)
        .anyMatch(value -> value.equals(String.valueOf(companyId)));
  }
}
