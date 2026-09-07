package com.pingan.banzu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pingan.jwt")
public record JwtProperties(String issuer, String secret, long ttlMinutes) {}
