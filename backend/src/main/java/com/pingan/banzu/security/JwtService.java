package com.pingan.banzu.security;

import com.pingan.banzu.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final JwtProperties properties;
  private final SecretKey signingKey;

  public JwtService(JwtProperties properties) {
    this.properties = properties;
    this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
  }

  public String issue(CurrentUser user, long authVersion) {
    Instant now = Instant.now();
    return Jwts.builder()
        .issuer(properties.issuer())
        .subject(user.username())
        .claim("userId", user.userId())
        .claim("realName", user.realName())
        .claim("orgId", user.orgId())
        .claim("orgPath", user.orgPath())
        .claim("roles", user.roles())
        .claim("authVersion", authVersion)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(properties.ttlMinutes(), ChronoUnit.MINUTES)))
        .signWith(signingKey)
        .compact();
  }

  @SuppressWarnings("unchecked")
  public ParsedJwt parse(String token) {
    Claims claims =
        Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    CurrentUser user =
        new CurrentUser(
            claims.get("userId", Number.class).longValue(),
            claims.getSubject(),
            claims.get("realName", String.class),
            claims.get("orgId", Number.class).longValue(),
            claims.get("orgPath", String.class),
            (List<String>) claims.get("roles", List.class));
    Number version = claims.get("authVersion", Number.class);
    return new ParsedJwt(user, version == null ? 0 : version.longValue());
  }

  public record ParsedJwt(CurrentUser user, long authVersion) {}
}
